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
  console.log('=== Firestore Status Check ===');
  
  // 1. Fetch all employees
  const empSnap = await db.collection('employees').get();
  console.log(`Total employees in 'employees' collection: ${empSnap.size}`);
  
  let approvedEmps = 0;
  let unapprovedEmps = 0;
  
  empSnap.forEach(doc => {
    const data = doc.data();
    if (data.isApproved === false) {
      unapprovedEmps++;
    } else {
      approvedEmps++;
    }
  });
  
  console.log(`- Approved Employees (isApproved !== false): ${approvedEmps}`);
  console.log(`- Unapproved/Disabled Employees (isApproved === false): ${unapprovedEmps}`);
  
  // 2. Fetch all pending registrations
  const pendingSnap = await db.collection('pending_registrations').get();
  console.log(`\nTotal registrations in 'pending_registrations' collection: ${pendingSnap.size}`);
  
  let pendingCount = 0;
  let approvedRegCount = 0;
  let rejectedRegCount = 0;
  let otherRegCount = 0;
  
  const pendingList = [];
  
  pendingSnap.forEach(doc => {
    const data = doc.data();
    const status = (data.status || 'pending').toLowerCase();
    if (status === 'pending') {
      pendingCount++;
      pendingList.push({ id: doc.id, ...data });
    } else if (status === 'approved') {
      approvedRegCount++;
    } else if (status === 'rejected') {
      rejectedRegCount++;
    } else {
      otherRegCount++;
    }
  });
  
  console.log(`- Status 'pending': ${pendingCount}`);
  console.log(`- Status 'approved': ${approvedRegCount}`);
  console.log(`- Status 'rejected': ${rejectedRegCount}`);
  console.log(`- Status 'other': ${otherRegCount}`);
  
  // 3. Check for duplicates / matches between pending registrations and approved employees
  console.log('\n=== Checking for Pending Registrations that are Already Approved ===');
  
  let duplicateCount = 0;
  
  for (const pending of pendingList) {
    let matchedEmp = null;
    let matchReason = '';
    
    empSnap.forEach(doc => {
      const emp = doc.data();
      const isApproved = emp.isApproved !== false;
      if (!isApproved) return;
      
      const pendingKgidStr = String(pending.kgid || '').trim().toLowerCase();
      const empKgidStr = String(emp.kgid || '').trim().toLowerCase();
      const matchKgid = pendingKgidStr && empKgidStr && pendingKgidStr === empKgidStr;
      
      const pendingEmailStr = String(pending.email || '').trim().toLowerCase();
      const empEmailStr = String(emp.email || '').trim().toLowerCase();
      const matchEmail = pendingEmailStr && empEmailStr && pendingEmailStr === empEmailStr;
      
      const pendingMobileStr = String(pending.mobile1 || '').trim();
      const empMobile1Str = String(emp.mobile1 || '').trim();
      const empMobile2Str = String(emp.mobile2 || '').trim();
      const matchMobile = pendingMobileStr && (pendingMobileStr === empMobile1Str || pendingMobileStr === empMobile2Str);
      
      if (matchKgid || matchEmail || matchMobile) {
        matchedEmp = emp;
        matchReason = JSON.stringify({ matchKgid, matchEmail, matchMobile });
      }
    });
    
    if (matchedEmp) {
      duplicateCount++;
      console.log(`[Duplicate #${duplicateCount}] Pending Name: "${pending.name}" (KGID: ${pending.kgid}, Email: ${pending.email})`);
      console.log(`    Matched Approved Employee: "${matchedEmp.name}" (KGID: ${matchedEmp.kgid}, Email: ${matchedEmp.email})`);
      console.log(`    Matched on: ${matchReason}`);
    }
  }
  
  console.log(`\nTotal pending registrations that match an already approved employee: ${duplicateCount}`);
  
  process.exit(0);
}

run().catch(err => {
  console.error(err);
  process.exit(1);
});
