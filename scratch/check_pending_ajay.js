const admin = require('firebase-admin');
const path = require('path');

const serviceAccount = require('../serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function run() {
  console.log('--- Checking Ajay Gupta in pending_registrations ---');
  const pendingSnap = await db.collection('pending_registrations').get();
  let pendingUser = null;
  pendingSnap.forEach(doc => {
    const data = doc.data();
    if (data.name && data.name.toUpperCase().includes('AJAY')) {
      pendingUser = data;
      console.log('Pending Doc ID:', doc.id);
      console.log('kgid:', data.kgid);
      console.log('email:', data.email);
      console.log('mobile1:', data.mobile1);
      console.log('status:', data.status);
    }
  });

  if (!pendingUser) {
    console.log('No pending Ajay found');
    process.exit(0);
  }

  console.log('\n--- Checking matches in employees collection ---');
  
  // Query all employees and search for matches manually
  const empSnap = await db.collection('employees').get();
  console.log(`Total employees in db: ${empSnap.size}`);
  
  let matchCount = 0;
  empSnap.forEach(doc => {
    const emp = doc.data();
    
    const pendingKgidStr = String(pendingUser.kgid || '').trim().toLowerCase();
    const empKgidStr = String(emp.kgid || '').trim().toLowerCase();
    const matchKgid = pendingKgidStr && empKgidStr && pendingKgidStr === empKgidStr;
    
    const pendingEmailStr = String(pendingUser.email || '').trim().toLowerCase();
    const empEmailStr = String(emp.email || '').trim().toLowerCase();
    const matchEmail = pendingEmailStr && empEmailStr && pendingEmailStr === empEmailStr;
    
    const pendingMobileStr = String(pendingUser.mobile1 || '').trim();
    const empMobile1Str = String(emp.mobile1 || '').trim();
    const empMobile2Str = String(emp.mobile2 || '').trim();
    const matchMobile1 = pendingMobileStr && (pendingMobileStr === empMobile1Str || pendingMobileStr === empMobile2Str);
    
    if (matchKgid || matchEmail || matchMobile1) {
      matchCount++;
      console.log(`\nMatch #${matchCount}:`);
      console.log('Doc ID:', doc.id);
      console.log('emp.kgid:', emp.kgid);
      console.log('emp.name:', emp.name);
      console.log('emp.email:', emp.email);
      console.log('emp.mobile1:', emp.mobile1);
      console.log('emp.mobile2:', emp.mobile2);
      console.log('emp.isApproved:', emp.isApproved);
      console.log('Matched on:', { matchKgid, matchEmail, matchMobile1 });
    }
  });

  if (matchCount === 0) {
    console.log('No matching approved employees found in database.');
  }

  process.exit(0);
}

run().catch(err => {
  console.error(err);
  process.exit(1);
});
