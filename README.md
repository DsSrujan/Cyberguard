# 🛡️ CyberGuard — Intelligent Phishing Detection System

> **Hackathon Submission** | Problem Statement #1: Intelligent Phishing Detection System

---

## 📌 Problem Statement

### Problem
With the rapid growth of digital communication, phishing attacks through emails, SMS, and malicious URLs are increasing at an alarming rate, putting users and organizations at significant financial and privacy risk.

### Challenge
Build a system that can detect phishing attempts using:
- **Email/SMS text analysis** — scanning message content for fraud patterns
- **URL inspection and reputation scoring** — evaluating links for malicious behavior
- **Rule-based and/or AI-driven techniques** — combining classical ML with LLMs

### Goal
Enable users and organizations to identify and prevent phishing attacks in real-time, improving digital trust and security.

---

## 🎯 Our Solution: CyberGuard

CyberGuard is a full-stack Android application that brings together real-time AI analysis, machine learning-based message scanning, and URL reputation scoring in one cohesive, production-ready mobile experience.

---

## ✨ Key Features

| Feature | Description | Technology |
|---|---|---|
| 🔍 **Link Inspector** | Paste any URL to get an instant phishing risk score | FastAPI + VirusTotal API |
| 📩 **Message Scanner** | Detects SMS/Email phishing patterns with fraud score & smart advice | On-device ML + keyword analysis |
| 🤖 **AI Assistant** | Chat-based AI for fraud questions and cybercrime awareness | Google Gemini 1.5 Flash |
| 📰 **Scam News Feed** | Live cybercrime news from around the world | NewsAPI |
| 🔐 **Secure Auth** | Google Sign-In with Firebase; no passwords stored | Firebase Authentication |
| 📊 **Risk Score Visualization** | Animated circular progress indicator with color-coded verdict | Material 3 |
| 🚨 **Contextual Fraud Advice** | Smart, context-sensitive warnings (Financial Fraud, Lottery Scam, etc.) | Kotlin |
| 📞 **Call Overlay** | Real-time scam risk indicator during live phone calls | Android Overlay Service |
| 🎙️ **Voice Scam Detection** | Transcribes and analyzes call audio for fraud patterns | Node.js + Google Speech-to-Text |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Android App (Kotlin)                   │
│                                                             │
│  ┌──────────┐  ┌────────────┐  ┌──────────┐  ┌──────────┐  │
│  │  News    │  │   Link     │  │  AI      │  │  Msg     │  │
│  │  Feed    │  │ Inspector  │  │ Assistant│  │ Scanner  │  │
│  └────┬─────┘  └─────┬──────┘  └────┬─────┘  └────┬─────┘  │
│       │               │              │               │       │
│       │         ┌─────┴──────┐       │        On-Device ML  │
│       │         │  FastAPI   │       │        (vocabulary    │
│       │         │  Backend   │       │         .json)        │
│       │         │ (Python)   │       │                       │
│       │         └─────┬──────┘       │                       │
│       │         VirusTotal API       │                       │
│       │                              │                       │
│  NewsAPI                     Gemini 1.5 Flash API            │
└─────────────────────────────────────────────────────────────┘
         ┌──────────────────────────────────────┐
         │      Node.js Proxy Server             │
         │  • Twilio Voice Relay                 │
         │  • Google Cloud Speech-to-Text        │
         │  • Real-time call fraud analysis      │
         └──────────────────────────────────────┘
