const admin = require('firebase-admin');
const serviceAccount = require('../service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

const TARGET_EMAIL = 'ravipolice@gmail.com';
const NEW_NAME    = 'Ravikumar J';
const NEW_KGID    = '1953036';

async function fixProfile() {
  // Search by email field in employees collection
  const snapshot = await db.collection('employees')
    .where('email', '==', TARGET_EMAIL)
    .get();

  if (snapshot.empty) {
    console.log('❌ No employee found with email:', TARGET_EMAIL);
    process.exit(1);
  }

  for (const doc of snapshot.docs) {
    const data = doc.data();
    console.log(`\n📄 Found document: ${doc.id}`);
    console.log(`   Current name : ${data.name}`);
    console.log(`   Current kgid : ${data.kgid}`);

    await doc.ref.update({
      name: NEW_NAME,
      kgid: NEW_KGID
    });

    console.log(`✅ Updated → name: "${NEW_NAME}", kgid: "${NEW_KGID}"`);
  }

  console.log('\n🎉 Profile fix complete!');
  process.exit(0);
}

fixProfile().catch(err => {
  console.error('❌ Error:', err);
  process.exit(1);
});
