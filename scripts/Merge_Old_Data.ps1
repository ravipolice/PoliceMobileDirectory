$ErrorActionPreference = "Stop"

$oldPath = "c:\Users\ravip\AndroidStudioProjects\PoliceMobileDirectory\officers_export_2026-05-06_15-16.csv"
$newPath = "c:\Users\ravip\AndroidStudioProjects\PoliceMobileDirectory\KSP_Officers_App.csv"

Write-Host "Loading datasets..."
$oldData = Import-Csv $oldPath -Encoding UTF8
$newData = Import-Csv $newPath -Encoding UTF8

function Clean-String($s) {
    if (-not $s) { return "" }
    return ($s -replace '[^\w]','') -replace '\s','' | ForEach-Object { $_.ToLower() }
}

Write-Host "Building lookup table..."
$oldLookup = @{}
foreach ($o in $oldData) {
    if ($o.Mobile -ne "") { $oldLookup["MOB_" + $o.Mobile] = $o }
    if ($o.Landline -ne "") { $oldLookup["LAN_" + ($o.Landline -replace '\D','')] = $o }
    if ($o.Name -ne "") { $oldLookup["NAM_" + (Clean-String $o.Name)] = $o }
}

$matchedCount = 0
$unmatchedCount = 0

Write-Host "Merging data..."
foreach ($n in $newData) {
    $match = $null
    
    $m = "MOB_" + $n.mobile
    $l = "LAN_" + ($n.landline -replace '\D','')
    $nameStr = $n.rank + $n.station
    $nam = "NAM_" + (Clean-String $nameStr)
    
    if ($n.mobile -ne "" -and $oldLookup.ContainsKey($m)) { $match = $oldLookup[$m] }
    elseif ($n.landline -ne "" -and $oldLookup.ContainsKey($l)) { $match = $oldLookup[$l] }
    elseif ($nameStr -ne "" -and $oldLookup.ContainsKey($nam)) { $match = $oldLookup[$nam] }
    
    if ($match) {
        $matchedCount++
        
        # Override fields with legacy curated data
        $n.district = $match.District
        $n.unit     = $match.Unit
        
        # Check if the old District is actually a Range
        if ($match.District -match "Range") {
            $n.range = $match.District
            # Clear district if it's purely a range, though the old DB put them in District
        }
        
        # Override station with old Office, but try to strip the rank to prevent "ACP ACP Banaswadi"
        $oldOffice = $match.Office
        $rank = $n.rank
        if ($rank -ne "" -and $oldOffice.StartsWith($rank)) {
            $oldOffice = $oldOffice.Substring($rank.Length).Trim(' ', '-', ',', '.')
        }
        $n.station = $oldOffice
        
        # Update searchBlob
        $blobFields = @(
            $n.agid,
            $n.name,
            $n.rank,
            $n.station,
            $n.unit,
            $n.range,
            $n.district,
            $n.office,
            $n.landline,
            $n.landline2,
            $n.mobile,
            $n.mobile2,
            $n.email
        ) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | ForEach-Object { $_.ToLower().Trim() }
        
        $n.searchBlob = "  [$($n.agid)] " + ($blobFields -join " ")
    } else {
        $unmatchedCount++
    }
}

Write-Host "Matched: $matchedCount"
Write-Host "Unmatched (Kept raw scraped data): $unmatchedCount"

Write-Host "Exporting updated CSV to $newPath..."
$newData | Export-Csv $newPath -NoTypeInformation -Encoding UTF8
Write-Host "Done!"
