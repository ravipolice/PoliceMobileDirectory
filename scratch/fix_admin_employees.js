const admin = require('firebase-admin');
const fs = require('fs');
const { parse } = require('csv-parse/sync');

const serviceAccount = require('../service-account.json');

if (!admin.apps.length) {
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount)
    });
}

const db = admin.firestore();
const collectionRef = db.collection('admin_employees');

async function fix() {
    // 1. Wipe the collection first
    console.log("Wiping existing 'admin_employees' collection to remove empty rows...");
    const snapshot = await collectionRef.get();
    let batch = db.batch();
    let deleteCount = 0;
    
    for (const doc of snapshot.docs) {
        batch.delete(doc.ref);
        deleteCount++;
        if (deleteCount % 500 === 0) {
            await batch.commit();
            batch = db.batch();
        }
    }
    if (deleteCount % 500 !== 0) {
        await batch.commit();
    }
    console.log(`Deleted ${deleteCount} old/empty documents.`);

    // 2. Parse and upload only VALID records
    const csvPath = 'admin_employees.tsv';
    const fileContent = fs.readFileSync(csvPath, 'utf8');

    const records = parse(fileContent, {
      columns: true,
      skip_empty_lines: true,
      delimiter: '\t'
    });

    let count = 0;
    batch = db.batch();
    
    for (const record of records) {
        // Skip rows that are just empty tabs (no kgid and no email and no name)
        if (!record.kgid && !record.KGID && !record.email && !record.name) {
            continue;
        }

        // Remove BOM from kgid if it exists
        const actualKgid = record['\uFEFFkgid'] || record['kgid'] || record['KGID'];
        const docId = actualKgid || record.email || record.Email || collectionRef.doc().id;
        
        const cleanRecord = {};
        
        // Allowed fields in the Android App Data Model
        const allowedFields = [
            'name', 'email', 'pin', 'mobile1', 'mobile2', 'rank', 'metalNumber',
            'district', 'station', 'bloodGroup', 'photoUrl', 'fcmToken',
            'firebaseUid', 'photoUrlFromGoogle', 'unit', 'landline', 'landline2',
            'gender', 'subSection', 'dutyRole'
        ];
        
        // Only map allowed fields if they have a non-empty value
        for (const field of allowedFields) {
            if (record[field] && typeof record[field] === 'string' && record[field].trim() !== '') {
                cleanRecord[field] = record[field].trim();
            }
        }
        
        if (actualKgid && actualKgid.trim() !== '') cleanRecord.kgid = actualKgid.trim();
        
        // Handle boolean fields
        cleanRecord.isAdmin = true;
        cleanRecord.isApproved = true;
        cleanRecord.isManualStation = record.isManualStation === 'TRUE' || record.isManualStation === 'true';
        cleanRecord.isManualSubSection = record.isManualSubSection === 'TRUE' || record.isManualSubSection === 'true';

        cleanRecord.updatedAt = admin.firestore.FieldValue.serverTimestamp();
        
        // If the object is completely empty except for booleans/updatedAt, skip it
        if (!cleanRecord.kgid && !cleanRecord.name && !cleanRecord.email) continue;
        
        const docRef = collectionRef.doc(docId);
        batch.set(docRef, cleanRecord, { merge: true });
        
        count++;
        if (count % 500 === 0) {
            await batch.commit();
            batch = db.batch();
        }
    }
    
    if (count % 500 !== 0) {
        await batch.commit();
    }
    
    console.log(`--- UPLOAD COMPLETE ---`);
    console.log(`Successfully uploaded ${count} VALID employees to Firestore collection 'admin_employees'.`);
    process.exit(0);
}

fix().catch(console.error);
