# KSP Contact Directory: Organization & Sorting Rules

This document outlines the definitive rules for organizing, sorting, and formatting the Karnataka State Police (KSP) Mobile Directory. Follow these rules whenever rebuilding or updating the Excel/CSV master files.

## 1. Data Hierarchy (Top Sort)
The Master Sheet and individual sheets MUST be sorted by rank seniority:
1. **DG & IGP** (Director General of Police)
2. **ADGP** (Additional Director General of Police)
3. **IGP** (Inspector General of Police)
4. **DIGP** (Deputy Inspector General of Police)
5. **SP / DCP** (Superintendent of Police / Deputy Commissioner)
6. **Addl. SP**
7. **DySP / ACP**
8. **PI / CPI**
9. **PSI**
10. **Ministerial Staff** (AO, AAO, etc.)

## 2. Column Mapping & Alignment
- **UNIT (Major Head)**: Defines the primary functional assignment. Standard civil police (SPs, CPs, Range IGPs) must be classified as **L&O** (Law & Order). Headquarters staff are **Admin**. "Others" should only be used as an absolute last resort for truly unclassifiable units.
- **Section (Minor Head)**: Defines the specific sub-office, subdivision, or granular unit (e.g., State INT, 1st Battalion, Western Range Mangaluru). This must be preserved and never overwritten by the Major Head.
- **Range**: Must strictly contain only the official name of the Range (e.g., "Western Range") or the Commissionerate (e.g., "Bengaluru City"). Extraneous labels like "State Level" or appended districts like "- Mangaluru" must be left blank or stripped out.
- **Mobile 1/2**: Must be 10-digit numbers. Usually start with 7, 8, or 9.
- **Office 1/2**: Landline numbers. Must include the STD code (e.g., 080-, 0824-).
- **Rank**: Must contain *only* the official rank designation (e.g., "PI", "DySP"). All trailing unit data, locations, or comma-separated details (e.g., "PI, Finger Print Bureau") must be strictly stripped out.
- **Email 1/2**: Must contain *only* the exact email address. It must be strictly extracted (e.g., using regex `/[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}/`) to prevent massive concatenated search blobs from bleeding into the cell.
- **Cleaning**: Remove all encoding artifacts like `â`, `\x80`, and `undefined`.

## 3. Mandatory Column Order
Every sheet must follow this exact column structure from left to right:
1. `agid` (Unique Identifier)
2. `UNIT`
3. `Range`
4. `District`
5. `Section`
6. `Name`
7. `Rank`
8. `station`
9. `office1`
10. `office 2`
11. `mobile 1`
12. `mobile 2`
13. `email1`
14. `email2`

## 4. Sheet Structure & Visibility
- The **MASTER_MERGED_FINAL** sheet must always be the **first tab** in the workbook.

## 5. Sheet Splitting Logic
Contacts must be distributed into the following 53 standard sheets:

### A. Special Cities (Individual Sheets)
- Bengaluru City, Belagavi City, Hubballi–Dharwad City, Kalaburagi City, Mangaluru City, Mysuru City.

### B. Range Sheets (District Mapping)
- **Northern Range – Belagavi**: Bagalkote, Belagavi, Dharwad, Gadag, Vijayapura.
- **Ballari Range – Ballari**: Ballari, Raichur, Koppal, Vijayanagara.
- **North-Eastern Range – Kalaburag**: Bidar, Kalaburagi, Yadgir.
- **Southern Range – Mysuru**: Chamarajanagara, Hassan, Kodagu, Mandya, Mysuru.
- **Central Range – Bengaluru**: Chikkaballapura, Kolar, Ramanagara, Tumakuru, Bengaluru Rural.
- **Eastern Range – Davanagere**: Chitradurga, Davanagere, Haveri.
- **Western Range – Mangaluru**: Dakshina Kannada, Mangaluru, Udupi, Chikkamagaluru, Shivamogga, Uttara Kannada.

