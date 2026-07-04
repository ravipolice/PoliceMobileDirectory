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
  
  const agidDocs = [];
  const kspMobiles = new Set();
  const kspNames = new Set();
  const kspCfds = new Set();
  
  snap.forEach(doc => {
    const data = doc.data();
    const id = doc.id;
    if (id.startsWith('AGID')) {
      agidDocs.push({ id, ...data });
    } else if (id.startsWith('KSP')) {
      const name = String(data.name || '').trim().toLowerCase();
      const mobile = String(data.mobile || '').trim();
      if (name) kspNames.add(name);
      if (mobile && mobile !== 'NM' && mobile !== 'N/A') kspMobiles.add(mobile);
    }
  });
  
  console.log(`Found ${agidDocs.length} AGID documents.`);
  
  let unmatchedCount = 0;
  for (const doc of agidDocs) {
    const name = String(doc.name || '').trim().toLowerCase();
    const mobile = String(doc.mobile || '').trim();
    
    const hasNameMatch = kspNames.has(name);
    const hasMobileMatch = mobile && mobile !== 'NM' && mobile !== 'N/A' && kspMobiles.has(mobile);
    
    if (!hasNameMatch && !hasMobileMatch) {
      unmatchedCount++;
      console.log(`Unmatched AGID doc: ID=${doc.id}, Name="${doc.name}", Mobile="${doc.mobile}"`);
    }
  }
  
  console.log(`\nTotal unmatched AGID documents: ${unmatchedCount}`);
  process.exit(0);
}

run().catch(console.error);
