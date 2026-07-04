const admin = require('firebase-admin');
const serviceAccount = require('../serviceAccountKey.json');

if (admin.apps.length === 0) {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
}

const db = admin.firestore();

async function run() {
  const snap = await db.collection('officers').get();
  
  let agidCount = 0;
  let kspCount = 0;
  let otherCount = 0;
  
  snap.forEach(doc => {
    const id = doc.id;
    if (id.startsWith('AGID')) {
      agidCount++;
    } else if (id.startsWith('KSP')) {
      kspCount++;
    } else {
      otherCount++;
    }
  });
  
  console.log(`Total documents: ${snap.size}`);
  console.log(`- Starts with 'AGID': ${agidCount}`);
  console.log(`- Starts with 'KSP': ${kspCount}`);
  console.log(`- Others: ${otherCount}`);
  
  process.exit(0);
}

run().catch(console.error);
