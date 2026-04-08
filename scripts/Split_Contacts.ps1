
# Split_Contacts.ps1
# Splits the combined Phone field into separate Office_Phone and Mobile columns
# Format in CSV: "office1, office2 / mobile1, mobile2"
# Result: Section, Unit, Designation, Office_Phone, Mobile, Email

$inCsv  = "c:\Users\ravip\AndroidStudioProjects\PoliceMobileDirectory\KSP_Contacts_Master_Clean.csv"
$outCsv = "c:\Users\ravip\AndroidStudioProjects\PoliceMobileDirectory\KSP_Contacts_Split.csv"
$xlsxPath = "c:\Users\ravip\AndroidStudioProjects\PoliceMobileDirectory\KSP_Contacts_UnitWise.xlsx"

$data = Import-Csv $inCsv -Encoding UTF8

# ── Helper: clean a single phone token ────────────────────────────────────
function Clean-Token($t) {
    $t = $t.Trim().Trim('-').Trim()
    # Remove RES- prefix, -F fax marker
    $t = $t -replace '^RES-', '' -replace '-F$', '' -replace '\(F\)$', ''
    $t = $t.Trim().Trim('-')
    if ($t -eq '' -or $t -eq '-' -or $t -eq '—') { return $null }
    return $t
}

# ── Split each row into Office_Phone and Mobile ────────────────────────────
$split = [System.Collections.Generic.List[PSCustomObject]]::new()

foreach ($row in $data) {
    $raw = $row.Phone

    # Split on "/" to get [office_part, mobile_part]
    if ($raw -match '/') {
        $parts  = $raw -split '/', 2
        $office = $parts[0].Trim()
        $mobile = $parts[1].Trim()
    } else {
        # No slash - decide by number pattern
        # Indian mobile numbers start with 6-9 and are 10 digits
        $office = $raw.Trim()
        $mobile = ""
    }

    # Clean "—" and "-" placeholders
    if ($office -match '^[-—\s]*$') { $office = "" }
    if ($mobile -match '^[-—\s]*$') { $mobile = "" }

    # Clean individual tokens within each part
    $officeTokens = ($office -split ',') | ForEach-Object { Clean-Token $_ } | Where-Object { $_ -ne $null }
    $mobileTokens = ($mobile -split ',') | ForEach-Object { Clean-Token $_ } | Where-Object { $_ -ne $null }

    $officeStr = $officeTokens -join ', '
    $mobileStr = $mobileTokens -join ', '

    # Clean multi-email
    $emailUniq = ($row.Email -split '[,;]') | ForEach-Object { $_.Trim() } | Where-Object { $_ -match '@' } | Select-Object -Unique
    $emailStr = $emailUniq -join ', '

    $split.Add([PSCustomObject]@{
        Section      = $row.Section
        Unit         = $row.Unit
        Designation  = $row.Designation
        Office_Phone = $officeStr
        Mobile       = $mobileStr
        Email        = $emailStr
    })
}

Write-Host "Total rows: $($split.Count)"
Write-Host "Rows with office phone: $(($split | Where-Object { $_.Office_Phone -ne '' }).Count)"
Write-Host "Rows with mobile:       $(($split | Where-Object { $_.Mobile -ne '' }).Count)"
Write-Host "Rows with email:        $(($split | Where-Object { $_.Email -ne '' }).Count)"

# Export CSV
$split | Export-Csv $outCsv -NoTypeInformation -Encoding UTF8
Write-Host "`nCSV saved: $outCsv"

# ── Now regenerate Excel with split columns ────────────────────────────────
$headerBg  = 0x1F3864
$headerFg  = 0xFFFFFF
$altRowBg  = 0xDCE6F1
$tabPalette = @(
    0x1F3864, 0x375623, 0x843C0C, 0x3D3D6B, 0x5C3E6E, 0x006B5E,
    0x7B3F00, 0x004080, 0x5B2333, 0x2F5233, 0x404040, 0x614B00,
    0x004B50, 0x472D6E, 0x8B0000, 0x1A5276, 0x145A32, 0x7D3C98,
    0x0E4D92, 0x6E2C00, 0x154360, 0x512E5F, 0x1B4F72, 0x0B5345
)

$units = $split | Select-Object -ExpandProperty Unit | Sort-Object -Unique

$xl = New-Object -ComObject Excel.Application
$xl.Visible       = $false
$xl.DisplayAlerts = $false
$wb = $xl.Workbooks.Add()
$defaultSheet = $wb.Sheets.Item(1)

