const fs = require('fs');
const path = require('path');

const jsonPath = path.join(__dirname, '..', 'officers_real_db.json');

function normalizeRank(rankStr) {
    if (!rankStr) return '';
    let r = rankStr.trim();
    
    // Check if it starts with RETD.
    const isRetd = r.toUpperCase().startsWith('RETD.');
    let cleanRank = r;
    if (isRetd) {
        cleanRank = r.substring(5).trim();
    }
    
    const d = cleanRank.toLowerCase();
    
    let normalized = cleanRank;
    
    if (d === 'digp' || d === 'deputy inspector general' || d === 'deputy inspector general of police') normalized = 'DIG';
    else if (d === 'commandant') normalized = 'CMDT';
    else if (d === 'deputy commandant') normalized = 'DEPT.CMDT';
    else if (d === 'assistant commandant') normalized = 'ASST.CMDT';
    else if (d === 'addl. sp' || d === 'addl.sp' || d === 'additional superintendent' || d === 'additional superintendent of police') normalized = 'Addl.SP';
    else if (d === 'dysp' || d === 'deputy superintendent' || d === 'deputy superintendent of police' || d === 'dsp') normalized = 'DySP';
    else if (d === 'asp' || d === 'assistant superintendent' || d === 'assistant superintendent of police') normalized = 'ASP';
    else if (d === 'acp' || d === 'assistant commissioner' || d === 'assistant commissioner of police') normalized = 'ACP';
    else if (d === 'dcp' || d === 'deputy commissioner' || d === 'deputy commissioner of police') normalized = 'DCP';
    else if (d === 'dg & igp' || d === 'director general & inspector general of police') normalized = 'DG & IGP';
    else if (d === 'adgp' || d === 'additional director general' || d === 'additional director general of police') normalized = 'ADGP';
    else if (d === 'igp' || d === 'inspector general' || d === 'inspector general of police') normalized = 'IGP';
    else if (d === 'dgp' || d === 'director general of police') normalized = 'DGP';
    else if (d === 'pi' || d === 'police inspector' || d === 'inspector') normalized = 'PI';
    else if (d === 'psi' || d === 'police sub-inspector' || d === 'sub-inspector') normalized = 'PSI';
    else if (d === 'asi' || d === 'assistant sub-inspector') normalized = 'ASI';
    else if (d === 'hc' || d === 'head constable') normalized = 'HC';
    else if (d === 'pc' || d === 'police constable') normalized = 'PC';
    
    return isRetd ? `RETD. ${normalized}` : normalized;
}

const dbContacts = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));

let diffCount = 0;
const rankChanges = {};

dbContacts.forEach(r => {
    if (r.rank) {
        const normalized = normalizeRank(r.rank);
        if (normalized !== r.rank) {
            diffCount++;
            const key = `${r.rank} -> ${normalized}`;
            rankChanges[key] = (rankChanges[key] || 0) + 1;
        }
    }
});

console.log('════════════════════════════════════════════════');
console.log('📊 RANK NORMALIZATION TEST');
console.log('════════════════════════════════════════════════');
console.log(`Total ranks changing: ${diffCount}\n`);
console.log('Breakdown:');
Object.entries(rankChanges).forEach(([change, count]) => {
    console.log(`  - ${change}: ${count} records`);
});
console.log('════════════════════════════════════════════════');
