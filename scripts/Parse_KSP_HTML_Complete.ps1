
# Parse_KSP_HTML_Complete.ps1  (v2 - with email decoding)
# Decodes CloudFlare-protected emails AND extracts plain mailto: links
# Output: KSP_Contacts_Master_Clean.csv

$htmlPath = "c:\Users\ravip\AndroidStudioProjects\PoliceMobileDirectory\ksp_page.html"
$csvPath  = "c:\Users\ravip\AndroidStudioProjects\PoliceMobileDirectory\KSP_Contacts_Master_Clean.csv"

$html = Get-Content $htmlPath -Raw -Encoding UTF8

# ── CloudFlare email decoder ──────────────────────────────────────────────
function Decode-CFEmail($hex) {
    if ([string]::IsNullOrWhiteSpace($hex) -or $hex.Length -lt 4 -or $hex.Length % 2 -ne 0) { return "" }
    try {
        $key = [Convert]::ToInt32($hex.Substring(0,2), 16)
        $sb  = [System.Text.StringBuilder]::new()
        for ($i = 2; $i -lt $hex.Length; $i += 2) {
            [void]$sb.Append([char]([Convert]::ToInt32($hex.Substring($i,2),16) -bxor $key))
        }
        return $sb.ToString()
    } catch { return "" }
}

# ── Decode ALL emails in HTML, replace CDN links with decoded text ─────────
# Replace CDN-encoded email hrefs with decoded email text inline
$decodedHtml = [regex]::Replace(
    $html,
    '(?i)<a\s[^>]*href="https://[^"]*cdn-cgi/l/email-protection#([a-f0-9]+)"[^>]*>[^<]*</a>',
    {
        param($m)
        $decoded = Decode-CFEmail $m.Groups[1].Value
        return $decoded   # just the email text, no <a> tag
    }
)

# Replace plain mailto: links with just the email
$decodedHtml = [regex]::Replace(
    $decodedHtml,
    '(?i)<a\s[^>]*href="mailto:([^"]+)"[^>]*>[^<]*</a>',
    { param($m); return $m.Groups[1].Value }
)

Write-Host "HTML decoded. Starting parse..."

# ── Strip HTML tags and decode entities ─────────────────────────────────────
function Strip-Html($text) {
    if ([string]::IsNullOrWhiteSpace($text)) { return "" }
    $text = [regex]::Replace($text, '<[^>]+>', ' ')
    $text = $text -replace '&amp;','&' -replace '&lt;','<' -replace '&gt;','>' `
                  -replace '&nbsp;',' ' -replace '&#39;',"'" -replace '&quot;','"' `
                  -replace '&#x27;',"'" -replace '&apos;',"'"
    return ([regex]::Replace($text, '\s+', ' ')).Trim()
}

function Clean-Phone($raw) {
    $raw = $raw -replace '(?i)<br\s*/?>', ' / '
    $s = Strip-Html $raw
    $s = $s -replace '\s*[-–—]\s*F\b','' -replace '\(F\)','' -replace '\s*/\s*/', '/'
    return ([regex]::Replace($s, '\s+', ' ')).Trim().Trim('/').Trim()
}

# ── Build position-sorted anchor list ────────────────────────────────────────
$anchors = [System.Collections.Generic.List[PSCustomObject]]::new()

# Section anchors  — use grp-title div (not grp-icon span)
$grpMatches = [regex]::Matches($decodedHtml, '(?is)<div[^>]*class="grp-header"[^>]*>(.*?)</div>\s*</div>')
foreach ($m in $grpMatches) {
    $titleM = [regex]::Match($m.Groups[1].Value, '(?i)<div[^>]*class="grp-title"[^>]*>([^<]{2,80})</div>')
    if ($titleM.Success) {
        $name = (Strip-Html $titleM.Groups[1].Value).Trim()
        $anchors.Add([PSCustomObject]@{ Pos=$m.Index; Type="Section"; Name=$name })
    }
}

