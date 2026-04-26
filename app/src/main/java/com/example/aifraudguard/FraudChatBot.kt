package com.example.aifraudguard

/**
 * Local offline fraud-prevention chatbot.
 * Provides intelligent responses without any API calls.
 * Used as primary or fallback when Gemini API is unavailable.
 */
object FraudChatBot {

    data class KnowledgeEntry(val keywords: List<String>, val response: String)

    private val knowledge = listOf(

        // ── OTP / Banking ─────────────────────────────────────────────────────
        KnowledgeEntry(
            listOf("otp", "one time password", "share otp", "otp fraud", "bank otp"),
            """🔐 **OTP (One-Time Password) Fraud**

OTPs are the most targeted secret in phone scams. Here's what you must know:

• **No bank, government body, or company will EVER ask for your OTP** — not even their own employees.
• Scammers pose as bank executives, TRAI officers, or RBI agents to urgently request your OTP.
• If you share an OTP, money can be instantly stolen from your account.

✅ **What to do:**
→ Never share OTP with anyone, ever.
→ If you received an OTP you didn't request, your account may be targeted — change your password immediately.
→ Report to your bank and cybercrime.gov.in"""
        ),

        // ── UPI / PhonePe / GPay ──────────────────────────────────────────────
        KnowledgeEntry(
            listOf("upi", "phonepe", "gpay", "google pay", "paytm", "bhim", "upi fraud", "upi pin"),
            """💸 **UPI Fraud**

UPI scams are the #1 digital fraud in India. Common tricks:

• **Collect request scam**: Scammer sends a "collect" request pretending it's a refund — entering your UPI PIN will debit your account, NOT credit it.
• **Fake QR codes**: Scanning a malicious QR code can trigger unauthorized payments.
• **Screen sharing**: Fraudsters ask you to share your screen over AnyDesk/TeamViewer to steal your PIN.
• **Fake customer care numbers**: Searching "PhonePe customer care" on Google often shows fake numbers.

✅ **Golden Rule:** Receiving money NEVER requires entering your UPI PIN. If someone asks you to enter your PIN to "receive" money — it's a scam."""
        ),

        // ── Phishing ──────────────────────────────────────────────────────────
        KnowledgeEntry(
            listOf("phishing", "fake website", "fake link", "suspicious link", "phishing email", "phishing sms"),
            """🎣 **Phishing Attacks**

Phishing creates fake lookalike pages to steal your credentials.

**How to spot a phishing link:**
• The URL has a slight misspelling (e.g., "sbi-bank.net" instead of "sbi.co.in")
• The site uses HTTP instead of HTTPS (no lock icon)
• It asks for your password, OTP, or card details suddenly
• Received via WhatsApp, SMS, or email with urgency ("Your account will be blocked!")

✅ **Protect yourself:**
→ Always type the URL manually instead of clicking links
→ Use CyberGuard's Link Inspector tab to scan any suspicious URL
→ Enable 2-factor authentication on all accounts"""
        ),

        // ── KYC ───────────────────────────────────────────────────────────────
        KnowledgeEntry(
            listOf("kyc", "kyc update", "kyc fraud", "kyc expired", "complete kyc"),
            """📋 **KYC Fraud**

"Your KYC is incomplete — account will be blocked in 24 hours!" — this is a classic scam.

Real banks never:
• Ask you to share an Aadhaar/PAN photo over WhatsApp
• Ask you to install remote access apps (AnyDesk, QuickSupport) for KYC
• Ask for your PIN/OTP during KYC

✅ **Verify KYC only** by visiting your bank branch in person or through the bank's official app downloaded from the Play Store."""
        ),

        // ── Lottery / Prize ────────────────────────────────────────────────────
        KnowledgeEntry(
            listOf("lottery", "prize", "winner", "won", "lucky draw", "congratulations", "reward"),
            """🎰 **Lottery & Prize Scams**

"Congratulations! You've won ₹25,00,000 in KBC Lucky Draw!" — **100% scam**.

Red flags:
• You didn't enter any contest
• They ask for "processing fee" or "tax" to release the prize
• They ask for your bank details or Aadhaar number
• Contact is via WhatsApp or unknown number

✅ **Reality check:** Legitimate lotteries do not contact random individuals. No real prize requires you to pay money first. Delete and block immediately."""
        ),

        // ── Job Scams ──────────────────────────────────────────────────────────
        KnowledgeEntry(
            listOf("job", "work from home", "part time job", "earn money", "online job", "job offer", "fake job"),
            """💼 **Fake Job / Work-From-Home Scams**

"Earn ₹5000/day working from home — like videos, subscribe channels!" — scam.

Common patterns:
• Initial small payments to build trust (₹200–500)
• Then asked to "invest" or pay a "subscription fee" to access more tasks
• Telegram-based schemes that disappear after collecting money
• LinkedIn/Naukri messages from fake recruiters asking for registration fees

✅ **Safe job search:** Use official platforms (Naukri, LinkedIn, Indeed). Legitimate employers NEVER charge fees. If it sounds too good to be true, it is."""
        ),

        // ── Investment / Crypto ────────────────────────────────────────────────
        KnowledgeEntry(
            listOf("investment", "crypto", "bitcoin", "trading", "stock tips", "forex", "double money", "profit"),
            """📈 **Investment & Crypto Fraud**

"Our AI trading bot gives 300% returns in 30 days!" — guaranteed scam.

Warning signs:
• Guaranteed high returns with no risk
• Pressure to invest quickly before "offer expires"
• Asking you to join a WhatsApp/Telegram "trading group"
• Asking for money in crypto (hard to trace/recover)
• Celebrity endorsements that look fake

✅ **Protect your money:**
→ SEBI-registered advisors never guarantee returns
→ Verify any investment firm at sebi.gov.in
→ Never invest money you can't afford to lose based on social media tips"""
        ),

        // ── What to do if scammed ──────────────────────────────────────────────
        KnowledgeEntry(
            listOf("scammed", "lost money", "fraud happened", "cheated", "what to do", "help", "report fraud", "report"),
            """🆘 **What to Do If You've Been Scammed**

Act immediately — speed matters:

1. **Call 1930** (National Cyber Crime Helpline) within minutes to freeze the fraudulent transfer
2. **File a complaint** at cybercrime.gov.in — available 24/7
3. **Contact your bank** immediately to block your account/card
4. **Change all passwords** for banking, email, and UPI apps
5. **File an FIR** at your nearest police station with all transaction details

⏱️ Golden window: Most banks can reverse transactions if reported within **2–4 hours.** Don't delay!"""
        ),

        // ── Social Media Scams ─────────────────────────────────────────────────
        KnowledgeEntry(
            listOf("instagram", "facebook", "whatsapp", "social media", "fake account", "impersonation", "hacked account"),
            """📱 **Social Media Scams**

Common attacks via social platforms:

• **Fake friend in trouble**: "I'm stuck abroad, please transfer money — I'll return it"
• **Hacked friend accounts**: Message from a known contact asking to borrow money
• **Fake giveaways**: "Share this post and win iPhone" — used to harvest data
• **Romance scams**: Building emotional connection over weeks then asking for money
• **Fake customer care**: Fake brand accounts replying to complaints with phishing links

✅ **Always verify** by calling the person directly before sending any money, even if the message looks legitimate."""
        ),

        // ── General safety tips ────────────────────────────────────────────────
        KnowledgeEntry(
            listOf("tips", "safe", "protect", "safety", "secure", "how to stay safe", "advice"),
            """🛡️ **Top 10 Cybersecurity Tips**

1. 🔐 Never share OTP, PIN, or password with anyone
2. 🔗 Scan suspicious links with CyberGuard's Link Inspector
3. 📩 Check message senders with CyberGuard's Message Scanner
4. 🔄 Enable 2-Factor Authentication (2FA) on all accounts
5. 📲 Download apps only from official Play Store/App Store
6. 🌐 Look for HTTPS and a padlock icon on all websites
7. 💳 Never save card details on unknown websites
8. 📞 Hang up on callers asking for OTP/PIN — banks never call for this
9. 🔍 Search customer care numbers only on official websites
10. 🆘 Report fraud immediately to 1930 or cybercrime.gov.in"""
        ),

        // ── About app ─────────────────────────────────────────────────────────
        KnowledgeEntry(
            listOf("what can you do", "features", "help me", "what is cyberguard", "about", "how does this app work"),
            """🛡️ **CyberGuard — Your Digital Safety Companion**

I'm FraudGuard Assist, built to keep you safe online. Here's what I can help with:

• **Link Inspector** → Paste any URL to check if it's a phishing/malware site
• **Message Scanner** → Analyze SMS or email text for fraud patterns
• **AI Assistant** → Ask me anything about scams, fraud, or cybersecurity (that's me!)
• **Scam News** → Stay updated on the latest frauds happening in India and globally

Ask me about: OTP fraud, UPI scams, phishing, job scams, investment fraud, or what to do if you've been scammed."""
        )
    )

