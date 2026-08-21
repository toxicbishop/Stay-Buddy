// Upload cities and areas to Firestore using Application Default Credentials
// Run: node firestore-seed/upload.js

const { Firestore } = require('@google-cloud/firestore');
const fs = require('fs');

const db = new Firestore({ projectId: 'stay-buddy-9d294' });

async function upload() {
  console.log('🔄 Uploading cities...');

  // Upload cities
  const cities = JSON.parse(fs.readFileSync(__dirname + '/cities.json', 'utf8'));
  await db.collection('location_data').doc('cities').set(cities);
  console.log('✅ Cities uploaded:', Object.keys(cities.cities).length, 'cities');

  console.log('🔄 Uploading areas...');

  // Upload areas
  const areas = JSON.parse(fs.readFileSync(__dirname + '/areas.json', 'utf8'));
  await db.collection('location_data').doc('areas').set(areas);
  console.log('✅ Areas uploaded:', Object.keys(areas.areas).length, 'cities with areas');

  // Count total areas
  let totalAreas = 0;
  for (const city of Object.values(areas.areas)) {
    totalAreas += Object.keys(city).length;
  }
  console.log('📊 Total areas:', totalAreas);

  process.exit(0);
}

upload().catch(err => {
  console.error('❌ Error:', err.message);
  process.exit(1);
});
