const admin = require('firebase-admin');
const xlsx = require('xlsx');
const path = require('path');

const serviceAccount = require('../service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function uploadOfficers() {
    console.log("Reading KSP_Contacts_Final_Directory_V3.xlsx...");
    const workbook = xlsx.readFile('KSP_Contacts_Final_Directory_V3.xlsx');
    const sheet = workbook.Sheets['MASTER_MERGED_FINAL'];
    const records = xlsx.utils.sheet_to_json(sheet);
    console.log(`Read ${records.length} records from Excel.`);

    const officersCol = db.collection('officers');

    // 1. Delete existing officers (Batch deletion)
    console.log("Clearing existing officers collection...");
    const snap = await officersCol.select().get();
    
    const deleteBatchSize = 400;
    for (let i = 0; i < snap.size; i += deleteBatchSize) {
        const batch = db.batch();
        const chunk = snap.docs.slice(i, i + deleteBatchSize);
        chunk.forEach(doc => batch.delete(doc.ref));
        await batch.commit();
        console.log(`Deleted batch ${i/deleteBatchSize + 1}...`);
    }
    console.log(`Successfully cleared ${snap.size} records.`);

    // 2. Upload fresh standardized records
    console.log("Uploading fresh standardized records...");
    const uploadBatchSize = 400;
    for (let i = 0; i < records.length; i += uploadBatchSize) {
        const batch = db.batch();
        const chunk = records.slice(i, i + uploadBatchSize);
        
        chunk.forEach(record => {
            const docRef = officersCol.doc(); // Auto-ID
            batch.set(docRef, {
                ...record,
                updatedAt: admin.firestore.FieldValue.serverTimestamp()
            });
        });
        
        await batch.commit();
        console.log(`Uploaded batch ${i/uploadBatchSize + 1} (${i + chunk.length}/${records.length})...`);
    }

    console.log("Officer Synchronization complete!");
    process.exit(0);
}

uploadOfficers().catch(err => {
    console.error("Upload Error:", err);
    process.exit(1);
});
