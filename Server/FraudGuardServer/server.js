require('dotenv').config();
const express = require('express');
const http = require('http');
const WebSocket = require('ws');
const SpeechSDK = require('microsoft-cognitiveservices-speech-sdk');
const twilio = require('twilio');
const axios = require('axios');
const mongoose = require('mongoose');
const cors = require('cors');

// --- CONFIGURATION ---
const TWILIO_ACCOUNT_SID = process.env.TWILIO_ACCOUNT_SID || "YOUR_TWILIO_ACCOUNT_SID_HERE";
const TWILIO_AUTH_TOKEN = process.env.TWILIO_AUTH_TOKEN || "YOUR_TWILIO_AUTH_TOKEN_HERE";
// ⚠️ CRITICAL: REPLACE WITH YOUR TWILIO VERIFY SERVICE SID
const TWILIO_VERIFY_SERVICE_SID = process.env.TWILIO_VERIFY_SERVICE_SID || "YOUR_TWILIO_VERIFY_SERVICE_SID_HERE"; 
const AZURE_KEY = process.env.AZURE_KEY || "YOUR_AZURE_KEY_HERE";
const AZURE_REGION = "centralindia";

// Initialize Twilio Client
const twilioClient = twilio(TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN); 

// --- NEW/UPDATED CONFIG ---
const MONGO_URI = 'mongodb://localhost:27017/fraudguard';
const PYTHON_API_URL = 'http://127.0.0.1:5000/predict'; // Correct IP for your Python API
const FRAUD_THRESHOLD = 0.7; // You can adjust this threshold

const speechConfig = SpeechSDK.SpeechConfig.fromSubscription(AZURE_KEY, AZURE_REGION);
speechConfig.speechRecognitionLanguage = "en-US";

// --- SERVER SETUP ---
const app = express();
const port = 3001;
const server = http.createServer(app);
// Use the same server for both Twilio and App WebSockets, but on different paths
const wssTwilio = new WebSocket.Server({ noServer: true });
const wssApp = new WebSocket.Server({ noServer: true });

app.use(cors());
app.use(express.json());

// --- DATABASE CONNECTION & SCHEMA ---
mongoose.connect(MONGO_URI)
    .then(() => console.log('✅ MongoDB connected successfully!'))
    .catch(err => console.error('❌ MongoDB connection error:', err));

const reportSchema = new mongoose.Schema({
    scammerNumber: { type: String, required: true },
    userNumber: { type: String, required: true },
    scamSummary: { type: String, required: true },
    scamTypeName: { type: String, required: true },
    timestamp: { type: Date, default: Date.now }
});

const Report = mongoose.model('Report', reportSchema);

// --- NEW SCHEMA: User (to save authenticated users) ---
const userSchema = new mongoose.Schema({
    phoneNumber: { type: String, required: true, unique: true },
    name: { type: String, required: true },
    isVerified: { type: Boolean, default: false },
    lastLogin: { type: Date, default: Date.now }
});

const User = mongoose.model('User', userSchema);
// --- END OF DATABASE SECTION ---

let androidAppWs = null;

const ulaw_decode = (ulaw_byte) => {
    ulaw_byte = ~ulaw_byte;
    let exponent = (ulaw_byte & 0x70) >> 4;
    let mantissa = ulaw_byte & 0x0f;
    let pcm_val = (mantissa << 3) + 0x84;
    pcm_val <<= exponent - 1;
    return (ulaw_byte & 0x80) ? -pcm_val : pcm_val;
};

// --- HTTP Server Upgrade Handling for Multiple WS Paths ---
server.on('upgrade', (request, socket, head) => {
    const pathname = request.url;
    // Twilio connects to the root path /
    if (pathname === '/') {
        wssTwilio.handleUpgrade(request, socket, head, (ws) => {
            wssTwilio.emit('connection', ws, request);
        });
    // Android app connects to /app
    } else if (pathname === '/app') {
        wssApp.handleUpgrade(request, socket, head, (ws) => {
            wssApp.emit('connection', ws, request);
        });
    } else {
        socket.destroy();
    }
});

// --- Android App WebSocket Logic ---
wssApp.on('connection', (ws) => {
    console.log('Node.js: Android App WebSocket connected.');

    ws.on('message', (message) => {
        const msg = JSON.parse(message);
        if (msg.type === 'app_connect') {
            androidAppWs = ws; // Store the app's WebSocket reference
            console.log('Node.js: Android App connection reference saved.');
        }
    });

    ws.on('close', () => {
        if (androidAppWs === ws) {
            androidAppWs = null;
            console.log('Node.js: Android App WebSocket disconnected.');
        }
    });
});

