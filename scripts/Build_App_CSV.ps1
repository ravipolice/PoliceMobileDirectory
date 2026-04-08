
# Build_App_CSV.ps1  (v2 - all Officer fields matched)
# Maps KSP contacts to exact Officer model schema for Firestore import via Google Sheets
#
# Officer fields:
# agid | name | rank | station | unit | district | office | landline | landline2 |
# mobile | mobile2 | email | bloodGroup | photoUrl | isHidden | searchBlob

$inCsv  = "c:\Users\ravip\AndroidStudioProjects\PoliceMobileDirectory\KSP_Contacts_Final.csv"
$outCsv = "c:\Users\ravip\AndroidStudioProjects\PoliceMobileDirectory\KSP_Officers_App.csv"

$data = Import-Csv $inCsv -Encoding UTF8

# ── Rank prefix list (longest first to avoid partial matches) ─────────────
$rankPrefixes = @(
    "Director General & Inspector General of Police",
    "Director General of Police",
    "Additional Director General",
    "Additional Director",
    "Additional Superintendent of Police",
    "Additional Inspector General of Police",
    "Additional Inspector General",
    "Inspector General of Police",
    "Deputy Inspector General of Police",
    "Deputy Inspector General",
    "Superintendent of Police",
    "Deputy Superintendent of Police",
    "Assistant Superintendent of Police",
    "Deputy Commissioner of Police",
    "Assistant Commissioner of Police",
    "Circle Police Inspector",
    "Police Inspector",
    "Police Sub-Inspector",
    "Assistant Police Inspector",
    "DG & IGP", "DG&IGP",
    "Addl. IG", "Addl. SP-1", "Addl. SP-2", "Addl. SP", "Addl. DG", "Addl. DCP",
    "AC",
    "ADGP", "DGP",
    "IGP", "DIGP",
    "SP (W)", "SP(W)", "DSP (W)", "DSP(W)", "PI (W)", "PI(W)", "ACP (W)", "ACP(W)",
    "SP", "DSP", "ASP",
    "DCP", "ADCP",
    "ACP",
    "CPI",
    "SDPO",
    # DySP variants
    "DySP",
    "PI(W)-1", "PI(W)-2", "PI(W)-3",
    "PI",
    "PSI-1", "PSI-2", "PSI-3", "PSI-4", "PSI",
    "SI", "ASI",
    "SHO",
    "HC", "PC",
    "HM",
    # RPI (Reserve Police Inspector)
    "RPI-1", "RPI-2", "RPI-3", "RPI",
    # RSI (Reserve Sub Inspector)
    "RSI",
    # Principal/Vice Principal
    "Vice Principal", "Principal",
    # Commandant
    "6th BN Commdt.",
    # Admin
    "AAO", "AO", "FAO",
    "CAO", "APRO",
    "PA to", "P.A. to",
    "Secretary",
    "Member Secretary",
    "PA"
)

# ── District extraction from unit name ────────────────────────────────────
# Maps known unit patterns to districts
$unitToDistrict = @{
    # Commissionerates
    "Bengaluru City"                                   = "Bengaluru Urban"
    "Bengaluru Metropolitan Task Force (BMTF)"         = "Bengaluru Urban"
    "Hubballi-Dharwad City"                            = "Dharwad"
    "Mangaluru City"                                   = "Dakshina Kannada"
    "Mysuru City"                                      = "Mysuru"
    "Belagavi City"                                    = "Belagavi"
    "Kalaburagi City"                                  = "Kalaburagi"
    # Ranges
    "Central Range - Bengaluru"                        = "Bengaluru Urban"
    "Northern Range - Belagavi"                        = "Belagavi"
    "Western Range - Mangaluru"                        = "Dakshina Kannada"
    "Southern Range - Mysuru"                          = "Mysuru"
    "Eastern Range - Davanagere"                       = "Davangere"
    "Ballari Range - Ballari"                          = "Ballari"
    "North-Eastern Range - Kalaburagi"                 = "Kalaburagi"
    # HQ & Special
    "Headquarters"                                     = "Bengaluru Urban"
    "State Intelligence"                               = "Bengaluru Urban"
    "Control Rooms in Bengaluru"                       = "Bengaluru Urban"
    "Karnataka Lokayukta"                              = "Bengaluru Urban"
    "Directorate of Civil Rights Enforcement (DCRE)"   = "Bengaluru Urban"
    "Directorate of Forensic Science"                  = "Bengaluru Urban"
    "Finger Print Bureau (FPB)"                        = "Bengaluru Urban"
    "Criminal Investigation Department (CID)"          = "Bengaluru Urban"
    "Internal Security Division (ISD)"                 = "Bengaluru Urban"
    "Karnataka Railways Police"                        = "Bengaluru Urban"
    "KSRP Battalions"                                  = "Bengaluru Urban"
    "Karnataka State Reserve Police (KSRP)"            = "Bengaluru Urban"
    "Communication, Logistics & Modernisation"         = "Bengaluru Urban"
    "Department of Prisons & Correctional Services"    = "Bengaluru Urban"
    "Home, Civil Defence, Fire & Emergency Services"   = "Bengaluru Urban"
    "IRB Battalions & Training Institutions"           = "Bengaluru Urban"
    "IRB"                                              = "Bengaluru Urban"
    "KSPH & IDCL"                                      = "Bengaluru Urban"
    "Karnataka Police Academy (KPA)"                   = "Mysuru"
    "SPORTI"                                           = "Bengaluru Urban"
    "Special Action Force (SAF) - Mangaluru"           = "Dakshina Kannada"
    "Special Investigation Team (SIT)"                 = "Bengaluru Urban"
    "Special Task Force (STF)"                         = "Bengaluru Urban"
    "Forest Cell"                                      = "Bengaluru Urban"
    "Police Computer Wing (PCW) / SCRB"                = "Bengaluru Urban"
    "Police Recruitment Wing"                          = "Bengaluru Urban"
    "Police Training Institutions"                     = "Bengaluru Urban"
    "Police Training Wing"                             = "Bengaluru Urban"
    "Karnataka State Police Complaint Authority"       = "Bengaluru Urban"
    "Karnataka State Sports Promotion Control Board"   = "Bengaluru Urban"
    "Retired Police Officers"                          = ""
    "Officers on State Deputation"                     = ""
}

