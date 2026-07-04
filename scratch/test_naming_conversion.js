const fs = require('fs');
const path = require('path');

const readyPath = path.join(__dirname, '..', 'officers_merge_ready.json');
const ready = JSON.parse(fs.readFileSync(readyPath, 'utf8'));

const uppercaseWords = new Set([
    'DG', 'IGP', 'ADGP', 'DGP', 'DIGP', 'DIG', 'SP', 'DCP', 'DySP', 'DVP', 'IG', 
    'SPL', 'SEC', 'GOVT', 'INDIA', 'CM', 'MD', 'KSPH', 'IDCL', 'BMTC', 'RERA', 
    'KAT', 'SIT', 'CID', 'IPS', 'KLA', 'ANF', 'CCT', 'SDRF', 'HRM', 'L&O'
]);

function formatRetiredName(name) {
    name = name.replace(/\s+/g, ' ');
    const words = name.split(' ');
    const formattedWords = words.map(word => {
        let cleanWord = word.replace(/^[^\w\&\/]+|[^\w\&\/]+$/g, '');
        let cleanUpper = cleanWord.toUpperCase();

        if (uppercaseWords.has(cleanUpper)) {
            return word.toUpperCase();
        }

        let lowered = word.toLowerCase();
        let processed = lowered.replace(/(?:^|[^a-zA-Z0-9])([a-z])/g, function(match, char) {
            return match.toUpperCase();
        });
        
        processed = processed.replace(/\bRetd\b/g, 'Retd');
        processed = processed.replace(/\bDr\b/g, 'Dr');
        
        return processed;
    });

    return formattedWords.join(' ').replace(/\bHon'Ble\b/g, "Hon'ble");
}

function moveInitialsToEnd(name) {
    const lower = name.toLowerCase();
    let index = lower.indexOf(' retd');
    if (index === -1) {
        index = lower.indexOf(' retired');
    }
    if (index === -1) return name;
    
    let personalPart = name.substring(0, index).trim();
    let retdPart = name.substring(index).trim();
    
    const initialMatch = personalPart.match(/^(Dr\.\s+|Dr\s+)?([A-Z]\.(?:\s*[A-Z]\.?)*)\s*([A-Z][\s\S]+)$/);
    if (initialMatch) {
        const title = initialMatch[1] || "";
        const initials = initialMatch[2].trim();
        const rest = initialMatch[3].trim();
        
        let cleanInitials = initials.split(/\s*[\.\s]\s*/).filter(Boolean).join('.');
        
        personalPart = `${title}${rest} ${cleanInitials}`.replace(/\s+/g, ' ').trim();
    }
    
    return `${personalPart} ${retdPart}`.replace(/\s+/g, ' ').trim();
}

const retired = ready.filter(o => 
    /retired/i.test(o.unit) || 
    /retd/i.test(o.rank) || 
    /retd/i.test(o.name)
);

console.log(`Total retired officers found: ${retired.length}`);
console.log('Sample formatting (first 40):');
retired.slice(0, 40).forEach(o => {
    const formatted = formatRetiredName(o.name);
    const reordered = moveInitialsToEnd(formatted);
    console.log(`${o.name}  ==>  ${reordered}`);
});
