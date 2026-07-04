const admin = require('firebase-admin');
const path = require('path');
const serviceAccount = require('../service-account.json');

if (!admin.apps.length) {
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
    });
}

const db = admin.firestore();

async function checkProfile() {
    const email = 'ravipolice@gmail.com';
    console.log(`Checking profile for: ${email}`);
    
    const snapshot = await db.collection('employees').where('email', '==', email).get();
    
    if (snapshot.empty) {
        console.log('❌ No employee found with this email.');
        return;
    }
    
    snapshot.forEach(doc => {
        console.log('✅ Found Profile:');
        console.log('Document ID (KGID):', doc.id);
        const data = doc.data();
        console.log('Name:', data.name);
        console.log('KGID Field:', data.kgid);
        console.log('Email:', data.email);
        console.log('IsAdmin:', data.isAdmin);
        console.log('IsApproved:', data.isApproved);
    });
}

checkProfile();