$districtToRange = @{
    "Bengaluru Rural" = "Central Range"
    "Tumakuru" = "Central Range"
    "Kolar" = "Central Range"
    "Chikkaballapura" = "Central Range"
    "Ramanagara" = "Central Range"
    "Davanagere" = "Eastern Range"
    "Chitradurga" = "Eastern Range"
    "Haveri" = "Eastern Range"
    "Belagavi" = "Northern Range"
    "Vijayapura" = "Northern Range"
    "Bagalkot" = "Northern Range"
    "Gadag" = "Northern Range"
    "Dakshina Kannada" = "Western Range"
    "Udupi" = "Western Range"
    "Uttara Kannada" = "Western Range"
    "Chikkamagaluru" = "Western Range"
    "Mysuru" = "Southern Range"
    "Mandya" = "Southern Range"
    "Hassan" = "Southern Range"
    "Kodagu" = "Southern Range"
    "Chamarajanagara" = "Southern Range"
    "Ballari" = "Ballari Range"
    "Koppal" = "Ballari Range"
    "Raichur" = "Ballari Range"
    "Vijayanagara" = "Ballari Range"
    "Kalaburagi" = "North-Eastern Range"
    "Bidar" = "North-Eastern Range"
    "Yadgir" = "North-Eastern Range"
    "Bengaluru Urban" = ""
    "Hubballi-Dharwad" = ""
}

# ── Load Legacy Data ──────────────────────────────────────────────────────
$oldLookup = @{}
$oldDataPath = Join-Path $PSScriptRoot "..\officers_export_2026-05-06_15-16.csv"
if (Test-Path $oldDataPath) {
    Write-Host "Loading legacy export data for mapping..."
    $oldData = Import-Csv $oldDataPath -Encoding UTF8
    foreach ($o in $oldData) {
        if ($o.Mobile -ne "") { $oldLookup["MOB_" + $o.Mobile] = $o }
        if ($o.Landline -ne "") { $oldLookup["LAN_" + ($o.Landline -replace '\D','')] = $o }
        if ($o.Name -ne "") { 
            $cleanName = ($o.Name -replace '[^\w]','') -replace '\s','' | ForEach-Object { $_.ToLower() }
            $oldLookup["NAM_" + $cleanName] = $o 
        }
    }
}

$districts = @(
    "Bagalkot", "Ballari", "Belagavi", "Bengaluru Rural", "Bengaluru Urban",
    "Bidar", "Chamarajanagara", "Chikkaballapura", "Chikkamagaluru", "Chitradurga",
    "Dakshina Kannada", "Davanagere", "Dharwad", "Gadag", "Hassan", "Haveri",
    "Kalaburagi", "Kodagu", "Kolar", "Koppal", "Mandya", "Mysuru", "Raichur",
    "Ramanagara", "Shivamogga", "Tumakuru", "Udupi", "Uttara Kannada", "Vijayapura",
    "Yadgir", "Vijayanagara"
)