    private val greetings = listOf("hi", "hello", "hey", "hii", "helo", "namaste", "good morning", "good evening")

    fun getResponse(userMessage: String): String {
        val lower = userMessage.lowercase().trim()

        // Handle greetings
        if (greetings.any { lower == it || lower.startsWith("$it ") || lower.endsWith(" $it") }) {
            return """👋 **Hello! I'm FraudGuard Assist.**

I can help you with:
• 🔐 OTP and UPI fraud prevention
• 🎣 Phishing link and fake website detection
• 💼 Job scam and investment fraud awareness
• 🆘 What to do if you've been scammed
• 🛡️ General cybersecurity tips

Just ask me anything about online fraud and scams! You can also use the **Link Inspector** and **Message Scanner** tabs above for real-time analysis."""
        }

        // Match against knowledge base
        val matched = knowledge.filter { entry ->
            entry.keywords.any { keyword -> lower.contains(keyword) }
        }

        if (matched.isNotEmpty()) {
            // Return the best match (most keyword hits)
            val best = matched.maxByOrNull { entry ->
                entry.keywords.count { keyword -> lower.contains(keyword) }
            }
            return best?.response ?: matched.first().response
        }

        // Default response
        return """🤔 I'm not sure about that specific topic, but I can help with:

• **OTP / UPI fraud** — how they work and how to stay safe
• **Phishing links** — how to identify and avoid fake websites  
• **Lottery / Prize scams** — why you didn't really win
• **Job scams** — fake work-from-home offers
• **Investment fraud** — crypto and trading scams
• **What to do if scammed** — step-by-step recovery guide

Also try our **Link Inspector** tab to check any suspicious URL, or **Message Scanner** to analyze a suspicious SMS or email!

*Tip: Ask me "give me safety tips" for a quick fraud prevention guide.*"""
    }
}
