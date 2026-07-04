const admin = require('firebase-admin');

const serviceAccount = require('../service-account.json');

if (!admin.apps.length) {
    admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
}

const db = admin.firestore();

async function removeDuplicates() {
    console.log('📡 Fetching employees collection...');
    const empSnap = await db.collection('employees').get();

    // Build lookup sets from employees collection
    const empKgids = new Set();
    const empEmails = new Set();

    empSnap.docs.forEach(doc => {
        const data = doc.data();
        const kgid = (data.kgid || doc.id || '').trim().toLowerCase();
        const email = (data.email || '').trim().toLowerCase();
        if (kgid) empKgids.add(kgid);
        if (email) empEmails.add(email);
    });

    console.log(`✅ Loaded ${empKgids.size} KGIDs and ${empEmails.size} emails from employees.`);

    console.log('\n📡 Fetching admin_employees collection...');
    const adminSnap = await db.collection('admin_employees').get();
    console.log(`✅ Loaded ${adminSnap.size} records from admin_employees.\n`);

    const toDelete = [];
    const toKeep = [];

    adminSnap.docs.forEach(doc => {
        const data = doc.data();
        const kgid = (data.kgid || doc.id || '').trim().toLowerCase();
        const email = (data.email || '').trim().toLowerCase();

        const kgidMatch = kgid && empKgids.has(kgid);
        const emailMatch = email && empEmails.has(email);

        if (kgidMatch || emailMatch) {
            toDelete.push({
                id: doc.id,
                name: data.name || '(no name)',
                kgid: data.kgid || doc.id,
                email: data.email || '',
                reason: kgidMatch ? `KGID match: ${kgid}` : `Email match: ${email}`
            });
        } else {
            toKeep.push(doc.id);
        }
    });

    console.log(`🔍 Found ${toDelete.length} duplicate(s) to remove from admin_employees:`);
    console.log(`✅ ${toKeep.length} unique records will remain.\n`);

    if (toDelete.length === 0) {
        console.log('🎉 No duplicates found! Both databases are clean.');
        process.exit(0);
    }

    toDelete.forEach((d, i) => {
        console.log(`  ${i + 1}. [${d.kgid}] ${d.name} (${d.email}) → ${d.reason}`);
    });

    // Delete in batches
    console.log('\n🗑️  Deleting duplicates from admin_employees...');
    let batch = db.batch();
    let count = 0;

    for (const dup of toDelete) {
        batch.delete(db.collection('admin_employees').doc(dup.id));
        count++;
        if (count % 500 === 0) {
            await batch.commit();
            batch = db.batch();
            console.log(`  Committed ${count}...`);
        }
    }
    if (count % 500 !== 0) {
        await batch.commit();
    }

    console.log(`\n✅ Done! Removed ${toDelete.length} duplicate(s) from admin_employees.`);
    console.log(`📊 admin_employees now has ${toKeep.length} unique HR records.`);
    process.exit(0);
}

removeDuplicates().catch(err => {
    console.error('❌ Script failed:', err);
    process.exit(1);
});
