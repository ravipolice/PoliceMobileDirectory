const admin = require('firebase-admin');
const path = require('path');

const serviceAccount = require('../service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function checkTotal() {
  console.log('Counting documents in officers_v2...');
  const snapshot = await db.collection('officers_v2').get();
  console.log('Total documents:', snapshot.size);
  process.exit(0);
}

checkTotal().catch(err => {
  console.error('Error:', err);
  process.exit(1);
});
