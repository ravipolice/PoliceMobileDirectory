const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

const serviceAccount = require('../service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function downloadCollection() {
  console.log('Fetching officers_v2 collection...');
  const snapshot = await db.collection('officers_v2').get();
  const data = [];
  
  snapshot.forEach(doc => {
    data.push(doc.data());
  });

  const outputPath = path.join(__dirname, '..', 'officers_v2_current.json');
  fs.writeFileSync(outputPath, JSON.stringify(data, null, 2));
  console.log(`Successfully downloaded ${data.length} records to ${outputPath}`);
  process.exit(0);
}

downloadCollection().catch(err => {
  console.error('Error downloading collection:', err);
  process.exit(1);
});
