const { execSync } = require('child_process');

const secrets = {
    NOTION_API_KEY: process.env.NOTION_API_KEY || "YOUR_NOTION_API_KEY",
    NOTION_DATABASE_ID: process.env.NOTION_DATABASE_ID || "YOUR_NOTION_DATABASE_ID",
    FIREBASE_PROJECT_ID: process.env.FIREBASE_PROJECT_ID || "stay-buddy-9d294",
    FIREBASE_WEB_API_KEY: process.env.FIREBASE_WEB_API_KEY || "YOUR_FIREBASE_WEB_API_KEY",
    BOT_EMAIL: process.env.BOT_EMAIL || "bot@staybuddy.com",
    BOT_PASSWORD: process.env.BOT_PASSWORD || "YOUR_BOT_PASSWORD"
};

// Deploy first so the worker exists
console.log("Deploying worker...");
try {
    execSync('npx wrangler deploy', { stdio: 'inherit' });
} catch (e) {
    console.error("Deploy failed:", e);
}

for (const [key, value] of Object.entries(secrets)) {
    console.log(`Setting secret ${key}...`);
    try {
        execSync(`npx wrangler secret put ${key}`, {
            input: value,
            stdio: ['pipe', 'inherit', 'inherit']
        });
    } catch (e) {
        console.error(`Failed to set ${key}`);
    }
}

console.log("Done!");
