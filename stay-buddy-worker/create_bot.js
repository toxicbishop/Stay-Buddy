const apiKey = process.env.FIREBASE_API_KEY;
const email = process.env.BOT_EMAIL || "bot@staybuddy.com";
const password = process.env.BOT_PASSWORD;

if (!apiKey || !password) {
    console.error("Missing required environment variables: FIREBASE_API_KEY and BOT_PASSWORD");
    process.exit(1);
}

fetch(`https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=${apiKey}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password, returnSecureToken: true })
})
.then(res => res.json())
.then(data => {
    if (data.error && data.error.message !== 'EMAIL_EXISTS') {
        console.error("Error creating bot user:", data.error.message);
    } else {
        console.log("Bot user ready!");
    }
})
.catch(err => console.error(err));
