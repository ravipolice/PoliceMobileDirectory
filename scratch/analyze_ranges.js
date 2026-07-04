const fs = require('fs');
const lines = fs.readFileSync('KSP_Contacts_Master_Clean.csv', 'utf8').split('\n');
const ranges = {};

lines.forEach(l => {
    // Basic CSV parser
    const matches = [];
    let insideQuote = false;
    let entry = '';
    for (let j = 0; j < l.length; j++) {
        const char = l[j];
        if (char === '"') insideQuote = !insideQuote;
        else if (char === ',' && !insideQuote) {
            matches.push(entry.trim());
            entry = '';
        } else entry += char;
    }
    matches.push(entry.trim());

    if (matches[0] === 'Ranges') {
        const range = matches[1];
        if (!ranges[range]) ranges[range] = new Set();
        // Look for SP, <District> or Addl. SP, <District>
        const desig = matches[2] || '';
        if (desig.startsWith('SP,') || desig.startsWith('Addl. SP,')) {
            const dist = desig.split(',')[1].trim();
            ranges[range].add(dist);
        }
    }
});

Object.keys(ranges).forEach(r => {
    console.log(r + ': ' + Array.from(ranges[r]).join(', '));
});
