const admin = require('firebase-admin');
const fs = require('fs');
const { parse } = require('csv-parse/sync');

// 1. Initialize Firebase
const serviceAccount = require('../service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: "https://pmd-police-mobile-directory-default-rtdb.asia-southeast1.firebasedatabase.app"
});

const db = admin.database();
const ref = db.ref('officers_v2');

// 2. Read and Parse CSV
const csvPath = '../KSP_Officers_App.csv';
const fileContent = fs.readFileSync(csvPath, 'utf8');

const records = parse(fileContent, {
  columns: true,
  skip_empty_lines: true
});

console.log(`Starting upload of ${records.length} records to 'officers_v2'...`);

// 3. Batch Upload
async function uploadData() {
    try {
        const batchSize = 500;
        for (let i = 0; i < records.length; i += batchSize) {
            const batch = records.slice(i, i + batchSize);
            const updates = {};
            
            batch.forEach(record => {
                // Key by AGID for easy lookup
                updates[record.agid] = record;
            });
            
            await ref.update(updates);
            console.log(`Uploaded batch ${Math.floor(i/batchSize) + 1} (${i + batch.length}/${records.length})`);
        }
        
        console.log('--- UPLOAD COMPLETE ---');
        console.log(`Successfully uploaded ${records.length} officers to Firebase node 'officers_v2'.`);
        process.exit(0);
    } catch (error) {
        console.error('Upload failed:', error);
        process.exit(1);
    }
}

uploadData();
