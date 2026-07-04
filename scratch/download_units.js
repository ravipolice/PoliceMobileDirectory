const admin = require('firebase-admin');
const fs = require('fs');

const serviceAccount = require('./service-account.json');

if (!admin.apps.length) {
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
    });
}

const db = admin.firestore();

async function downloadUnits() {
    console.log('Fetching units collection...');
    const snapshot = await db.collection('units').get();
    const data = [];
    
    snapshot.forEach(doc => {
        data.push({ id: doc.id, ...doc.data() });
    });

    const outputPath = 'units_config.json';
    fs.writeFileSync(outputPath, JSON.stringify(data, null, 2));
    console.log(`Successfully downloaded ${data.length} records to ${outputPath}`);
    process.exit(0);
}

downloadUnits().catch(err => {
    console.error('Error downloading collection:', err);
    process.exit(1);
});
