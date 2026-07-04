const admin = require('firebase-admin');
const path = require('path');

const serviceAccount = require('../service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function checkAll() {
  const cols = await db.listCollections();
  for (const col of cols) {
      const snap = await col.get();
      console.log(`Collection ${col.id}: ${snap.size}`);
  }
  process.exit(0);
}

checkAll().catch(console.error);
