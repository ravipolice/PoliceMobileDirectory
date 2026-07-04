const xlsx = require('xlsx');
const fs = require('fs');
const path = require('path');
const ExcelJS = require('exceljs');
const admin = require('firebase-admin');

const v3Path = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const serviceAccount = require('../service-account.json');

if (!admin.apps.length) {
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
    });
}
const db = admin.firestore();

async function fixAndRedeploy() {
    console.log("Starting Ultimate Cleanup and Unique ID Assignment...");

    // 1. Load V3 Excel
    const wb = xlsx.readFile(v3Path);
    const rows = xlsx.utils.sheet_to_json(wb.Sheets['MASTER_MERGED_FINAL']);
    
    // 2. Assign 100% Unique AGIDs (KSP_0001 format to be safe and clean)
    const processedRows = rows.map((row, index) => {
        const uniqueId = `KSP${String(index + 1).padStart(4, '0')}`;
        
        // Ensure name is clean (No brackets, descriptive)
        let name = String(row.Name || "").trim();
        // Remove brackets just in case
        name = name.replace(/[\[\]\(\)]/g, '').replace(/\s+/g, ' ').trim();
        
        row.agid = uniqueId;
        row.Name = name;

        const blobParts = [row.Name, row.Rank, row.station, row.UNIT, row.District, row.Section, row.office1, row['office 2'], row['mobile 1'], row['mobile 2'], row.email1, row.email2]
            .filter(v => v && String(v).trim() !== '').map(v => String(v).toLowerCase());

        return {
            docId: uniqueId,
            agid: uniqueId,
            name: row.Name,
            rank: row.Rank || '',
            office: row.station || '',
            unit: row.UNIT || '',
            district: row.District || '',
            subDivision: row.Section || '',
            landline: row.office1 || '',
            landline2: row['office 2'] || '',
            mobile: row['mobile 1'] || '',
            mobile2: row['mobile 2'] || '',
            email: row.email1 || '',
            email2: row.email2 || '',
            searchBlob: [...new Set(blobParts)].join(' '),
            updatedAt: new Date().toISOString()
        };
    });

    console.log(`Generated ${processedRows.length} unique records.`);

    // 3. Save Updated Excel
    const workbook = new ExcelJS.Workbook();
    const headers = ["agid", "UNIT", "Range", "District", "Section", "Name", "Rank", "station", "office1", "office 2", "mobile 1", "mobile 2", "email1", "email2"];
    const ws = workbook.addWorksheet('MASTER_MERGED_FINAL');
    ws.columns = headers.map(h => ({ header: h, key: h, width: 20 }));
    rows.forEach(r => ws.addRow(r));
    ws.getRow(1).font = { bold: true };
    await workbook.xlsx.writeFile(v3Path);
    console.log("Excel file updated with unique IDs.");

    // 4. Wipe 'officers' collection
    console.log("Wiping 'officers' collection...");
    const snapshot = await db.collection('officers').get();
    const deleteBatch = db.batch();
    snapshot.docs.forEach(doc => deleteBatch.delete(doc.ref));
    // Note: If > 500 docs, we'd need multiple batches, but we'll use a helper for robustness
    await wipeCollection('officers');
    console.log("Collection wiped.");

    // 5. Upload New DB
    console.log(`Uploading ${processedRows.length} records...`);
    const collectionRef = db.collection('officers');
    let count = 0;
    let batch = db.batch();
    
    for (const record of processedRows) {
        const docRef = collectionRef.doc(record.docId);
        const { docId, ...dataToSave } = record;
        batch.set(docRef, dataToSave);
        count++;
        
        if (count % 500 === 0) {
            await batch.commit();
            console.log(`Uploaded ${count}...`);
            batch = db.batch();
        }
    }
    if (count % 500 !== 0) await batch.commit();

    console.log("--- FINAL DEPLOYMENT SUCCESSFUL ---");
    console.log(`Total Unique Records: ${count}`);
    process.exit(0);
}

async function wipeCollection(collectionPath) {
    const collectionRef = db.collection(collectionPath);
    const query = collectionRef.orderBy('__name__').limit(500);
    return new Promise((resolve, reject) => {
        deleteQueryBatch(db, query, resolve).catch(reject);
    });
}

async function deleteQueryBatch(db, query, resolve) {
    const snapshot = await query.get();
    if (snapshot.size === 0) { resolve(); return; }
    const batch = db.batch();
    snapshot.docs.forEach(doc => batch.delete(doc.ref));
    await batch.commit();
    process.nextTick(() => deleteQueryBatch(db, query, resolve));
}

fixAndRedeploy().catch(console.error);
