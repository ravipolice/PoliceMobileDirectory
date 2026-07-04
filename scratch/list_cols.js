const admin = require('firebase-admin');
const path = require('path');

const serviceAccount = require('../service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function listCollections() {
  const cols = await db.listCollections();
  console.log('Collections:', cols.map(c => c.id));
  process.exit(0);
}

listCollections().catch(err => {
  console.error(err);
  process.exit(1);
});