### C. Specialized Units (Functional Sheets)
Contacts belonging to specialized units must use the exact unit names as defined in the Android App registration form:
"L&O", "DAR", "CAR", "Ministerial", "KSRP", "ISD", "Intelligence", "CEN", "DCRE", "FSL", "CID", "Admin", "ASC Team", "BDDS", "Control Room", "CCB", "CCRB", "CDR", "Coast Guard", "Computer", "Court", "CSB", "CSP", "DCIB", "DCRB", "Dog Squad", "DSB", "ERSS", "ESCOM", "Excise", "Fire", "Forest", "FPB", "FRRO", "Guest House", "Health", "Home Guard", "IPS", "Lokayukta", "Others", "Prison", "Railway", "RTO", "SCRB", "Social Media", "Toll", "Traffic", "VVIP", "Wireless", "Training".

## 4. Administrative Formatting
- **Header Row**: Row 1 must be **BOLD**.
- **Freeze Panes**: Row 1 must be frozen (Freeze Top Row).
- **Auto-Fit**: All columns must be auto-fitted for readability.
- **Master Sheet Name**: `MASTER_MERGED_FINAL`.

## 5. Naming Standards & Rank Abbreviations
All ranks must be shortened to their official abbreviations to keep the directory clean and compatible with the app dropdowns:
- **Director General & IGP** / **Director General and IGP** → **DG & IGP**
- **Director General of Police** → **DGP**
- **Additional Director General of Police** / **Additional DGP** / **Addl DGP** → **ADGP**
- **Inspector General of Police** → **IGP**
- **Deputy Inspector General of Police** → **DIG**
- **Superintendent of Police** → **SP**
- **Deputy Commissioner of Police** → **DCP**
- **Additional Superintendent of Police** / **Additional SP** / **ADDL_SP** → **Addl.SP**
- **Deputy Superintendent of Police** / **Deputy SP** / **DSP** → **DySP**
- **Assistant Superintendent of Police** / **Assistant SP** → **ASP**
- **Assistant Commissioner of Police** → **ACP**
- **Commandant** → **CMDT**
- **Deputy Commandant** → **DEPT.CMDT**
- **Assistant Commandant** → **ASST.CMDT**
- **Circle Police Inspector** / **Circle PI** → **CPI**
- **Reserve Police Inspector** / **Reserve PI** → **RPI**
- **Women Police Inspector** → **WPI**
- **Police Inspector** → **PI**
- **Police Sub Inspector** / **Sub-Inspector** → **PSI**
- **Assistant Sub Inspector** → **ASI**
- **Head Constable** → **HC**
- **Police Constable** → **PC**
- **Administrative Officer** → **AO**
- **Assistant Administrative Officer** → **AAO**

### AAO and AO Formatting Rules
For ministerial positions like AAO (Assistant Administrative Officer) and AO (Administrative Officer), the display name is conditionally formatted based on the unit type:
1. **Special/Functional Units**: Format as `AAO <Special Unit>` (e.g., `"AAO KPA"`, `"AAO CLM"`).
2. **Range Units**: Format as `AAO <District>` (e.g., `"AAO Ramanagara"`, `"AAO Mandya"`).
This prevents redundant rank duplication (like `"AAO AAO"`) and provides clear context on which specific office the contact represents.

### Range Abbreviations
All range names must be shortened to their official abbreviations to keep unit labels and search queries concise:
- **Central Range** → **CR**
- **Western Range** → **WR**
- **Northern Range** → **NR**
- **Southern Range** → **SR**
- **Eastern Range** → **ER**
- **Ballari Range** → **BR**
- **North-Eastern Range** → **NER**

### Unit Short Codes
To keep search results, cards, and display names concise, large functional unit names must be shortened to their official abbreviations:
- **Communication, Logistics & Modernisation** → **CLM**
- **Karnataka Police Academy (KPA)** → **KPA**
- **Police Training Wing** → **Training**
- **Police Recruitment Wing** → **Recruitment**
- **Karnataka Railways Police** → **Railways**
- **Police Computer Wing (PCW) / SCRB** → **PCW / SCRB**
- **State Intelligence** → **Intelligence**
- **Directorate of Civil Rights Enforcement (DCRE)** → **DCRE**
- **Criminal Investigation Department (CID)** → **CID**
- **Internal Security Division (ISD)** → **ISD**
- **Karnataka State Reserve Police (KSRP)** → **KSRP**