# Unit anchors
$subMatches = [regex]::Matches($decodedHtml, '(?is)<div[^>]*class="sub-header"[^>]*>(.*?)</div>')
foreach ($m in $subMatches) {
    $spanM = [regex]::Match($m.Groups[1].Value, '(?i)<span[^>]*>([^<]{2,100})</span>')
    if ($spanM.Success) {
        $anchors.Add([PSCustomObject]@{ Pos=$m.Index; Type="Unit"; Name=(Strip-Html $spanM.Groups[1].Value).Trim() })
    }
}

# Table anchors
$tblMatches = [regex]::Matches($decodedHtml, '(?is)<table[^>]*class="contact-table"[^>]*>(.*?)</table>')
foreach ($m in $tblMatches) {
    $anchors.Add([PSCustomObject]@{ Pos=$m.Index; Type="Table"; Name=$m.Groups[1].Value })
}

$anchors = $anchors | Sort-Object Pos

Write-Host ("Anchors: {0} Sections, {1} Units, {2} Tables" -f
    ($anchors | Where-Object Type -eq 'Section').Count,
    ($anchors | Where-Object Type -eq 'Unit').Count,
    ($anchors | Where-Object Type -eq 'Table').Count)

# ── Walk anchors, assign Section/Unit to each Table ─────────────────────────
$results    = [System.Collections.Generic.List[PSCustomObject]]::new()
$curSection = "State Police Headquarters"
$curUnit    = "General"

foreach ($anchor in $anchors) {
    switch ($anchor.Type) {
        "Section" { $curSection = $anchor.Name }
        "Unit"    { $curUnit    = $anchor.Name  }
        "Table"   {
            $rowMatches = [regex]::Matches($anchor.Name, '(?is)<tr(?:\s[^>]*)?>(.*?)</tr>')
            foreach ($row in $rowMatches) {
                $rowHtml = $row.Groups[1].Value
                if ($rowHtml -imatch '<th')             { continue }
                if ($rowHtml -imatch 'class="title-row"') { continue }

                $cells = [regex]::Matches($rowHtml, '(?is)<td[^>]*>(.*?)</td>')
                if ($cells.Count -lt 2) { continue }

                $desig = Strip-Html $cells[0].Groups[1].Value
                if ([string]::IsNullOrWhiteSpace($desig) -or $desig.Length -lt 2) { continue }
                if ($desig -match '^\d+$') { continue }

                $phone  = if ($cells.Count -gt 1) { Clean-Phone $cells[1].Groups[1].Value } else { "" }
                $mobile = if ($cells.Count -gt 2) { Clean-Phone $cells[2].Groups[1].Value } else { "" }
                
                # Email cell — now decoded, just strip remaining tags
                $rawEmail = if ($cells.Count -gt 3) { Strip-Html $cells[3].Groups[1].Value } else { "" }
                # Keep only valid-looking emails
                $emails = ($rawEmail -split '[,\s]+') | Where-Object { $_ -match '@[a-z]' } | Select-Object -Unique
                $emailStr = $emails -join ', '

                $allPhone = @($phone, $mobile) | Where-Object { $_ -ne "" } | Select-Object -Unique
                $phoneStr = $allPhone -join " / "

                $results.Add([PSCustomObject]@{
                    Section     = $curSection
                    Unit        = $curUnit
                    Designation = $desig
                    Phone       = $phoneStr
                    Email       = $emailStr
                })
            }
        }
    }
}

# ── Summary ───────────────────────────────────────────────────────────────
$byUnit = $results | Group-Object Unit | Sort-Object Name
Write-Host "`n=== Units: $($byUnit.Count) | Total records: $($results.Count) ==="
$withEmail = ($results | Where-Object { $_.Email -ne "" }).Count
Write-Host "Records with email: $withEmail  |  Without: $($results.Count - $withEmail)"

Write-Host "`nSample emails:"
$results | Where-Object { $_.Email -ne "" } | Select-Object -First 8 | Format-Table Designation, Email -AutoSize

$results | Export-Csv -Path $csvPath -NoTypeInformation -Encoding UTF8
Write-Host "`nSaved: $csvPath"
