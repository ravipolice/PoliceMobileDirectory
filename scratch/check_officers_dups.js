const admin = require('firebase-admin');
const serviceAccount = require('../serviceAccountKey.json');

if (admin.apps.length === 0) {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
}

const db = admin.firestore();

async function run() {
  console.log('=== Checking Officers Collection in Firestore ===');
  
  const snap = await db.collection('officers').get();
  console.log(`Total documents in 'officers' collection: ${snap.size}`);
  
  const nameMap = new Map();
  const mobileMap = new Map();
  const cfdMap = new Map();
  
  let duplicateNames = 0;
  let duplicateMobiles = 0;
  
  snap.forEach(doc => {
    const data = doc.data();
    const name = String(data.name || '').trim().toLowerCase();
    const mobile = String(data.mobile || '').trim();
    const cfd = String(data.cfd || data.agid || doc.id).trim().toLowerCase();
    
    if (name) {
      if (nameMap.has(name)) {
        nameMap.get(name).push(doc.id);
      } else {
        nameMap.set(name, [doc.id]);
      }
    }
    
    if (mobile && mobile !== 'NM' && mobile !== 'N/A') {
      if (mobileMap.has(mobile)) {
        mobileMap.get(mobile).push(doc.id);
      } else {
        mobileMap.set(mobile, [doc.id]);
      }
    }
    
    if (cfd) {
      if (cfdMap.has(cfd)) {
        cfdMap.get(cfd).push(doc.id);
      } else {
        cfdMap.set(cfd, [doc.id]);
      }
    }
  });
  
  console.log('\n--- Duplicate Names (showing first 10) ---');
  let nameDupCount = 0;
  for (const [name, ids] of nameMap.entries()) {
    if (ids.length > 1) {
      nameDupCount++;
      if (nameDupCount <= 10) {
        console.log(`- "${name}" exists in ${ids.length} docs: ${ids.join(', ')}`);
      }
    }
  }
  console.log(`Total duplicate names: ${nameDupCount}`);
  
  console.log('\n--- Duplicate Mobiles (showing first 10) ---');
  let mobileDupCount = 0;
  for (const [mobile, ids] of mobileMap.entries()) {
    if (ids.length > 1) {
      mobileDupCount++;
      if (mobileDupCount <= 10) {
        console.log(`- "${mobile}" exists in ${ids.length} docs: ${ids.join(', ')}`);
      }
    }
  }
  console.log(`Total duplicate mobiles: ${mobileDupCount}`);
  
  console.log('\n--- Duplicate CFD/AGID (showing first 10) ---');
  let cfdDupCount = 0;
  for (const [cfd, ids] of cfdMap.entries()) {
    if (ids.length > 1) {
      cfdDupCount++;
      if (cfdDupCount <= 10) {
        console.log(`- "${cfd}" exists in ${ids.length} docs: ${ids.join(', ')}`);
      }
    }
  }
  console.log(`Total duplicate CFD/AGID: ${cfdDupCount}`);
  
  process.exit(0);
}

run().catch(console.error);