// --- Twilio Call WebSocket Logic ---
wssTwilio.on('connection', (ws) => {
    console.log('Node.js: Twilio Media Stream WebSocket established.');

    let recognizer = null;
    let pushStream = null;

    ws.on('message', async (message) => {
        try {
            const msg = JSON.parse(message);

            if (msg.event === 'start') {
                console.log('Node.js: Twilio media stream started.');
                const audioFormat = SpeechSDK.AudioStreamFormat.getWaveFormatPCM(8000, 16, 1);
                pushStream = SpeechSDK.AudioInputStream.createPushStream(audioFormat);
                const audioConfig = SpeechSDK.AudioConfig.fromStreamInput(pushStream);
                recognizer = new SpeechSDK.SpeechRecognizer(speechConfig, audioConfig);

                recognizer.recognizing = async (s, e) => {
                    const transcript = e.result.text;
                    // FIX: Ensure the /predict endpoint is called from here for live analysis
                    if (transcript && transcript.length > 10) {
                        console.log(`Node.js (Call): RECOGNIZING: ${transcript}`);
                        try {
                            const response = await axios.post(PYTHON_API_URL, { text: transcript });
                            const analysis = response.data; // Full object from your Python API
                            
                            console.log(`Node.js (Call): Got analysis from Python: Score=${analysis.fraud_score.toFixed(4)}, Type=${analysis.scam_type_name}`);

                            if (analysis.fraud_score > FRAUD_THRESHOLD) {
                                console.log(`Node.js (Call): !!! FRAUD DETECTED !!!`);
                                
                                // Send the entire analysis object to the app
                                const alertPayload = { 
                                    type: "fraud_alert", 
                                    // Use 'report_summary' as the immediate reason text for the overlay
                                    reason: analysis.report_summary, 
                                    ...analysis 
                                };
                                
                                if (androidAppWs && androidAppWs.readyState === WebSocket.OPEN) {
                                    androidAppWs.send(JSON.stringify(alertPayload));
                                    console.log('Node.js (Call): Full analysis alert sent to Android app.');
                                } else {
                                    console.log('Node.js (Call): Warning: Android App WS not available or closed.');
                                }
                            }
                        } catch (error) {
                            console.error("Node.js (Call): An error occurred during the prediction process:", error.message);
                        }
                    }
                };
                recognizer.startContinuousRecognitionAsync();
            }

            if (msg.event === 'media' && pushStream) {
                const mulaw_buffer = Buffer.from(msg.media.payload, 'base64');
                const pcm_buffer = Buffer.alloc(mulaw_buffer.length * 2);
                for (let i = 0; i < mulaw_buffer.length; i++) {
                    pcm_buffer.writeInt16LE(ulaw_decode(mulaw_buffer[i]), i * 2);
                }
                pushStream.write(pcm_buffer);
            }
        } catch (err) {
            // Ignoring non-JSON messages from Twilio audio stream
        }
    });

    ws.on('close', () => {
        console.log('Node.js: Twilio Media Stream WebSocket closed.');
        if (recognizer) {
            recognizer.stopContinuousRecognitionAsync();
        }
    });
});

// --- Existing Twilio Webhook ---
app.post('/handle-call', (req, res) => {
    console.log('Node.js: Webhook received for a call.');
    const twiml = new twilio.twiml.VoiceResponse();
    const host = req.headers.host;
    twiml.start().stream({ url: `wss://${host}/` });
    twiml.say("Your call is now being protected.");
    twiml.pause({ length: 600 });
    res.type('text/xml');
    res.send(twiml.toString());
});

// --- OTP AUTHENTICATION ENDPOINTS (NEW) ---

// 1. Endpoint to send the OTP
app.post('/send-otp', async (req, res) => {
    const { phoneNumber, name } = req.body;

    if (!phoneNumber || !name) {
        return res.status(400).json({ error: 'Phone number and Name are required.' });
    }

    try {
        // Create or Update User in MongoDB to save the name before verification
        await User.findOneAndUpdate(
            { phoneNumber: phoneNumber },
            { name: name, isVerified: false, lastLogin: new Date() },
            { upsert: true, new: true, setDefaultsOnInsert: true }
        );
        
        // Initiate the verification process using Twilio Verify
        const verification = await twilioClient.verify.v2.services(TWILIO_VERIFY_SERVICE_SID)
            .verifications
            .create({ to: phoneNumber, channel: 'sms' });

        console.log(`✅ Sent verification code to ${phoneNumber}. Status: ${verification.status}`);
        res.status(200).json({ message: 'OTP sent successfully.' });

    } catch (error) {
        console.error('❌ Error sending OTP:', error.message);
        res.status(500).json({ error: 'Failed to send OTP. Check phone number format.' });
    }
});

