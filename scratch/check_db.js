const sqlite3 = require('sqlite3').verbose();
const db = new sqlite3.Database('employee_directory_db');

db.all("SELECT kgid, name, rank, unit, district, station FROM employee WHERE rank = 'AAO' OR name LIKE '%AAO%'", [], (err, rows) => {
    if (err) {
        console.error(err);
        return;
    }
    console.log("AAO records in sqlite:");
    console.log(JSON.stringify(rows, null, 2));
    db.close();
});