function Get-District($unit, $station) {
    # 1. First check if the station explicitly contains a district name
    foreach ($d in $districts) {
        if ($station -match "\b$d\b") { return $d }
    }
    # Special cases for station
    if ($station -match "\bBengaluru\b") { return "Bengaluru Urban" }
    if ($station -match "\bMangaluru\b") { return "Dakshina Kannada" }
    if ($station -match "\bHubballi\b") { return "Dharwad" }
    if ($station -match "\bBelgaum\b") { return "Belagavi" }
    if ($station -match "\bMysore\b") { return "Mysuru" }
    if ($station -match "\bBellary\b") { return "Ballari" }
    if ($station -match "\bBijapur\b") { return "Vijayapura" }
    if ($station -match "\bGulbarga\b") { return "Kalaburagi" }
    
    # 2. Then check the unit
    foreach ($d in $districts) {
        if ($unit -match "\b$d\b") { return $d }
    }
    if ($unit -match "Mangaluru") { return "Dakshina Kannada" }
    if ($unit -match "Hubballi") { return "Dharwad" }
    if ($unit -match "Bengaluru") { return "Bengaluru Urban" }
    
    # 3. Fallbacks for specific headquarters units that default to Bengaluru Urban
    if ($unit -match "Headquarters|CID|ISD|Directorate|State Intelligence|KSRP|Police Computer Wing") {
        return "Bengaluru Urban"
    }
    
    return ""
}

# ── Designation splitter ──────────────────────────────────────────────────
function Split-Designation($desig) {
    $desig = $desig.Trim()

    # Retired: "NAME RETD. RANK"
    if ($desig -match '^(.+?)\s+RETD\.\s+(.+)$') {
        return @{ Rank = "$($Matches[2].Trim()) (Retd.)"; Station = ""; Name = $Matches[1].Trim() }
    }

    # Try known rank prefixes
    foreach ($rank in $rankPrefixes) {
        $escaped = [regex]::Escape($rank)
        # RANK, STATION or RANK STATION
        if ($desig -imatch "^($escaped)\s*[,]?\s+(.+)$") {
            return @{ Rank = $Matches[1].Trim(); Station = $Matches[2].Trim().TrimStart(',').Trim(); Name = "" }
        }
        if ($desig -ieq $rank) {
            return @{ Rank = $rank; Station = ""; Name = "" }
        }
    }

    # PSI-N STATION
    if ($desig -imatch '^(PSI-\d+)\s+(.+)$') {
        return @{ Rank = $Matches[1]; Station = $Matches[2].Trim(); Name = "" }
    }

    # No rank detected
    return @{ Rank = ""; Station = $desig; Name = "" }
}

function Extract-Phones($rawStrings) {
    $all = $rawStrings -join " / "
    $parts = $all -split '[/,&|\n]'
    
    $mobiles = @()
    $landlines = @()

    foreach ($p in $parts) {
        if ($p -match '(\d[\d\-\s]{5,}\d)') {
            $num = $Matches[1] -replace '\s',''
            $cleanNum = $num -replace '\D',''
            
            if ($cleanNum -match '^(?:0|91)?([6-9]\d{9})$') {
                $mobiles += $num
            } elseif ($cleanNum.Length -ge 5) {
                $landlines += $num
            }
        }
    }
    
    return @{
        Mobiles = @($mobiles | Select-Object -Unique)
        Landlines = @($landlines | Select-Object -Unique)
    }
}

# ── Build output ──────────────────────────────────────────────────────────
$result = [System.Collections.Generic.List[PSCustomObject]]::new()
$idx    = 1

