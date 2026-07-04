const admin = require('firebase-admin');
const path = require('path');

const serviceAccount = require('../service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function getCounts() {
  const v1 = await db.collection('officers').get();
  const v2 = await db.collection('officers_v2').get();
  console.log('Count officers:', v1.size);
  console.log('Count officers_v2:', v2.size);
  process.exit(0);
}

getCounts().catch(console.error);
