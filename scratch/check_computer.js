const admin = require('firebase-admin');

const serviceAccount = require('../service-account.json');

if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
}

const db = admin.firestore();

async function checkComputer() {
  const doc = await db.collection('units').doc('w055TSvICtdCsWSErxxF').get();
  console.log(JSON.stringify(doc.data(), null, 2));
}

checkComputer().then(() => process.exit(0)).catch(err => {
  console.error(err);
  process.exit(1);
});
