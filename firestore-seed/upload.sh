#!/bin/bash
# Upload cities and areas to Firestore using Firebase CLI
# Run: bash firestore-seed/upload.sh

PROJECT="stay-buddy-9d294"

echo "Uploading cities..."
npx firebase-tools firestore:delete location_data/cities --project $PROJECT --recursive --yes 2>/dev/null
npx firebase-tools firestore:import --project $PROJECT --collection-path location_data --backup-uri gs://$PROJECT.appspot.com/backup 2>&1 | head -5

echo ""
echo "Alternatively, use Firebase Console:"
echo "1. Go to https://console.firebase.google.com/project/$PROJECT/firestore"
echo "2. Open location_data collection"
echo "3. Update 'cities' document with cities.json data"
echo "4. Create/update 'areas' document with areas.json data"
echo ""
echo "Data files are in firestore-seed/ directory"
