/**
 * SITA Firestore Migration Script
 * ================================
 * Updates all existing employee/station records in Firestore where
 * station = "SITA" → "SITA (State Intelligence Training Academy)"
 *
 * Run this ONCE from a Node.js environment with Firebase Admin SDK.
 *
 * How to run:
 *   1. Install deps:  npm install firebase-admin
 *   2. Download serviceAccountKey.json from:
 *      Firebase Console → Project Settings → Service Accounts → Generate new private key
 *   3. Run: node scripts/migrate-sita.js
 */

const admin = require("firebase-admin");
const serviceAccount = require("../serviceAccountKey.json"); // place key file at project root

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});

const db = admin.firestore();

const OLD_NAME = "SITA";
const NEW_NAME = "SITA (State Intelligence Training Academy)";

async function migrateCollection(collectionName, fieldName) {
  console.log(`\n🔍 Scanning '${collectionName}' collection for ${fieldName} = "${OLD_NAME}"...`);

  const snapshot = await db
    .collection(collectionName)
    .where(fieldName, "==", OLD_NAME)
    .get();

  if (snapshot.empty) {
    console.log(`  ✅ No records found with ${fieldName} = "${OLD_NAME}"`);
    return 0;
  }

  console.log(`  📝 Found ${snapshot.size} record(s) to update...`);

  const batch = db.batch();
  snapshot.docs.forEach((doc) => {
    console.log(`  → Updating doc ${doc.id}: ${fieldName} "${OLD_NAME}" → "${NEW_NAME}"`);
    batch.update(doc.ref, { [fieldName]: NEW_NAME });
  });

  await batch.commit();
  console.log(`  ✅ Updated ${snapshot.size} record(s) in '${collectionName}'`);
  return snapshot.size;
}

async function migrateStationsCollection() {
  console.log(`\n🔍 Scanning 'stations' collection for name = "${OLD_NAME}"...`);

  const snapshot = await db
    .collection("stations")
    .where("name", "==", OLD_NAME)
    .get();

  if (snapshot.empty) {
    console.log(`  ✅ No station records found with name = "${OLD_NAME}"`);
    return 0;
  }

  console.log(`  📝 Found ${snapshot.size} station(s) to update...`);

  const batch = db.batch();
  snapshot.docs.forEach((doc) => {
    console.log(`  → Updating station doc ${doc.id}`);
    batch.update(doc.ref, { name: NEW_NAME });
  });

  await batch.commit();
  console.log(`  ✅ Updated ${snapshot.size} station(s)`);
  return snapshot.size;
}

async function main() {
  console.log("===========================================");
  console.log("  PMD SITA → Full Name Migration Script  ");
  console.log("===========================================");

  let totalUpdated = 0;

  // Update employees where station = "SITA"
  totalUpdated += await migrateCollection("employees", "station");

  // Update stations collection where name = "SITA"
  totalUpdated += await migrateStationsCollection();

  // Also check officers (in case any have office = "SITA")
  totalUpdated += await migrateCollection("officers", "office");

  console.log(`\n🎉 Migration complete. Total records updated: ${totalUpdated}`);
  console.log("   The web app getStations() mapping is still in place as a safety net.");
  process.exit(0);
}

main().catch((err) => {
  console.error("❌ Migration failed:", err);
  process.exit(1);
});
