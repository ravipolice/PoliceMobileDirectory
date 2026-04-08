
$inputFile = "C:\Users\ravip\.gemini\antigravity\brain\98428531-3e32-4c5f-8d9d-b9948623171c\.system_generated\steps\224\content.md"
$outputFile = "C:\Users\ravip\AndroidStudioProjects\PoliceMobileDirectory\KSP_Contact_Sheet_Final.csv"

if (-not (Test-Path $inputFile)) {
    Write-Error "Input file not found: $inputFile"
    exit
}

$lines = Get-Content $inputFile
$results = New-Object System.Collections.Generic.List[PSObject]

$currentDistrict = "State Headquarters"
$currentUnit = ""
$currentStation = ""
$currentRank = ""
$currentName = ""
$tempMobiles = New-Object System.Collections.Generic.List[string]
$tempLandlines = New-Object System.Collections.Generic.List[string]
$tempEmails = New-Object System.Collections.Generic.List[string]

function Save-Entry {
    if ($tempMobiles.Count -gt 0 -or $tempLandlines.Count -gt 0 -or $tempEmails.Count -gt 0) {
        # Final cleanup of rank
        $cleanRank = $currentRank.Replace("Click a section header to expand", "").Replace("Â·", "").Trim()
        if ($cleanRank -match "^Email$|^Phone$|^Sl No$|^Sl\.No\.$|^Sl\. No\.$" -or $cleanRank -eq "") { return }

        $obj = [PSCustomObject]@{
            name      = $currentName
            rank      = $cleanRank
            district  = $currentDistrict
            unit      = $currentUnit
            station   = $currentStation
            mobile    = if ($tempMobiles.Count -gt 0) { $tempMobiles[0] } else { "" }
            mobile2   = if ($tempMobiles.Count -gt 1) { $tempMobiles[1] } else { "" }
            landline  = if ($tempLandlines.Count -gt 0) { $tempLandlines[0] } else { "" }
            landline2 = if ($tempLandlines.Count -gt 1) { $tempLandlines[1] } else { "" }
            email     = if ($tempEmails.Count -gt 0) { $tempEmails[0] } else { "" }
        }
        $results.Add($obj)
    }
    $tempMobiles.Clear()
    $tempLandlines.Clear()
    $tempEmails.Clear()
}

foreach ($line in $lines) {
    $line = $line.Trim()
    if ($line -eq "" -or $line -match "^Sl No$|^Sl\.No\.$") { continue }

    # Section headers (#)
    if ($line -match "^#+\s+(.*)") {
        Save-Entry
        $header = $matches[1]
        if ($header -match "District|Range|Commissionerate") {
            $currentDistrict = $header
        } else {
            $currentUnit = $header
        }
        continue
    }

    # Detect Email
    if ($line -match "\[(.*?)\]\(mailto:(.*?)\)" -or $line -match "\[(.*?)\]\(https://.*?email-protection.*?\)") {
        $eText = $matches[1] -split "[,/·;]"
        foreach ($e in $eText) { 
            $e = $e.Trim()
            if ($e -ne "") { $tempEmails.Add($e) }
        }
        continue
    }

    # Detect Phone
    if ($line -match "\d") {
        # Check if it's a phone number line (contains at least 5 digits)
        if ($line -match "\d{5,}") {
            $pText = $line -split "[,/·;]"
            foreach ($p in $pText) {
                $p = $p.Trim().Replace(" ", "").Replace("-", "")
                if ($p -match "^[6789]\d{9}$") {
                    $tempMobiles.Add($p)
                } elseif ($p -match "^0\d{8,10}$" -or $p.Length -gt 5) {
                    $tempLandlines.Add($p)
                }
            }
            continue
        }
    }

    # If it's not email/phone/header, it's Rank or Name
    if ($line.Length -gt 3) {
        # If we already have data, save it before starting a new one
        if ($tempEmails.Count -gt 0 -or $tempMobiles.Count -gt 0 -or $tempLandlines.Count -gt 0) {
            Save-Entry
        }
        
        # Logic to differentiate Name vs Rank:
        # Usually Rank is first, or it's a known title like "SP", "DGP", "Inspector"
        if ($line -match "Police|Officer|Inspector|SP|DGP|ADGP|DIGP|IGP|Commissioner|DySP|PSI|ASI") {
            $currentRank = $line
            $currentName = ""
        } else {
            # Probably a name
            $currentName = $line
        }
    }
}

Save-Entry

# Export
$results | Export-Csv -Path $outputFile -NoTypeInformation -Encoding utf8
Write-Host "Exported $($results.Count) unique contact records to $outputFile"
