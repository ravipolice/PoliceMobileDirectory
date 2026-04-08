
$inputFile = "C:\Users\ravip\.gemini\antigravity\brain\98428531-3e32-4c5f-8d9d-b9948623171c\.system_generated\steps\224\content.md"
$outputFile = "C:\Users\ravip\AndroidStudioProjects\PoliceMobileDirectory\KSP_Contacts_Directory.csv"

if (-not (Test-Path $inputFile)) {
    Write-Error "Input file not found: $inputFile"
    exit
}

$lines = Get-Content $inputFile
$contacts = New-Object System.Collections.Generic.List[PSObject]

$currentSection = "General"
$currentUnit = ""
$currentDesignation = ""
$currentName = ""

# Track state
$lastLineWasEmailHeader = $false
$lastLineWasPhoneHeader = $false

foreach ($line in $lines) {
    $line = $line.Trim()
    if ($line -eq "") { continue }

    # Detect Section Headers (usually single lines or with [Links])
    if ($line -match "^#+\s+(.*)") {
        $currentSection = $matches[1]
        continue
    }

    # Common patterns for designations or units
    if ($line -match "Headquarters|Division|Range|Commissionerate|District") {
        $currentUnit = $line
    }

    # Detect "Email" or "Phone" headers
    if ($line -eq "Email") {
        $lastLineWasEmailHeader = $true
        $lastLineWasPhoneHeader = $false
        continue
    }
    if ($line -eq "Phone") {
        $lastLineWasPhoneHeader = $true
        $lastLineWasEmailHeader = $false
        continue
    }

    # Detect Emails (markdown link format)
    if ($line -match "\[(.*?)\]\(mailto:(.*?)\)" -or $line -match "\[(.*?)\]\(https://.*?email-protection.*?\)") {
        $emailText = $matches[1]
        
        # Split multiple emails if needed
        $emailList = $emailText -split "[,/·;]"
        foreach ($e in $emailList) {
            $e = $e.Trim()
            if ($e -ne "") {
                $contacts.Add([PSCustomObject]@{
                    Section = $currentSection
                    Unit = $currentUnit
                    Designation = $currentDesignation
                    Name = $currentName
                    ContactType = "Email"
                    Value = $e
                })
            }
        }
        $lastLineWasEmailHeader = $false
        continue
    }

    # Detect Phone Numbers (usually contain dashes or digits)
    if ($lastLineWasPhoneHeader -or ($line -match "\d{2,}-\d{5,}" -or $line -match "\d{10}")) {
        # Split multiple phones if needed
        $phoneList = $line -split "[,/·;]"
        foreach ($p in $phoneList) {
            $p = $p.Trim()
            if ($p -ne "") {
                $contacts.Add([PSCustomObject]@{
                    Section = $currentSection
                    Unit = $currentUnit
                    Designation = $currentDesignation
                    Name = $currentName
                    ContactType = "Phone"
                    Value = $p
                })
            }
        }
        $lastLineWasPhoneHeader = $false
        continue
    }

    # If it's not a header, email, or phone, it might be a designation or name
    # We update these as we go
    if ($line.Length -gt 5 -and $line.Length -lt 200) {
        $currentDesignation = $line
    }
}

# Export to CSV
$contacts | Export-Csv -Path $outputFile -NoTypeInformation -Encoding utf8
Write-Host "Exported $($contacts.Count) contact records to $outputFile"
