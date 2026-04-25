# 🛡️ AI Fraud Guard

An Android application that helps users detect and prevent fraud, scams, and cybercrime using AI-powered assistance and real-time news updates.

## ✨ Features

- **🤖 AI Assistant**: Chat with an AI assistant specialized in fraud detection and cybersecurity
- **📰 News Feed**: Stay updated with latest scam and fraud news from around the world
- **🔐 Secure Authentication**: Google Sign-In integration
- **📱 Modern UI**: Clean, intuitive interface with bottom navigation
- **🔄 Real-time Updates**: Live news feed with automatic refresh

## 📸 Screenshots

[Add your app screenshots here]

## 🚀 Getting Started

### Prerequisites

- Android Studio (latest version)
- Android SDK 24 or higher
- JDK 8 or higher

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/ai-fraud-guard.git
   cd ai-fraud-guard
   ```

2. **Set up API Keys**
   
   Copy the template file:
   ```bash
   cp apikeys.properties.template apikeys.properties
   ```
   
   Edit `apikeys.properties` and add your API keys:
   ```properties
   NEWS_API_KEY=your_newsapi_key_here
   GEMINI_API_KEY=your_gemini_api_key_here
   ```

3. **Get API Keys**

   - **NewsAPI**: Get your free API key from [newsapi.org](https://newsapi.org/)
   - **Gemini AI**: Get your API key from [Google AI Studio](https://makersuite.google.com/app/apikey)

4. **Set up Firebase** (for Google Sign-In)
   
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create a new project or use existing one
   - Add your Android app
   - Download `google-services.json` and place it in the `app/` directory

5. **Build and Run**
   
   Open the project in Android Studio and run it on your device or emulator.

## 🔑 API Keys Setup

This project uses the following APIs:

### NewsAPI
- **Purpose**: Fetch latest fraud and scam news
- **Get Key**: https://newsapi.org/
- **Free Tier**: 100 requests/day

### Gemini AI
- **Purpose**: AI-powered fraud detection assistant
- **Get Key**: https://makersuite.google.com/app/apikey
- **Free Tier**: Available

### Firebase (Google Sign-In)
- **Purpose**: User authentication
- **Setup**: https://console.firebase.google.com/

## 📁 Project Structure

```
ai-fraud-guard/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/aifraudguard/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── AIAssistantFragment.kt
│   │   │   │   ├── NewsFragment.kt
│   │   │   │   ├── ApiConfig.kt
│   │   │   │   └── ...
│   │   │   └── res/
│   │   └── ...
│   └── build.gradle.kts
├── apikeys.properties.template
├── .gitignore
└── README.md
```

## 🛠️ Built With

- **Kotlin** - Programming language
- **Android SDK** - Mobile platform
- **Jetpack Compose** - Modern UI toolkit
- **ViewPager2** - Swipeable pages
- **OkHttp** - HTTP client
- **Gson** - JSON parsing
- **Glide** - Image loading
- **Firebase** - Authentication
- **Material Design** - UI components

## 🔒 Security

- API keys are stored in `apikeys.properties` (not committed to git)
- Uses BuildConfig for secure key management
- Firebase authentication for user security
- HTTPS for all API communications

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👥 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📧 Contact

Your Name - [@yourtwitter](https://twitter.com/yourtwitter)

Project Link: [https://github.com/yourusername/ai-fraud-guard](https://github.com/yourusername/ai-fraud-guard)

## 🙏 Acknowledgments

- [NewsAPI](https://newsapi.org/) for news data
- [Google Gemini](https://ai.google.dev/) for AI capabilities
- [Firebase](https://firebase.google.com/) for authentication
- All contributors and supporters

## ⚠️ Disclaimer

This app is for educational and informational purposes only. Always verify information from official sources and report suspected fraud to appropriate authorities.

<!-- test comment -->