# ── Column definitions (now 6 cols: A-F) ──────────────────────────────────
$colHeaders = @("Section","Unit","Designation","Office Phone","Mobile","Email")
$colWidths  = @(20, 28, 45, 30, 25, 35)   # max widths

function Write-Sheet($ws, $title, $rows, $tabColor) {
    $ws.Tab.Color = $tabColor

    # Title row
    $t = $ws.Range("A1:F1")
    $t.Merge()
    $t.Value2              = $title
    $t.Font.Bold           = $true
    $t.Font.Size           = 13
    $t.Font.Color          = $headerBg
    $t.Interior.Color      = 0xF0F4F8
    $t.HorizontalAlignment = -4108
    $ws.Rows.Item(1).RowHeight = 26

    # Header row
    for ($c = 1; $c -le 6; $c++) {
        $cell = $ws.Cells.Item(2, $c)
        $cell.Value2              = $colHeaders[$c-1]
        $cell.Font.Bold           = $true
        $cell.Font.Color          = $headerFg
        $cell.Interior.Color      = $headerBg
        $cell.HorizontalAlignment = -4108
    }
    $ws.Rows.Item(2).RowHeight = 18

    # Data
    $rowNum = 3
    $alt    = $false
    foreach ($r in $rows) {
        $ws.Cells.Item($rowNum, 1).Value2 = $r.Section
        $ws.Cells.Item($rowNum, 2).Value2 = $r.Unit
        $ws.Cells.Item($rowNum, 3).Value2 = $r.Designation
        $ws.Cells.Item($rowNum, 4).Value2 = $r.Office_Phone
        $ws.Cells.Item($rowNum, 5).Value2 = $r.Mobile
        $ws.Cells.Item($rowNum, 6).Value2 = $r.Email
        if ($alt) {
            $ws.Range($ws.Cells.Item($rowNum,1), $ws.Cells.Item($rowNum,6)).Interior.Color = $altRowBg
        }
        $alt = -not $alt
        $rowNum++
    }

    # Borders
    if ($rowNum -gt 3) {
        $ws.Range($ws.Cells.Item(2,1), $ws.Cells.Item($rowNum-1,6)).Borders.LineStyle = 1
        $ws.Range($ws.Cells.Item(2,1), $ws.Cells.Item($rowNum-1,6)).Borders.Weight    = 2
    }

    # Freeze panes
    $ws.Activate()
    $xl.ActiveWindow.FreezePanes = $false
    $ws.Cells.Item(3,1).Select()
    $xl.ActiveWindow.FreezePanes = $true

    # Set column widths
    $ws.Columns.AutoFit() | Out-Null
    for ($c = 1; $c -le 6; $c++) {
        $maxW = $colWidths[$c-1]
        if ($ws.Columns.Item($c).ColumnWidth -gt $maxW) {
            $ws.Columns.Item($c).ColumnWidth = $maxW
        }
        if ($ws.Columns.Item($c).ColumnWidth -lt 8) {
            $ws.Columns.Item($c).ColumnWidth = 8
        }
    }
}

# ALL sheet
$allWs = $defaultSheet
$allWs.Name = "ALL"
Write-Sheet $allWs "KSP Contact Directory - All Units ($($split.Count) records)" $split 0x000080
Write-Host "Sheet 'ALL': $($split.Count) records"

# Unit sheets
$idx = 0
foreach ($unit in $units) {
    $rows = $split | Where-Object { $_.Unit -eq $unit }
    $ws   = $wb.Sheets.Add([System.Reflection.Missing]::Value, $wb.Sheets.Item($wb.Sheets.Count))
    $tabName = ($unit -replace '[\\\/\?\*\[\]:]', '-').Trim()
    if ($tabName.Length -gt 31) { $tabName = $tabName.Substring(0, 28) + "..." }
    $ws.Name = $tabName
    $tabColor = $tabPalette[$idx % $tabPalette.Count]
    Write-Sheet $ws "KSP - $unit" $rows $tabColor
    Write-Host ("  [{0,3}] {1}" -f $rows.Count, $tabName)
    $idx++
}

$wb.Sheets.Item("ALL").Activate()

if (Test-Path $xlsxPath) { Remove-Item $xlsxPath -Force -ErrorAction SilentlyContinue }
$wb.SaveAs($xlsxPath, 51)
$wb.Close($false)
$xl.Quit()
[System.Runtime.InteropServices.Marshal]::ReleaseComObject($xl) | Out-Null

Write-Host ""
Write-Host "DONE -> $xlsxPath"
Write-Host "47 sheets (ALL + 46 units), 6 columns: Section | Unit | Designation | Office Phone | Mobile | Email"
