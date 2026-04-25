const express = require('express');
const http = require('http');
const WebSocket = require('ws');
const twilio = require('twilio');
const axios = require('axios');
const mongoose = require('mongoose');
const cors = require('cors');
const speech = require('@google-cloud/speech');
require('dotenv').config();

// --- CONFIGURATION ---
const TWILIO_ACCOUNT_SID = process.env.TWILIO_ACCOUNT_SID;
const TWILIO_AUTH_TOKEN = process.env.TWILIO_AUTH_TOKEN;
const TWILIO_VERIFY_SERVICE_SID = process.env.TWILIO_VERIFY_SERVICE_SID;

// GOOGLE CLOUD SETUP (Reverted to your working style)
const GOOGLE_API_KEY = process.env.GOOGLE_API_KEY;
const speechClient = new speech.SpeechClient({
    apiKey: GOOGLE_API_KEY
});

// Initialize Twilio Client
const twilioClient = twilio(TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN);

// --- DATABASE & API CONFIG ---
const MONGO_URI = 'mongodb://localhost:27017/fraudguard';
const PYTHON_API_URL = 'http://127.0.0.1:5000/predict';
const FRAUD_THRESHOLD = 0.7;

// --- SERVER SETUP ---
const app = express();
const port = 3001;
const server = http.createServer(app);
const wssTwilio = new WebSocket.Server({ noServer: true });
const wssApp = new WebSocket.Server({ noServer: true });

app.use(cors());
app.use(express.json());

// --- DATABASE CONNECTION ---
mongoose.connect(MONGO_URI)
    .then(() => console.log('✅ MongoDB connected successfully!'))
    .catch(err => console.error('❌ MongoDB connection error:', err));

const reportSchema = new mongoose.Schema({
    scammerNumber: String,
    userNumber: String,
    scamSummary: String,
    scamTypeName: String,
    timestamp: { type: Date, default: Date.now }
});
const Report = mongoose.model('Report', reportSchema);

const userSchema = new mongoose.Schema({
    phoneNumber: { type: String, unique: true },
    name: String,
    isVerified: { type: Boolean, default: false },
    lastLogin: { type: Date, default: Date.now }
});
const User = mongoose.model('User', userSchema);

let androidAppWs = null;

// --- HTTP Server Upgrade Handling ---
server.on('upgrade', (request, socket, head) => {
    const pathname = request.url;
    if (pathname === '/') {
        wssTwilio.handleUpgrade(request, socket, head, (ws) => wssTwilio.emit('connection', ws, request));
    } else if (pathname === '/app') {
        wssApp.handleUpgrade(request, socket, head, (ws) => wssApp.emit('connection', ws, request));
    } else {
        socket.destroy();
    }
});

// --- Android App WebSocket Logic ---
wssApp.on('connection', (ws) => {
    console.log('Node.js: Android App WebSocket connected.');
    ws.on('message', (message) => {
        try {
            const msg = JSON.parse(message);
            if (msg.type === 'app_connect') {
                androidAppWs = ws;
                console.log('Node.js: Android App connection reference saved.');
            }
        } catch (e) { console.error("Error parsing App message"); }
    });
    ws.on('close', () => {
        androidAppWs = null;
        console.log('Node.js: Android App disconnected.');
    });
});