*Formatting Rule*: Range names in the directory (e.g., in the `UNIT` or `Range` column) must be formatted as `<ShortenedRange>, <Location>` (e.g., `"Northern Range – Belagavi"` becomes `"NR, Belagavi"`; `"North-Eastern Range – Kalaburagi"` becomes `"NER, Kalaburagi"`). The separator must strictly be a comma followed by a space (`, `).

*Other Rules:*
- **Name Column & Designation Rank Normalization**: Rank abbreviations must be strictly applied when the rank appears embedded within the text of the `Name`, `office`, or `station` column itself (e.g., `"Additional Director General of Police, Admin"` becomes `"ADGP, Admin"`; `"DSP ACB"` becomes `"DySP ACB"`; `"DSP Airport PS"` becomes `"DySP Airport PS"`).
- **Retired Officers**: The prefix **RETD.** must be preserved when cleaning the `Rank` column (e.g., "RETD. DG & IGP").
- Use **Training** instead of **PTS** or **Police Training School**.
- Use **CSP** instead of **Coastal Security Police**.
## 6. Official App Ranks
All ranks must strictly map to one of the following official designations as defined in the Android App:

- **Constables**: "CPC", "APC", "S.RPC", "WPC", "PCW", "PC"
- **Head Constables**: "CHC", "AHC", "S.RHC", "WHC", "HCW", "HC"
- **ASI Group**: "ASI", "WASI", "ARSI", "ASIW", "S.ARSI"
- **RSI/PSI Group**: "RSI", "S.RSI", "PSI", "PSIW", "WPSI"
- **PI Group**: "PI", "PIW", "RPI", "S.RPI", "CPI", "WPI"
- **Officers**: "ACP", "DySP", "ASP", "Addl.SP", "SP", "ASST.CMDT", "DEPT.CMDT", "CMDT", "DCP", "DIG", "IGP", "ADGP", "DGP", "DG", "DG & IGP"
- **Ministerial/Support**: "FDA", "SDA", "SS", "STENO", "TYPIST", "PA", "FOLLOWER"
- **Intelligence/Others**: "IA", "AIO", "IO", "SIA", "CIO", "AAO", "AD", "DD", "AO"

## 7. Official Ranges, Districts, & Subdivisions
The directory must exactly mirror the Range-to-District mapping defined in the Android App (`Constants.kt`):

### Range Mapping
- **Central Range**: Bengaluru Urban, Bengaluru Dist, Kolar, Chikkaballapura, Ramanagara, Tumakuru
- **Northern Range**: Belagavi Dist, Vijayapura, Dharwad, Bagalkot, Gadag
- **North Eastern Range**: Kalaburagi, Bidar, Yadgir
- **Ballari Range**: Ballari, Raichur, Koppal, Vijayanagara
- **Southern Range**: Mysuru Dist, Chamarajanagar, Hassan, Kodagu, Mandya
- **Western Range**: Dakshina Kannada, Udupi, Chikkamagaluru, Shivamogga, Uttara Kannada
- **Davangere Range**: Davanagere, Chitradurga, Haveri
- **Commissionerates**: Bengaluru City, Hubballi Dharwad City, Mysuru City, Mangaluru City, Belagavi City, Kalaburagi City

### Subdivisions & Stations
- **Subdivisions**: Must be explicitly preserved and accurately mapped to their parent District.
- **Stations**: Must strictly match the `stationsByDistrictMap` lists in the app (e.g., "Bagalkot Town PS", "Badami PS"). If a new station is added to the Excel sheet, it MUST be added to the app's `Constants.kt` to prevent dropdown failures in the registration form.
