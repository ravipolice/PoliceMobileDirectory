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
  const kspDocs = [];
  
  snap.forEach(doc => {
    const data = doc.data();
    const id = doc.id;
    if (id.startsWith('AGID')) {
      agidDocs.push({ id, ...data });
    } else if (id.startsWith('KSP')) {
      kspDocs.push({ id, ...data });
    }
  });
  
  let resolvedCount = 0;
  let unresolved = [];
  
  for (const agidDoc of agidDocs) {
    const agidName = String(agidDoc.name || '').trim().toLowerCase();
    const agidMobile = String(agidDoc.mobile || '').trim();
    
    // Find closest match in KSP
    const matches = kspDocs.filter(kspDoc => {
      const kspName = String(kspDoc.name || '').trim().toLowerCase();
      const kspMobile = String(kspDoc.mobile || '').trim();
      
      // Exact mobile match
      if (agidMobile && agidMobile !== 'NM' && agidMobile !== 'N/A' && agidMobile === kspMobile) {
        return true;
      }
      
      // Name similarity match (either exact or one contains the other)
      if (agidName === kspName) return true;
      
      // Strip special characters and check
      const cleanAgidName = agidName.replace(/[^a-z0-9]/g, '');
      const cleanKspName = kspName.replace(/[^a-z0-9]/g, '');
      if (cleanAgidName === cleanKspName && cleanAgidName.length > 3) return true;
      
      return false;
    });
    
    if (matches.length > 0) {
      resolvedCount++;
    } else {
      unresolved.push(agidDoc);
    }
  }
  
  console.log(`Total AGID: ${agidDocs.length}`);
  console.log(`Resolved (matched): ${resolvedCount}`);
  console.log(`Unresolved: ${unresolved.length}`);
  
  if (unresolved.length > 0) {
    console.log('\nSample unresolved (first 10):');
    unresolved.slice(0, 10).forEach(u => {
      console.log(`- ID: ${u.id} | Name: "${u.name}" | Mobile: "${u.mobile}" | Landline: "${u.landline}"`);
    });
  }
  
  process.exit(0);
}

run().catch(console.error);