// 2. Endpoint to verify the OTP
app.post('/verify-otp', async (req, res) => {
    const { phoneNumber, otp } = req.body;

    if (!phoneNumber || !otp) {
        return res.status(400).json({ error: 'Phone number and OTP are required.' });
    }

    try {
        // Check the OTP using Twilio Verify
        const verificationCheck = await twilioClient.verify.v2.services(TWILIO_VERIFY_SERVICE_SID)
            .verificationChecks
            .create({ to: phoneNumber, code: otp });

        if (verificationCheck.status === 'approved') {
            console.log(`✅ Verification successful for ${phoneNumber}.`);
            
            // Mark the user as verified in MongoDB
            const user = await User.findOneAndUpdate(
                { phoneNumber: phoneNumber },
                { isVerified: true, lastLogin: new Date() },
                { new: true }
            );

            if (user) {
                res.status(200).json({ 
                    message: 'Verification successful.', 
                    name: user.name, 
                    phoneNumber: user.phoneNumber 
                });
            } else {
                res.status(404).json({ error: 'User not found after verification.' });
            }

        } else {
            console.log(`❌ Verification failed for ${phoneNumber}. Status: ${verificationCheck.status}`);
            res.status(401).json({ error: 'Invalid OTP or session expired.' });
        }

    } catch (error) {
        console.error('❌ Error during OTP verification:', error.message);
        res.status(500).json({ error: 'An unexpected error occurred during verification.' });
    }
});

// --- Messaging API Endpoint (Existing) ---
app.post('/predict', async (req, res) => {
    const { text } = req.body;

    if (!text) {
        return res.status(400).json({ error: 'Text is required' });
    }

    console.log(`Node.js (Message): Received text for analysis: "${text}"`);

    try {
        const response = await axios.post(PYTHON_API_URL, { text });
        const analysis = response.data; 

        console.log(`Node.js (Message): Got analysis from Python: Score=${analysis.fraud_score.toFixed(4)}, Type=${analysis.scam_type_name}`);
        res.status(200).json(analysis);

    } catch (error) {
        console.error("Node.js (Message): Error communicating with Python API:", error.message);
        res.status(500).json({ error: 'Failed to get prediction from AI service' });
    }
});

// --- API ENDPOINT FOR REPORTING A SCAM (Existing) ---
app.post('/report-scam', async (req, res) => {
    try {
        const { scammerNumber, victimNumber, reportSummary, scamTypeName } = req.body;

        if (!scammerNumber || !victimNumber || !reportSummary || !scamTypeName) {
            console.error('Missing fields in report:', req.body); 
            return res.status(400).json({ error: 'Missing required fields for reporting. Expected: scammerNumber, victimNumber, reportSummary, scamTypeName' });
        }

        const newReport = new Report({
            scammerNumber: scammerNumber,
            userNumber: victimNumber, 
            scamSummary: reportSummary, 
            scamTypeName: scamTypeName
        });

        await newReport.save();
        console.log('✅ New scam report saved to the database:', newReport);
        res.status(201).json({ message: 'Report submitted successfully!', data: newReport });

    } catch (error) {
        console.error('❌ Error saving scam report:', error);
        res.status(500).json({ error: 'Failed to save the report.' });
    }
});

// --- GET API TO FETCH ALL REPORTS (Existing) ---
app.get('/get-reports', async (req, res) => {
    try {
        const reports = await Report.find({});
        
        console.log(`✅ Fetched ${reports.length} reports from the database.`);
        res.status(200).json(reports);

    } catch (error) {
        console.error('❌ Error fetching reports:', error);
        res.status(500).json({ error: 'Failed to fetch reports from the database.' });
    }
});

// --- PYTHON API PROXY ROUTE (NEW) ---
app.post('/check-url', (req, res) => {
    const options = {
        hostname: '127.0.0.1',
        port: 8000,
        path: '/check-url',
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    };
    
    const proxyReq = http.request(options, (proxyRes) => {
        res.writeHead(proxyRes.statusCode, proxyRes.headers);
        proxyRes.pipe(res, { end: true });
    });
    
    proxyReq.on('error', (e) => {
        console.error(`Python backend proxy error: ${e.message}`);
        res.status(500).json({ error: "Failed to connect to Python backend." });
    });
    
    proxyReq.write(JSON.stringify(req.body));
    proxyReq.end();
});

// --- SERVER START ---
server.listen(port, () => {
    console.log(`✅ FraudGuard Server is running on http://localhost:${port}`);
});