foreach ($row in $data) {
    $split    = Split-Designation $row.Designation
    $agid     = "KSP{0:D4}" -f $idx

    # name: only for retired officers; blank for positional entries
    $name     = $split.Name   # empty string for positional entries

    $rank     = $split.Rank
    $station  = $split.Station
    $unit     = $row.Unit
    $district = Get-District $unit $station
    
    # --- LEGACY DATA LOOKUP ---
    $match = $null
    $m = "MOB_" + $row.Mobile1
    $l = "LAN_" + ($row.Office1 -replace '\D','')
    $nameStr = $rank + $station
    $nam = "NAM_" + (($nameStr -replace '[^\w]','') -replace '\s','' | ForEach-Object { $_.ToLower() })
    
    if ($row.Mobile1 -ne "" -and $oldLookup.ContainsKey($m)) { $match = $oldLookup[$m] }
    elseif ($row.Office1 -ne "" -and $oldLookup.ContainsKey($l)) { $match = $oldLookup[$l] }
    elseif ($nameStr -ne "" -and $oldLookup.ContainsKey($nam)) { $match = $oldLookup[$nam] }
    
    if ($match) {
        # Override fields with legacy curated data
        $unit = $match.Unit
        
        # Override station but strip rank to prevent "ACP ACP Banaswadi"
        $oldOffice = $match.Office
        if ($rank -ne "" -and $oldOffice.StartsWith($rank)) {
            $oldOffice = $oldOffice.Substring($rank.Length).Trim(' ', '-', ',', '.')
        }
        $station = $oldOffice
        
        # For district, if the legacy district is actually a range name, we use it to calculate Range later
        # but don't set it as the actual District (unless it's a valid district).
        if ($match.District -match "Range") {
            # It's a range, so leave district blank for range-level officers
            $district = ""
            $unit = $match.District # Keep the legacy unit (e.g. Central Range Bengaluru)
        } else {
            $district = $match.District
        }
    }
    # --------------------------

    $range    = if ($districtToRange.ContainsKey($district)) { $districtToRange[$district] } else { "" }
    
    # Fallback to Unit name if Unit is a Range
    if ($range -eq "" -and $unit -match 'Range') { 
        $range = ($unit -split '[-–]')[0].Trim() 
        # Standardize range names
        if ($range -match 'Central') { $range = 'Central Range' }
        if ($range -match 'Eastern') { $range = 'Eastern Range' }
        if ($range -match 'Western') { $range = 'Western Range' }
        if ($range -match 'Southern') { $range = 'Southern Range' }
        if ($range -match 'Northern') { $range = 'Northern Range' }
        if ($range -match 'North-Eastern|Northeastern') { $range = 'North-Eastern Range' }
        if ($range -match 'Ballari') { $range = 'Ballari Range' }
    }

    $office   = $row.Section   # "State Police Headquarters" / "Commissionerates" / "Ranges" / "Special Units"

    $phones = Extract-Phones @($row.Office1, $row.Office2, $row.Mobile1, $row.Mobile2)
    
    $landline  = if ($phones.Landlines.Count -gt 0) { $phones.Landlines[0] } else { "" }
    $landline2 = if ($phones.Landlines.Count -gt 1) { $phones.Landlines[1] } else { "" }
    $mobile    = if ($phones.Mobiles.Count -gt 0) { $phones.Mobiles[0] } else { "" }
    $mobile2   = if ($phones.Mobiles.Count -gt 1) { $phones.Mobiles[1] } else { "" }

    $email     = $row.Email

    # searchBlob — everything searchable in one lowercase string
    $blobParts = @($name, $rank, $station, $unit, $district, $office, $landline, $landline2, $mobile, $mobile2, $email) |
        Where-Object { $_ -ne $null -and $_ -ne "" }
    $searchBlob = ($blobParts -join " ").ToLower()

    $result.Add([PSCustomObject]@{
        agid        = $agid
        name        = $name
        rank        = $rank
        station     = $station
        unit        = $unit
        range       = $range
        district    = $district
        office      = $office
        landline    = $landline
        landline2   = $landline2
        mobile      = $mobile
        mobile2     = $mobile2
        email       = $email
        bloodGroup  = ""
        photoUrl    = ""
        isHidden    = "false"
        searchBlob  = $searchBlob
    })
    $idx++
}

# ── Stats ──────────────────────────────────────────────────────────────────
$withRank     = ($result | Where-Object { $_.rank    -ne "" }).Count
$withStation  = ($result | Where-Object { $_.station -ne "" }).Count
$withDistrict = ($result | Where-Object { $_.district -ne "" }).Count
$withEmail    = ($result | Where-Object { $_.email   -ne "" }).Count
$retd         = ($result | Where-Object { $_.rank    -match 'Retd' }).Count

Write-Host "Total records    : $($result.Count)"
Write-Host "With rank        : $withRank"
Write-Host "With station     : $withStation"
Write-Host "With district    : $withDistrict"
Write-Host "With email       : $withEmail"
Write-Host "Retired officers : $retd"

Write-Host "`n=== Sample (first 8 rows) ==="
$result | Select-Object -First 8 | Format-Table agid, name, rank, station, district, office, landline, mobile -AutoSize

Write-Host "`n=== Retired officers (5 samples) ==="
$result | Where-Object { $_.rank -match 'Retd' } | Select-Object -First 5 |
    Format-Table agid, name, rank, unit -AutoSize

Write-Host "`n=== PSI samples ==="
$result | Where-Object { $_.rank -match '^PSI' } | Select-Object -First 5 |
    Format-Table agid, rank, station, unit, district, mobile -AutoSize

Write-Host "`n=== SearchBlob sample (first 3) ==="
$result | Select-Object -First 3 | ForEach-Object {
    Write-Host "  [$($_.agid)] $($_.searchBlob)"
}

$result | Export-Csv $outCsv -NoTypeInformation -Encoding UTF8
Write-Host "`nSaved: $outCsv"
Write-Host "Columns: agid | name | rank | station | unit | range | district | office | landline | landline2 | mobile | mobile2 | email | bloodGroup | photoUrl | isHidden | searchBlob"
