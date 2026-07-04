const admin = require('firebase-admin');
const serviceAccount = require('../serviceAccountKey.json');

if (admin.apps.length === 0) {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
}

const db = admin.firestore();

async function run() {
  console.log('=== Searching for "sam" in Firestore ===');
  
  const empSnap = await db.collection('employees').get();
  console.log(`Employees containing "sam":`);
  empSnap.forEach(doc => {
    const name = doc.data().name || '';
    if (name.toLowerCase().includes('sam')) {
      console.log(`- Employee ID: ${doc.id}, Name: "${name}", isApproved: ${doc.data().isApproved}`);
    }
  });

  const offSnap = await db.collection('officers').get();
  console.log(`\nOfficers containing "sam":`);
  offSnap.forEach(doc => {
    const name = doc.data().name || '';
    if (name.toLowerCase().includes('sam')) {
      console.log(`- Officer ID: ${doc.id}, Name: "${name}", Mobile: "${doc.data().mobile}"`);
    }
  });
  
  process.exit(0);
}

run().catch(console.error);