// --- Twilio Call WebSocket Logic (GOOGLE AI) ---
// --- Twilio Call WebSocket Logic (GOOGLE AI) ---
wssTwilio.on('connection', (ws) => {
    console.log('Node.js: Twilio Media Stream WebSocket established.');
    let recognizeStream = null;

    ws.on('message', async (message) => {
        try {
            const msg = JSON.parse(message);

            if (msg.event === 'start') {
                console.log('Node.js: Twilio media stream started (Google AI).');
                const request = {
                    config: {
                        encoding: 'MULAW',
                        sampleRateHertz: 8000,
                        languageCode: 'en-IN',
                    },
                    interimResults: true,
                };

                recognizeStream = speechClient
                    .streamingRecognize(request)
                    .on('error', (err) => console.error("❌ GOOGLE API ERROR:", err.message))
                    .on('data', async (data) => {
                        if (data.results[0] && data.results[0].alternatives[0]) {
                            const transcript = data.results[0].alternatives[0].transcript;
                            console.log(`Listening: ${transcript}`);

                            if (data.results[0].isFinal && transcript.length > 5) {
                                console.log(`Google Heard: ${transcript}`);
                                try {
                                    const response = await axios.post(PYTHON_API_URL, { text: transcript });
                                    const analysis = response.data;

                                    if (analysis.fraud_score > FRAUD_THRESHOLD) {
                                        console.log(`!!! FRAUD DETECTED: ${analysis.scam_type_name} !!!`);

                                        if (androidAppWs && androidAppWs.readyState === WebSocket.OPEN) {
                                            // 🚨 THE FIX IS HERE 🚨
                                            // We map 'report_summary' from Python to 'reason' for the Android Overlay
                                            const alertPayload = {
                                                type: "fraud_alert",
                                                scam_type: analysis.scam_type_name,
                                                reason: analysis.report_summary, // This fills the red box
                                                score: analysis.fraud_score,
                                                educational_info: analysis.educational_summary
                                            };

                                            androidAppWs.send(JSON.stringify(alertPayload));
                                            console.log("✅ Summary sent to Overlay:", analysis.report_summary);
                                        }
                                    }
                                } catch (e) { console.error("Python AI Predictor Offline"); }
                            }
                        }
                    });
            }

            if (msg.event === 'media' && recognizeStream) {
                recognizeStream.write(msg.media.payload);
            }
        } catch (err) { }
    });

    ws.on('close', () => {
        console.log('Node.js: Twilio Media Stream WebSocket closed.');
        if (recognizeStream) recognizeStream.end();
    });
});

// --- Twilio Webhook ---
app.post('/handle-call', (req, res) => {
    console.log('Node.js: Webhook received for a call.');
    const twiml = new twilio.twiml.VoiceResponse();
    const host = req.headers.host;
    twiml.start().stream({ url: `wss://${host}/` });
    twiml.say("Your call is now being protected by Fraud Guard.");
    twiml.pause({ length: 600 });
    res.type('text/xml').send(twiml.toString());
});

// --- OTP AUTHENTICATION ---
app.post('/send-otp', async (req, res) => {
    const { phoneNumber, name } = req.body;
    try {
        await User.findOneAndUpdate({ phoneNumber }, { name, isVerified: false }, { upsert: true });
        await twilioClient.verify.v2.services(TWILIO_VERIFY_SERVICE_SID).verifications.create({ to: phoneNumber, channel: 'sms' });
        res.status(200).json({ message: 'OTP sent.' });
    } catch (error) { res.status(500).json({ error: error.message }); }
});

app.post('/verify-otp', async (req, res) => {
    const { phoneNumber, otp } = req.body;
    try {
        const check = await twilioClient.verify.v2.services(TWILIO_VERIFY_SERVICE_SID).verificationChecks.create({ to: phoneNumber, code: otp });
        if (check.status === 'approved') {
            await User.findOneAndUpdate({ phoneNumber }, { isVerified: true });
            res.status(200).json({ message: 'Verified' });
        } else { res.status(401).json({ error: 'Invalid OTP' }); }
    } catch (error) { res.status(500).json({ error: error.message }); }
});

// --- API PROXY ROUTE (LINK INSPECTOR) ---
app.post('/check-url', (req, res) => {
    const options = {
        hostname: '127.0.0.1',
        port: 8000,
        path: '/check-url',
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
    };
    const proxyReq = http.request(options, (pRes) => {
        res.writeHead(pRes.statusCode, pRes.headers);
        pRes.pipe(res, { end: true });
    });
    proxyReq.on('error', () => res.status(500).json({ error: "Link Inspector Offline." }));
    proxyReq.write(JSON.stringify(req.body));
    proxyReq.end();
});

// --- STANDARD ENDPOINTS ---
app.post('/report-scam', async (req, res) => {
    try {
        const newReport = new Report(req.body);
        await newReport.save();
        res.status(201).send("Reported");
    } catch (e) { res.status(500).send("DB Error"); }
});

app.get('/get-reports', async (req, res) => {
    try {
        const reports = await Report.find({});
        res.status(200).json(reports);
    } catch (e) { res.status(500).send("DB Error"); }
});

server.listen(port, () => console.log(`✅ FraudGuard Server is running on http://localhost:${port}`));