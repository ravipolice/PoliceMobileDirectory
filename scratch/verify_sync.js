const admin = require('firebase-admin');
const path = require('path');

const serviceAccount = require('../service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function verifySync() {
  console.log('--- VERIFYING PRODUCTION SYNC ---');
  const snapshot = await db.collection('officers').limit(5).get();
  
  if (snapshot.empty) {
    console.error('❌ ERROR: No documents found in production officers collection!');
    process.exit(1);
  }

  let allValid = true;
  snapshot.forEach(doc => {
    const data = doc.data();
    const fields = ['agid', 'name', 'rank', 'searchBlob', 'unit', 'district'];
    const missing = fields.filter(f => !data[f]);
    
    if (missing.length > 0) {
      console.warn(`⚠️ Document ${doc.id} is missing fields: ${missing.join(', ')}`);
      allValid = false;
    } else {
      console.log(`✅ Document ${doc.id} verified: ${data.name} (${data.agid})`);
    }
  });

  if (allValid) {
    console.log('\n✨ SUCCESS: Production synchronization verified. Data is structured correctly.');
  } else {
    console.warn('\n⚠️ WARNING: Some documents have missing fields. Please check the logs.');
  }
  process.exit(0);
}

verifySync().catch(err => {
  console.error('❌ CRITICAL ERROR:', err);
  process.exit(1);
});
