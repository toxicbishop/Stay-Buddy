const apiKey = "AIzaSyB2UOe0_vyxWo32GWxcRZBBW1aPPUelSt4";
const email = "bot@staybuddy.com";
const password = "TempPass123!";

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
