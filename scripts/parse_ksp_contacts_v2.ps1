
$inputFile = "C:\Users\ravip\.gemini\antigravity\brain\98428531-3e32-4c5f-8d9d-b9948623171c\.system_generated\steps\224\content.md"
$outputFile = "C:\Users\ravip\AndroidStudioProjects\PoliceMobileDirectory\KSP_Contacts_Master.csv"

if (-not (Test-Path $inputFile)) {
    Write-Error "Input file not found: $inputFile"
    exit
}

$lines = Get-Content $inputFile
$results = New-Object System.Collections.Generic.List[PSObject]

$currentSection = "State Police Headquarters"
$currentUnit = ""
$currentDesignation = ""
$currentName = ""
$tempPhones = New-Object System.Collections.Generic.List[string]
$tempEmails = New-Object System.Collections.Generic.List[string]

function Save-CurrentContact {
    if ($tempPhones.Count -gt 0 -or $tempEmails.Count -gt 0) {
        $cleanDesignation = $currentDesignation.Replace("Click a section header to expand", "").Replace("Â·", "").Trim()
        if ($cleanDesignation -match "^Email$|^Phone$" -or $cleanDesignation -eq "") { 
            # Skip if it's just a header word
        } else {
            $obj = [PSCustomObject]@{
                Section     = $currentSection
                Unit        = $currentUnit
                Designation = $cleanDesignation
                Name        = $currentName
                Phone1      = if ($tempPhones.Count -gt 0) { $tempPhones[0] } else { "" }
                Phone2      = if ($tempPhones.Count -gt 1) { $tempPhones[1] } else { "" }
                Phone3      = if ($tempPhones.Count -gt 2) { $tempPhones[2] } else { "" }
                Email1      = if ($tempEmails.Count -gt 0) { $tempEmails[0] } else { "" }
                Email2      = if ($tempEmails.Count -gt 1) { $tempEmails[1] } else { "" }
            }
            $results.Add($obj)
        }
    }
    $tempPhones.Clear()
    $tempEmails.Clear()
}

foreach ($line in $lines) {
    $line = $line.Trim()
    if ($line -eq "") { continue }

    # Section Headers
    if ($line -match "^#+\s+(.*)") {
        Save-CurrentContact
        $currentSection = $matches[1]
        continue
    }

    # Detect Email
    if ($line -match "\[(.*?)\]\(mailto:(.*?)\)" -or $line -match "\[(.*?)\]\(https://.*?email-protection.*?\)") {
        $emailText = $matches[1]
        $emailList = $emailText -split "[,/·;]"
        foreach ($e in $emailList) { 
            if ($e.Trim() -ne "") { $tempEmails.Add($e.Trim()) }
        }
        continue
    }

    # Detect Phone
    if ($line -match "\d{2,}-\d{5,}" -or $line -match "^\d{10}$" -or $line -match "\d{3}-\d{3}-\d{4}") {
        $phoneList = $line -split "[,/·;]"
        foreach ($p in $phoneList) { 
            if ($p.Trim() -ne "") { $tempPhones.Add($p.Trim()) }
        }
        continue
    }

    # If line is "Email" or "Phone" or "Sl No", skip
    if ($line -match "^Email$|^Phone$|^Sl No$|^Name$|^Designation$") {
        continue
    }

    # If we hit a new designation/name potential line, and we have data for the previous one, save it
    if ($line.Length -gt 3) {
        if ($tempPhones.Count -gt 0 -or $tempEmails.Count -gt 0) {
            Save-CurrentContact
        }
        # Heuristic: If line contains a lot of uppercase or common titles, it's a designation
        $currentDesignation = $line
        $currentName = "" # Clear name for new entry unless we find it
    }
}

Save-CurrentContact # Final save

# Export
$results | Export-Csv -Path $outputFile -NoTypeInformation -Encoding utf8
Write-Host "Exported $($results.Count) unique contact records to $outputFile"
