const admin = require('firebase-admin');
const path = require('path');
const serviceAccount = require('../serviceAccountKey.json');

if (admin.apps.length === 0) {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
}

const db = admin.firestore();

async function run() {
  const empSnap = await db.collection('employees').get();
  console.log(`=== Unapproved Employees in 'employees' Collection ===`);
  
  let count = 0;
  empSnap.forEach(doc => {
    const data = doc.data();
    if (data.isApproved === false || data.rank === 'Pending Verification') {
      count++;
      console.log(`\n[#${count}] Name: "${data.name}"`);
      console.log(`    KGID: ${data.kgid || doc.id}`);
      console.log(`    Email: ${data.email}`);
      console.log(`    Mobile: ${data.mobile1}`);
      console.log(`    Rank: ${data.rank}`);
      console.log(`    isApproved: ${data.isApproved}`);
      console.log(`    Created At: ${data.createdAt ? data.createdAt.toDate() : 'N/A'}`);
    }
  });
  
  process.exit(0);
}

run().catch(console.error);