```

---

## 🔬 Technical Implementation

### 1. Link Inspector (URL Phishing Detection)
- **Backend**: Python FastAPI server (`Server/FraudGuardServer/main.py`)
- **Analysis checks**:
  - HTTPS vs. HTTP protocol validation
  - Domain reputation scoring
  - URL pattern analysis (suspicious TLDs, long URLs)
  - VirusTotal API integration for real-time threat intelligence
  - IP-based URL detection
- **Output**: Risk score (0–100) with category breakdown (Malware, Phishing, Suspicious)

### 2. Message Scanner (SMS/Email Phishing Detection)
- **On-device ML**: Uses a local `vocabulary.json` (5,000+ scam keywords) for offline-first analysis
- **Signal detection** categories:
  - Financial fraud keywords (OTP, UPI, bank, account)
  - Suspicious URLs embedded in messages
  - Urgency/pressure language
  - Prize/lottery scams
  - Government/official impersonation
- **Output**: Fraud score (0.0–1.0) with SAFE / SUSPICIOUS / FRAUD verdict + AI-generated contextual advice

### 3. AI Assistant (Gemini-powered Chatbot)
- **Model**: Google Gemini 1.5 Flash via REST API (`v1beta`)
- **Focus**: Cybercrime awareness, fraud prevention, scam identification
- **Features**: Rate-limit throttling to respect free-tier quotas (15 req/min)

### 4. Voice Scam Detection
- **Stack**: Twilio Media Streams → Node.js WebSocket server → Google Cloud Speech-to-Text
- **Function**: Transcribes live call audio and analyzes for known scam scripts in real-time

### 5. Security Architecture
- API keys stored exclusively in `local.properties` (Android) and `.env` (Node.js)
- Both files are Git-ignored and fully excluded from version history
- Firebase Auth tokens used for session management
- All external API calls made over HTTPS

---

## 📁 Project Structure

```
Scam-Detection/
├── app/                                  # Android Application
│   └── src/main/java/.../aifraudguard/
│       ├── MainActivity.kt               # Dashboard + Google photo sync
│       ├── AIAssistantFragment.kt        # Gemini AI chatbot
│       ├── LinkInspectorFragment.kt      # URL safety checker
│       ├── MessageScanFragment.kt        # SMS/Email fraud scanner
│       ├── NewsFragment.kt               # Live scam news feed
│       ├── FraudDetector.kt              # On-device ML engine
│       ├── AuthHelper.kt                 # Firebase auth wrapper
│       ├── OverlayService.kt             # Call overlay
│       └── ApiConfig.kt                  # Secure BuildConfig keys
│
├── Server/FraudGuardServer/
│   ├── server.js                         # Node.js proxy + Twilio + Speech-to-Text
│   ├── main.py                           # FastAPI URL analysis engine
│   └── requirements_fastapi.txt          # Python dependencies
│
├── app/src/main/assets/
│   └── vocabulary.json                   # 5,000+ scam keyword patterns
│
├── local.properties                      # 🔒 Secret keys (NEVER commit)
├── .gitignore                            # Excludes local.properties & .env
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK 24+ (Android 7.0 Nougat and above)
- Python 3.10+ (for FastAPI backend)
- Node.js 18+ (for proxy server)
- JDK 17

### 1. Clone the Repository
```bash
git clone https://github.com/DsSrujan/Cyberguard.git
cd Cyberguard
```

### 2. Configure API Keys
Create `local.properties` in the root directory (this file will **never** be committed):
```properties
sdk.dir=C:\Users\YourUser\AppData\Local\Android\Sdk
NEWS_API_KEY=your_newsapi_key
GEMINI_API_KEY=your_gemini_api_key
```

Create `Server/FraudGuardServer/.env`:
```env
VIRUSTOTAL_API_KEY=your_virustotal_key
GOOGLE_APPLICATION_CREDENTIALS=path/to/service-account.json
TWILIO_ACCOUNT_SID=your_twilio_sid
TWILIO_AUTH_TOKEN=your_twilio_token
```

### 3. Firebase Setup
- Go to [Firebase Console](https://console.firebase.google.com/)
- Add an Android app with package name `com.example.aifraudguard`
- Download `google-services.json` and place it inside `app/`

### 4. Run the Backends

**FastAPI (URL Inspector):**
```bash
cd Server/FraudGuardServer
pip install -r requirements_fastapi.txt
python main.py
# Runs on http://localhost:8000
```

**Node.js (Voice Proxy):**
```bash
cd Server/FraudGuardServer
npm install
node server.js
# Runs on http://localhost:3001
```

**Ngrok (for physical device testing):**
```bash
ngrok http 3001
```

### 5. Build & Run
Open the project in Android Studio and press the **Green Play button** ▶️

---

## 🔑 API Keys Required

| Service | Purpose | Get Key |
|---|---|---|
| [NewsAPI](https://newsapi.org/) | Live cybercrime news | Free – 100 req/day |
| [Google Gemini](https://aistudio.google.com/app/apikey) | AI chatbot assistant | Free – 15 req/min |
| [VirusTotal](https://www.virustotal.com/gui/my-apikey) | URL reputation scoring | Free tier available |
| [Firebase](https://console.firebase.google.com/) | Google Sign-In / Auth | Free |
| [Google Cloud](https://cloud.google.com/) | Speech-to-Text (Voice) | Pay-as-you-go |
| [Twilio](https://www.twilio.com/) | Call media streaming | Trial available |

---

## 🛠️ Tech Stack

**Android (Frontend)**
- Kotlin, ViewPager2, Material Design 3
- OkHttp, Glide, Firebase Auth
- ShapeableImageView, CircularProgressIndicator

**Backend (Python)**
- FastAPI, Uvicorn
- VirusTotal API, tldextract, requests

**Backend (Node.js)**
- Express.js, Twilio, ws (WebSocket)
- Google Cloud Speech-to-Text

---

## 🔒 Security Practices
- All API secrets stored in git-ignored local files only
- Complete Git history scrubbed of any previously leaked keys
- HTTPS enforced for all API communication
- Firebase token-based session management
- OkHttp 30s timeout with graceful error handling

---

## 👥 Team

Built for **Hackathon 2026** — Problem Statement #1: Intelligent Phishing Detection System.

---

## ⚠️ Disclaimer

CyberGuard is developed for educational and hackathon demonstration purposes. Always verify threats through official channels and report confirmed fraud to appropriate authorities (cybercrime.gov.in in India).
