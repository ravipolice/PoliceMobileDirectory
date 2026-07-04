const admin = require('firebase-admin');
const serviceAccount = require('../service-account.json');

if (!admin.apps.length) {
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
    });
}

const db = admin.firestore();

async function checkProfile() {
    const email = 'ravipolice@gmail.com';
    const snapshot = await db.collection('employees').where('email', '==', email).get();
    
    if (snapshot.empty) {
        console.log('❌ No employee found.');
        return;
    }
    
    const doc = snapshot.docs[0];
    console.log('Document ID (KGID):', doc.id);
    console.log('Document Data:', JSON.stringify(doc.data(), null, 2));
}

checkProfile();
