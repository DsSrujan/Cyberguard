import requests
import json

resp = requests.post(
    'http://127.0.0.1:8000/check-url',
    json={'url': 'http://amaz0n.com/secure-login/verify-account-update'}
)
with open('test.json', 'w', encoding='utf-8') as f:
    json.dump(resp.json(), f, indent=2)
