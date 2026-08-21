const { OAuth2Client } = require('google-auth-library');
const readline = require('readline');

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

console.log("=========================================");
console.log(" Firebase Vercel Authentication Generator  ");
console.log("=========================================\n");

console.log("Since Google blocks Service Account Keys for new projects, we will use an OAuth2 Refresh Token instead.");
console.log("Step 1: Go to https://console.cloud.google.com/apis/credentials (ensure your StayBuddy project is selected at the top).");
console.log("Step 2: Click 'Create Credentials' -> 'OAuth client ID'.");
console.log("Step 3: If it asks to configure the Consent Screen, choose 'External', fill in mandatory fields, and click Save.");
console.log("Step 4: For Application type, select 'Desktop app'. Name it anything and click Create.");
console.log("Step 5: Copy the Client ID and Client Secret.\n");

rl.question('Paste your Client ID: ', (clientId) => {
  rl.question('Paste your Client Secret: ', async (clientSecret) => {
    const oAuth2Client = new OAuth2Client(
      clientId.trim(),
      clientSecret.trim(),
      'urn:ietf:wg:oauth:2.0:oob'
    );

    const authUrl = oAuth2Client.generateAuthUrl({
      access_type: 'offline',
      scope: ['https://www.googleapis.com/auth/firebase.messaging'],
      prompt: 'consent' // Forces it to return a refresh token
    });

    console.log('\n=========================================');
    console.log('Step 6: Open this URL in your browser and sign in with your Google Account:');
    console.log(authUrl);
    console.log('=========================================\n');

    rl.question('Step 7: Paste the Authorization Code from the browser here: ', async (code) => {
      try {
        const { tokens } = await oAuth2Client.getToken(code.trim());
        
        const finalJson = {
          type: "authorized_user",
          client_id: clientId.trim(),
          client_secret: clientSecret.trim(),
          refresh_token: tokens.refresh_token
        };

        console.log('\nSUCCESS! Copy the entire JSON block below and paste it as the VALUE for the FIREBASE_SERVICE_ACCOUNT variable in Vercel:');
        console.log('\n' + JSON.stringify(finalJson, null, 2) + '\n');
        
      } catch (error) {
        console.error('Error generating token:', error.message);
      }
      rl.close();
    });
  });
});
