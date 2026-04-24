import os
import requests
import base64
from difflib import SequenceMatcher
from urllib.parse import urlparse
from datetime import datetime, timezone
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from dotenv import load_dotenv

load_dotenv()

app = FastAPI(title="Link Inspector API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class UrlRequest(BaseModel):
    url: str

VIRUSTOTAL_API_KEY = os.getenv("VIRUSTOTAL_API_KEY", "")
GOOGLE_SAFE_BROWSING_API_KEY = os.getenv("GOOGLE_SAFE_BROWSING_API_KEY", "")

TRUSTED_DOMAINS = [
    "amazon.com", "google.com", "sbi.co.in", "paytm.com",
    "hdfc.com", "icici.com", "paypal.com", "flipkart.com",
    "npci.org.in", "yesbank.in", "axisbank.com", "phonepe.com",
    "bhimupi.org.in", "incometax.gov.in", "irctc.co.in"
]

SUSPICIOUS_KEYWORDS = [
    "login", "verify", "secure", "update", "account",
    "banking", "refund", "winner", "free", "lucky",
    "prize", "claim", "reward", "urgent", "suspend",
    "blocked", "kyc", "otp", "credential"
]

def check_https(url: str, domain: str):
    score = 0
    message = "Using secure HTTPS connection"
    status = "success"
    color = "green"
    icon = "✅"

    if url.startswith("http://"):
        score = 20
        message = "Unsafe HTTP connection detected"
        status = "failed"
        color = "red"
        icon = "🔴"
    elif "https" in domain and not url.startswith("https://"):
        score = 25
        message = "Attempting to spoof HTTPS in domain name"
        status = "failed"
        color = "red"
        icon = "🔴"
    elif not url.startswith("https://"):
        # if no protocol given, we assume it's risky
        score = 10
        message = "No secure protocol specified"
        status = "warning"
        color = "yellow"
        icon = "⚠️"

    return {"name": "HTTPS Check", "score": score, "message": message, "status": status, "color": color, "icon": icon}

def check_lookalike(domain: str):
    max_sim = 0.0
    matched_domain = None
    
    # Strip common subdomains for better matching
    clean_domain = domain.replace("www.", "")

    for trusted in TRUSTED_DOMAINS:
        if clean_domain == trusted:
            return {"name": "Lookalike Domain Check", "score": 0, "message": f"Exact match for trusted domain {trusted}", "status": "success", "color": "green", "icon": "✅"}
        sim = SequenceMatcher(None, clean_domain, trusted).ratio()
        if sim > max_sim:
            max_sim = sim
            matched_domain = trusted

    if max_sim > 0.75:
        return {"name": "Lookalike Domain Check", "score": 35, "message": f"Looks suspiciously like {matched_domain}", "status": "failed", "color": "red", "icon": "🔴"}

    return {"name": "Lookalike Domain Check", "score": 0, "message": "Domain does not mimic popular trusted brands", "status": "success", "color": "green", "icon": "✅"}

def check_keywords(url: str, domain: str):
    lower_url = url.lower()
    matches = []
    
    for word in SUSPICIOUS_KEYWORDS:
        if word in lower_url:
            matches.append(word)

    score = min(len(matches) * 10, 30)
    
    if score > 0:
        return {"name": "Suspicious Keywords", "score": score, "message": f"Found red-flag words: {', '.join(matches)}", "status": "warning" if score < 30 else "failed", "color": "yellow" if score < 30 else "red", "icon": "⚠️" if score < 30 else "🔴"}

    return {"name": "Suspicious Keywords", "score": 0, "message": "No typical phishing keywords found", "status": "success", "color": "green", "icon": "✅"}

def check_virustotal(url: str):
    if not VIRUSTOTAL_API_KEY:
        return {"name": "VirusTotal Check", "score": 0, "message": "Skipped properly (No API Key)", "status": "skipped", "color": "gray", "icon": "❓"}
        
    url_id = base64.urlsafe_b64encode(url.encode()).decode().strip("=")
    headers = {
        "accept": "application/json",
        "x-apikey": VIRUSTOTAL_API_KEY
    }
    
    try:
        resp = requests.get(f"https://www.virustotal.com/api/v3/urls/{url_id}", headers=headers, timeout=5)
        if resp.status_code == 200:
            data = resp.json()
            stats = data.get("data", {}).get("attributes", {}).get("last_analysis_stats", {})
            malicious = stats.get("malicious", 0)
            suspicious = stats.get("suspicious", 0)
            
            if malicious > 0:
                return {"name": "VirusTotal Check", "score": 40, "message": f"Flagged by {malicious} security vendors as malicious", "status": "failed", "color": "red", "icon": "🔴"}
            elif suspicious > 0:
                return {"name": "VirusTotal Check", "score": 20, "message": f"Flagged by {suspicious} security vendors as suspicious", "status": "warning", "color": "yellow", "icon": "⚠️"}
            else:
                return {"name": "VirusTotal Check", "score": 0, "message": "Clean, no security vendors flagged this URL", "status": "success", "color": "green", "icon": "✅"}
    except:
        pass

    return {"name": "VirusTotal Check", "score": 0, "message": "Could not fetch VirusTotal report", "status": "skipped", "color": "gray", "icon": "❓"}

def check_safe_browsing(url: str):
    if not GOOGLE_SAFE_BROWSING_API_KEY:
        return {"name": "Safe Browsing Check", "score": 0, "message": "Skipped (No API Key)", "status": "skipped", "color": "gray", "icon": "❓"}

    payload = {
        "client": {
            "clientId": "fraudguard-app",
            "clientVersion": "1.0.0"
        },
        "threatInfo": {
            "threatTypes": ["MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE"],
            "platformTypes": ["ANY_PLATFORM"],
            "threatEntryTypes": ["URL"],
            "threatEntries": [
                {"url": url}
            ]
        }
    }
    
    try:
        resp = requests.post(f"https://safebrowsing.googleapis.com/v4/threatMatches:find?key={GOOGLE_SAFE_BROWSING_API_KEY}", json=payload, timeout=5)
        if resp.status_code == 200:
            data = resp.json()
            if "matches" in data and len(data["matches"]) > 0:
                threats = ",".join(list(set([m.get("threatType", "THREAT") for m in data["matches"]])))
                return {"name": "Google Safe Browsing", "score": 50, "message": f"Flagged by Google as {threats}!", "status": "failed", "color": "red", "icon": "🔴"}
    except:
        pass
        
    return {"name": "Google Safe Browsing", "score": 0, "message": "Safe browsing found no known threats", "status": "success", "color": "green", "icon": "✅"}

@app.get("/health")
def health_check():
    return {"status": "ok"}

@app.post("/check-url")
def check_url(req: UrlRequest):
    url = req.url.strip()
    if not url:
        raise HTTPException(status_code=400, detail="URL cannot be empty")
        
    if not url.startswith("http://") and not url.startswith("https://"):
        url_to_parse = "http://" + url
    else:
        url_to_parse = url
        
    parsed = urlparse(url_to_parse)
    domain = parsed.netloc

    if not domain:
        raise HTTPException(status_code=400, detail="Invalid URL format")

    checks = []
    
    # Run Checks
    checks.append(check_https(url, domain))
    checks.append(check_lookalike(domain))
    checks.append(check_keywords(url, domain))
    checks.append(check_virustotal(url))
    checks.append(check_safe_browsing(url))

    final_score = min(sum(c["score"] for c in checks), 100)

    # Result Engine
    if final_score <= 30:
        verdict = "SAFE"
        color = "#4CAF50" # Green
        simple_message = "This link looks genuine. No major threats detected."
        recommendation = "You can proceed, but always manually verify the website branding."
    elif final_score <= 59:
        verdict = "SUSPICIOUS"
        color = "#FF9800" # Orange
        simple_message = "This link has warning signs. Avoid entering personal details."
        recommendation = "Proceed with caution. Do not input passwords or card details."
    else:
        verdict = "DANGEROUS"
        color = "#F44336" # Red
        simple_message = "This looks like a malicious or phishing link! Do NOT open or share any personal information."
        recommendation = "Close this page immediately and report the sender."

    return {
        "url": url,
        "final_score": final_score,
        "verdict": verdict,
        "color": color,
        "simple_message": simple_message,
        "recommendation": recommendation,
        "checks": checks
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
