from flask import Flask, request, jsonify
from transformers import DistilBertTokenizerFast, DistilBertForSequenceClassification
import torch
import torch.nn.functional as F

app = Flask(__name__)

# --- 1. Load Your Trained Model and Tokenizer ---
MODEL_PATH = "./my_final_fraud_model_v3"
tokenizer = DistilBertTokenizerFast.from_pretrained(MODEL_PATH)
model = DistilBertForSequenceClassification.from_pretrained(MODEL_PATH)
print("✅ AI Model loaded successfully!")

# --- 2. Create the Scam Content Library (UPDATED TERMINOLOGY) ---
SCAM_LIBRARY = {
    "1": {
        "scam_type_name": "Bank/OTP Impersonation Scam",
        "report_summary": "The caller attempted to impersonate a bank employee to illicitly obtain sensitive information like an OTP, citing issues like a blocked card or KYC verification.",
        "educational_summary": "You just encountered a common Bank Impersonation Scam. Scammers pretend to be from your bank and create a sense of panic. They will ask for your OTP, CVV, or passwords. REMEMBER: Your bank will NEVER ask for this information over the phone."
    },
    "2": {
        "scam_type_name": "Lottery/Prize Scam",
        "report_summary": "The caller falsely claimed the user has won a large prize or lottery and requested a processing fee or tax payment to claim it.",
        "educational_summary": "This was a classic Lottery Scam. They promise you a huge reward but ask for a small payment first for 'taxes' or 'fees'. Legitimate lotteries will never ask you to pay to receive your winnings. If an offer seems too good to be true, it always is."
    },
    "3": {
        "scam_type_name": "Urgent Payment Scam",
        "report_summary": "The caller created a false sense of urgency, threatening to disconnect a service like electricity unless an immediate payment was made.",
        "educational_summary": "This is an Urgent Payment Scam. Fraudsters try to make you panic and act without thinking. They often pretend to be from utility companies or government agencies. Always verify such claims by hanging up and calling the company's official customer care number yourself."
    },
    "4": {
        "scam_type_name": "Phishing/Link Scam",
        "report_summary": "The **caller** referred to a suspicious link or credential reset, often disguised as an alert, to steal personal credentials.",
        "educational_summary": "This is a **Voice Phishing (Vishing)** attempt. The caller's goal is to trick you into clicking a malicious link they mentioned or giving away login details. Always verify such requests by hanging up and checking official sources."
    },
    "5": {
        "scam_type_name": "General Fraud",
        "report_summary": "The call exhibits general characteristics of fraudulent communication, such as unusual requests for money or information.",
        "educational_summary": "This **call** shows general signs of a scam. Be cautious of unsolicited **calls** that ask for personal information or money. Verify such requests through another trusted channel."
    }
}


# --- 3. Define the Prediction Endpoint ---
@app.route('/predict', methods=['POST'])
def predict():
    data = request.json
    text_input = data.get('text')
    if not text_input:
        return jsonify({"error": "No text provided"}), 400

    # Tokenize the input text
    inputs = tokenizer(text_input, return_tensors="pt", padding=True, truncation=True)

    # Get prediction from the model
    with torch.no_grad():
        logits = model(**inputs).logits

    # Convert model output to probabilities
    probabilities = F.softmax(logits, dim=1).squeeze()
    
    # Get the predicted class (0, 1, 2, etc.) and the fraud score
    predicted_class_id = torch.argmax(probabilities).item()
    fraud_score = 1 - probabilities[0].item() # The score is 1 minus the probability of it being "Not a Scam"

    # Start with a default "safe" response
    response = {
        "fraud_score": fraud_score,
        "predicted_class_id": predicted_class_id,
        "scam_type_name": "Not a Scam",
        "report_summary": "N/A",
        "educational_summary": "This call appears to be safe. Stay vigilant!"
    }

    # If a scam is detected (class is not 0), get the correct summaries
    if predicted_class_id != 0:
        scam_details = SCAM_LIBRARY.get(str(predicted_class_id))
        if scam_details:
            response.update(scam_details)

    return jsonify(response)


# --- 4. Run the Server ---
if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)