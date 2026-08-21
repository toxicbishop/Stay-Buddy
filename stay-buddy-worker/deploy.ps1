$env:WRANGLER_SEND_METRICS="false"
"$env:NOTION_API_KEY" | npx wrangler secret put NOTION_API_KEY
"$env:NOTION_DATABASE_ID" | npx wrangler secret put NOTION_DATABASE_ID
"$env:FIREBASE_PROJECT_ID" | npx wrangler secret put FIREBASE_PROJECT_ID
"$env:FIREBASE_WEB_API_KEY" | npx wrangler secret put FIREBASE_WEB_API_KEY
"$env:BOT_EMAIL" | npx wrangler secret put BOT_EMAIL
"$env:BOT_PASSWORD" | npx wrangler secret put BOT_PASSWORD
# IMPORTANT: You must paste your Firebase Service Account JSON below (minified/single-line) to make push notifications work!
# '<YOUR_FIREBASE_SERVICE_ACCOUNT_JSON>' | npx wrangler secret put FIREBASE_SERVICE_ACCOUNT_JSON

npx wrangler deploy
