const fs = require('fs');
const path = require('path');

const dbPath = path.join(__dirname, '../officers_real_db.json');
const officers = JSON.parse(fs.readFileSync(dbPath, 'utf8'));

const FieldWeights = {
    EXACT_MATCH: 1.0,
    STARTS_WITH: 0.8,
    CONTAINS: 0.5,
    FUZZY_MATCH: 0.3,
    
    NAME_EXACT: 1.0,
    NAME_STARTS: 0.9,
    ID_EXACT: 0.95,
    ID_STARTS: 0.85,
    MOBILE_EXACT: 0.9,
    RANK_EXACT: 0.7,
    STATION_EXACT: 0.6,
    DISTRICT_EXACT: 0.5
};

function calculateNameScore(name, queryLower) {
    if (!name) return 0.0;
    const nameLower = name.toLowerCase();
    if (nameLower === queryLower) return FieldWeights.EXACT_MATCH;
    if (nameLower.startsWith(queryLower)) return FieldWeights.STARTS_WITH;
    if (nameLower.includes(queryLower)) return FieldWeights.CONTAINS;
    return 0.0;
}

function calculateIdScore(id, queryLower) {
    if (!id) return 0.0;
    const idLower = id.toLowerCase();
    if (idLower === queryLower) return FieldWeights.EXACT_MATCH;
    if (idLower.startsWith(queryLower)) return FieldWeights.STARTS_WITH;
    if (idLower.includes(queryLower)) return FieldWeights.CONTAINS;
    return 0.0;
}

function calculateFieldScore(field, queryLower) {
    if (!field) return 0.0;
    const fieldLower = field.toLowerCase();
    if (fieldLower === queryLower) return FieldWeights.EXACT_MATCH;
    if (fieldLower.startsWith(queryLower)) return FieldWeights.STARTS_WITH;
    if (fieldLower.includes(queryLower)) return FieldWeights.CONTAINS;
    return 0.0;
}

function calculateMobileScore(mobile1, mobile2, queryLower) {
    const mobile1Score = mobile1 ? calculateFieldScore(mobile1, queryLower) : 0.0;
    const mobile2Score = mobile2 ? calculateFieldScore(mobile2, queryLower) : 0.0;
    return Math.max(mobile1Score, mobile2Score);
}

function calculateOfficerScore(officer, queryLower) {
    const nameScore = calculateNameScore(officer.name, queryLower) * FieldWeights.NAME_EXACT;
    const idScore = calculateIdScore(officer.agid, queryLower) * FieldWeights.ID_EXACT;
    const mobileScore = calculateMobileScore(officer.mobile || officer.landline, officer.landline2, queryLower) * FieldWeights.MOBILE_EXACT;
    const rankScore = calculateFieldScore(officer.rank, queryLower) * FieldWeights.RANK_EXACT;
    const stationScore = calculateFieldScore(officer.station || officer.office, queryLower) * FieldWeights.STATION_EXACT;
    const districtScore = calculateFieldScore(officer.district, queryLower) * FieldWeights.DISTRICT_EXACT;
    
    return Math.max(nameScore, idScore, mobileScore, rankScore, stationScore, districtScore);
}

const query = 'cmdt';
const queryLower = query.toLowerCase();

// Filter down to officers matching searchBlob LIKE %query%
const matched = officers.filter(o => o.searchBlob && o.searchBlob.toLowerCase().includes(queryLower));

console.log(`Found ${matched.length} matches in searchBlob.`);

const scored = matched.map(o => {
    return {
        agid: o.agid,
        name: o.name,
        rank: o.rank,
        email: o.email,
        score: calculateOfficerScore(o, queryLower)
    };
});

// Sort by score descending
scored.sort((a, b) => b.score - a.score);

console.log("\nTop 15 sorted by score:");
console.table(scored.slice(0, 15));
