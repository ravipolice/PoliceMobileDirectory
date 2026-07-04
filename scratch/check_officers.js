const admin = require('firebase-admin');
const path = require('path');

const serviceAccount = require('../service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function checkOfficers() {
  const snapshot = await db.collection('officers').get();
  console.log('Total documents in officers:', snapshot.size);
  if (snapshot.size > 0) {
      const doc = snapshot.docs[0];
      console.log('Sample Document ID:', doc.id);
      console.log('Sample Data:', JSON.stringify(doc.data(), null, 2));
  }
  process.exit(0);
}

checkOfficers().catch(err => {
  console.error(err);
  process.exit(1);
});
