const fs = require('fs');
const content = fs.readFileSync('../KSP_Contacts_Master_Clean.csv', 'utf8');
const lines = content.split('\n');

let multiMobile = 0;
let multiEmail = 0;

lines.forEach(l => {
    // Basic CSV splitting (handling simple cases)
    const parts = l.split('","');
    if (parts.length >= 5) {
        const phone = parts[3];
        const email = parts[4];

        const mobiles = phone.match(/[789]\d{9}/g);
        if (mobiles && mobiles.length > 1) multiMobile++;

        const emails = email.match(/[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}/g);
        if (emails && emails.length > 1) multiEmail++;
    }
});

console.log('Total rows in CSV:', lines.length);
console.log('Records with >1 mobile:', multiMobile);
console.log('Records with >1 email:', multiEmail);
