const rankMapping = {
    'AO': 'AO', 'ADMINISTRATIVE OFFICER': 'AO', 'AAO': 'AAO', 'ASSISTANT ADMINISTRATIVE OFFICER': 'AAO'
};

const r = "AAO, RECRUITMENT";

const sortedKeys = Object.keys(rankMapping).sort((a, b) => b.length - a.length);

for (const key of sortedKeys) {
    const escapedKey = key.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&');
    let pattern = "";
    if (/^[A-Za-z0-9]/.test(key)) pattern += "\\b";
    pattern += escapedKey;
    if (/[A-Za-z0-9]$/.test(key)) pattern += "\\b";
    
    const regex = new RegExp(pattern, 'i');
    console.log(`Key: "${key}" | Pattern: "${pattern}" | Test: ${regex.test(r)}`);
}
