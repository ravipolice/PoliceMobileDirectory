
# Split_One_Per_Row.ps1
# Expands every phone number into its own row
# Input:  KSP_Contacts_Split.csv  (Office_Phone / Mobile may have comma-separated lists)
# Output: KSP_Contacts_Final.csv  (one phone number per row)
#         KSP_Contacts_UnitWise.xlsx (47 sheets, updated)

$inCsv   = "c:\Users\ravip\AndroidStudioProjects\PoliceMobileDirectory\KSP_Contacts_Split.csv"
$outCsv  = "c:\Users\ravip\AndroidStudioProjects\PoliceMobileDirectory\KSP_Contacts_Final.csv"
$xlsxPath= "c:\Users\ravip\AndroidStudioProjects\PoliceMobileDirectory\KSP_Contacts_UnitWise.xlsx"

$data = Import-Csv $inCsv -Encoding UTF8

# ── Helper: clean a single number token ────────────────────────────────────
function Clean-Num($t) {
    $t = $t.Trim().Trim('-').Trim('—').Trim()
    $t = $t -replace '^RES-',''-replace '-F$',''-replace '\(F\)$',''-replace '^-$',''
    $t = $t.Trim()
    if ($t -eq '' -or $t.Length -lt 4) { return $null }
    return $t
}

# ── Expand rows ─────────────────────────────────────────────────────────────
$result = [System.Collections.Generic.List[PSCustomObject]]::new()

foreach ($row in $data) {

    # Split office phones and mobiles on comma
    $offices = ($row.Office_Phone -split ',') | ForEach-Object { Clean-Num $_ } | Where-Object { $_ }
    $mobiles = ($row.Mobile       -split ',') | ForEach-Object { Clean-Num $_ } | Where-Object { $_ }

    # If both empty, still keep one blank row so we don't lose the contact
    if ($offices.Count -eq 0 -and $mobiles.Count -eq 0) {
        $result.Add([PSCustomObject]@{
            Section     = $row.Section
            Unit        = $row.Unit
            Designation = $row.Designation
            Phone_Type  = ""
            Phone       = ""
            Email       = $row.Email
        })
        continue
    }

    foreach ($num in $offices) {
        $result.Add([PSCustomObject]@{
            Section     = $row.Section
            Unit        = $row.Unit
            Designation = $row.Designation
            Phone_Type  = "Office"
            Phone       = $num
            Email       = $row.Email
        })
    }

    foreach ($num in $mobiles) {
        $result.Add([PSCustomObject]@{
            Section     = $row.Section
            Unit        = $row.Unit
            Designation = $row.Designation
            Phone_Type  = "Mobile"
            Phone       = $num
            Email       = $row.Email
        })
    }
}

Write-Host "Original rows : $($data.Count)"
Write-Host "Expanded rows : $($result.Count)"
Write-Host "  Office rows : $(($result | Where-Object { $_.Phone_Type -eq 'Office' }).Count)"
Write-Host "  Mobile rows : $(($result | Where-Object { $_.Phone_Type -eq 'Mobile' }).Count)"

$result | Export-Csv $outCsv -NoTypeInformation -Encoding UTF8
Write-Host "`nCSV saved: $outCsv"

# ── Verify sample ────────────────────────────────────────────────────────────
Write-Host "`nSample (first 8 rows):"
$result | Select-Object -First 8 | Format-Table Designation, Phone_Type, Phone, Email -AutoSize

# ── Rebuild Excel ─────────────────────────────────────────────────────────────
$headerBg  = 0x1F3864
$headerFg  = 0xFFFFFF
$altRowBg  = 0xDCE6F1
$officeCol = 0xE8F4FD   # light blue for office rows
$mobileCol = 0xFFF3CD   # light yellow for mobile rows

$tabPalette = @(
    0x1F3864, 0x375623, 0x843C0C, 0x3D3D6B, 0x5C3E6E, 0x006B5E,
    0x7B3F00, 0x004080, 0x5B2333, 0x2F5233, 0x404040, 0x614B00,
    0x004B50, 0x472D6E, 0x8B0000, 0x1A5276, 0x145A32, 0x7D3C98,
    0x0E4D92, 0x6E2C00, 0x154360, 0x512E5F, 0x1B4F72, 0x0B5345
)

$units = $result | Select-Object -ExpandProperty Unit | Sort-Object -Unique

$xl = New-Object -ComObject Excel.Application
$xl.Visible       = $false
$xl.DisplayAlerts = $false
$wb = $xl.Workbooks.Add()
$defaultSheet = $wb.Sheets.Item(1)

# Column headers
$colHeaders = @("Section","Unit","Designation","Type","Phone","Email")
$colWidths  = @(18, 26, 45, 8, 22, 35)

function Write-Sheet($ws, $title, $rows, $tabColor) {
    $ws.Tab.Color = $tabColor

    # Title
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
    foreach ($r in $rows) {
        $ws.Cells.Item($rowNum, 1).Value2 = $r.Section
        $ws.Cells.Item($rowNum, 2).Value2 = $r.Unit
        $ws.Cells.Item($rowNum, 3).Value2 = $r.Designation
        $ws.Cells.Item($rowNum, 4).Value2 = $r.Phone_Type
        $ws.Cells.Item($rowNum, 5).Value2 = $r.Phone
        $ws.Cells.Item($rowNum, 6).Value2 = $r.Email

        # Colour by phone type
        $bg = if ($r.Phone_Type -eq "Mobile") { $mobileCol } else { $officeCol }
        $ws.Range($ws.Cells.Item($rowNum,1), $ws.Cells.Item($rowNum,6)).Interior.Color = $bg

        $rowNum++
    }

    # Borders
    if ($rowNum -gt 3) {
        $dr = $ws.Range($ws.Cells.Item(2,1), $ws.Cells.Item($rowNum-1,6))
        $dr.Borders.LineStyle = 1
        $dr.Borders.Weight    = 2
    }

    # Freeze panes
    $ws.Activate()
    $xl.ActiveWindow.FreezePanes = $false
    $ws.Cells.Item(3,1).Select()
    $xl.ActiveWindow.FreezePanes = $true

    # Column widths
    $ws.Columns.AutoFit() | Out-Null
    for ($c = 1; $c -le 6; $c++) {
        $max = $colWidths[$c-1]
        if ($ws.Columns.Item($c).ColumnWidth -gt $max) { $ws.Columns.Item($c).ColumnWidth = $max }
        if ($ws.Columns.Item($c).ColumnWidth -lt 6)    { $ws.Columns.Item($c).ColumnWidth = 6 }
    }
}

# ALL sheet
$allWs = $defaultSheet
$allWs.Name = "ALL"
Write-Sheet $allWs "KSP Contact Directory - All Units ($($result.Count) rows)" $result 0x000080
Write-Host "Sheet 'ALL': $($result.Count) rows"

# Unit sheets
$idx = 0
foreach ($unit in $units) {
    $rows     = $result | Where-Object { $_.Unit -eq $unit }
    $ws       = $wb.Sheets.Add([System.Reflection.Missing]::Value, $wb.Sheets.Item($wb.Sheets.Count))
    $tabName  = ($unit -replace '[\\\/\?\*\[\]:]','-').Trim()
    if ($tabName.Length -gt 31) { $tabName = $tabName.Substring(0,28) + "..." }
    $ws.Name  = $tabName
    Write-Sheet $ws "KSP - $unit" $rows ($tabPalette[$idx % $tabPalette.Count])
    Write-Host ("  [{0,4}] {1}" -f $rows.Count, $tabName)
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
