(async function() {
    console.log("Starting KSP Contact Scrape...");

    // 1. Expand all Section Headers
    const grpHeaders = document.querySelectorAll('.grp-header');
    for (const header of grpHeaders) {
        if (!header.classList.contains('active')) {
            header.click();
            await new Promise(r => setTimeout(r, 300));
        }
    }
    console.log("Sections expanded.");

    // 2. Expand all Unit Headers
    const subHeaders = document.querySelectorAll('.sub-header');
    for (const sub of subHeaders) {
        if (!sub.classList.contains('active')) {
            sub.click();
            await new Promise(r => setTimeout(r, 200));
        }
    }
    console.log("Units expanded.");

    const results = [];
    
    // 3. Iterate through Sections
    const sections = document.querySelectorAll('.grp-container');
    sections.forEach(section => {
        const sectionName = section.previousElementSibling.textContent.trim();
        
        // Find units within this section
        const units = section.querySelectorAll('.sub-container');
        units.forEach(unit => {
            const unitName = unit.previousElementSibling.textContent.trim();
            
            // Find table within this unit
            const table = unit.querySelector('table');
            if (table) {
                const rows = table.querySelectorAll('tr');
                rows.forEach((row, index) => {
                    if (index === 0) return; // Skip header
                    
                    const cells = row.querySelectorAll('td');
                    if (cells.length >= 4) {
                        const slNo = cells[0].textContent.trim();
                        const designation = cells[1].textContent.trim();
                        const phone = cells[2].textContent.trim();
                        const email = cells[3].textContent.trim();
                        
                        results.push({
                            Section: sectionName,
                            Unit: unitName,
                            Designation: designation,
                            Phone: phone,
                            Email: email
                        });
                    }
                });
            }
        });
    });

    console.log(`Scraped ${results.length} records.`);
    
    // Convert to CSV
    const headers = ["Section", "Unit", "Designation", "Phone", "Email"];
    const csvContent = [
        headers.join(","),
        ...results.map(row => headers.map(h => `"${(row[h] || "").replace(/"/g, '""')}"`).join(","))
    ].join("\n");

    // Download CSV
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.setAttribute("download", "KSP_Contacts_Scraped_Raw.csv");
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    
    return results;
})();
