const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

const serviceAccount = require('../service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function checkUnits() {
  const snapshot = await db.collection('units').get();
  snapshot.forEach(doc => {
    const data = doc.data();
    console.log(`ID: ${doc.id} | Name: "${data.name}" | MappingType: "${data.mappingType}" | MappedDistrictsCount: ${data.mappedDistricts ? data.mappedDistricts.length : 0} | Scopes: ${JSON.stringify(data.scopes)}`);
    if (data.name === 'L&O' || data.name === 'Law & Order') {
      console.log('--- Detail L&O ---');
      console.log(JSON.stringify(data, null, 2));
    }
  });
}

checkUnits().then(() => process.exit(0)).catch(err => {
  console.error(err);
  process.exit(1);
});
