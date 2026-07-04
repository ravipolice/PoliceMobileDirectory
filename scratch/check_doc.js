const admin = require('firebase-admin');
const path = require('path');

const serviceAccount = require('../service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function checkDoc() {
  const doc = await db.collection('officers').doc('08Fh9K9cIwAiTbEuxeGx').get();
  console.log(JSON.stringify(doc.data(), null, 2));
  process.exit(0);
}

checkDoc().catch(console.error);
