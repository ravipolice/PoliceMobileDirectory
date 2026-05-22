const admin = require('firebase-admin');
const serviceAccount = require('./service-account.json');

if (!admin.apps.length) {
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
    });
}

const db = admin.firestore();

async function fixProfile() {
    const email = 'ravipolice@gmail.com';
    const snapshot = await db.collection('employees').where('email', '==', email).get();
    
    if (snapshot.empty) {
        console.log('❌ No employee found.');
        return;
    }
    
    const doc = snapshot.docs[0];
    await doc.ref.update({
        name: 'Ravikumar J',
        kgid: '1953036',
        isApproved: true,
        isAdmin: true
    });
    
    console.log('✅ Profile successfully updated to Approved status for Ravikumar J (1953036).');
}

fixProfile();
