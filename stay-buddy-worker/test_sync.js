const { Client } = require("@notionhq/client");

const env = {
    NOTION_API_KEY: process.env.NOTION_API_KEY || "YOUR_NOTION_API_KEY",
    NOTION_DATABASE_ID: process.env.NOTION_DATABASE_ID || "YOUR_NOTION_DATABASE_ID",
    FIREBASE_PROJECT_ID: process.env.FIREBASE_PROJECT_ID || "stay-buddy-9d294",
    FIREBASE_WEB_API_KEY: process.env.FIREBASE_WEB_API_KEY || "YOUR_FIREBASE_WEB_API_KEY",
    BOT_EMAIL: process.env.BOT_EMAIL || "bot@staybuddy.com",
    BOT_PASSWORD: process.env.BOT_PASSWORD || "YOUR_BOT_PASSWORD"
};

async function testSync() {
    try {
        console.log("Fetching Notion tickets...");
        const notion = new Client({ auth: env.NOTION_API_KEY });
        const response = await notion.databases.query({
            database_id: env.NOTION_DATABASE_ID,
            sorts: [{ timestamp: "last_edited_time", direction: "descending" }],
            page_size: 5
        });

        console.log(`Found ${response.results.length} tickets`);
        if (response.results.length === 0) return;

        console.log("Authenticating bot...");
        const url = `https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${env.FIREBASE_WEB_API_KEY}`;
        const authRes = await fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email: env.BOT_EMAIL, password: env.BOT_PASSWORD, returnSecureToken: true })
        });
        const authData = await authRes.json();
        
        if (!authData.idToken) {
            console.error("Auth failed:", authData);
            return;
        }
        
        const token = authData.idToken;
        console.log("Got token.");

        for (const page of response.results) {
            const ticketId = page.properties["Ticket ID"]?.title[0]?.plain_text;
            const status = page.properties["Status"]?.select?.name;
            
            console.log(`Ticket ${ticketId} -> Status: ${status}`);

            if (ticketId && status) {
                const patchUrl = `https://firestore.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/databases/(default)/documents/support_tickets/${ticketId}?updateMask.fieldPaths=status`;
                console.log(`Patching ${patchUrl}...`);
                const patchRes = await fetch(patchUrl, {
                    method: "PATCH",
                    headers: {
                        "Authorization": `Bearer ${token}`,
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({ fields: { status: { stringValue: status } } })
                });
                
                const patchData = await patchRes.json();
                console.log("Patch Response:", JSON.stringify(patchData, null, 2));
            }
        }
    } catch (e) {
        console.error("Test failed:", e);
    }
}

testSync();
