
# Split_Wide_Format.ps1  (v2 - fixed)
# ONE ROW per person, separate columns:  Office1-4 | Mobile1-3
# Input:  KSP_Contacts_Split.csv
# Output: KSP_Contacts_Final.csv  +  KSP_Contacts_UnitWise.xlsx

$inCsv    = "c:\Users\ravip\AndroidStudioProjects\PoliceMobileDirectory\KSP_Contacts_Split.csv"
$outCsv   = "c:\Users\ravip\AndroidStudioProjects\PoliceMobileDirectory\KSP_Contacts_Final.csv"
$xlsxPath = "c:\Users\ravip\AndroidStudioProjects\PoliceMobileDirectory\KSP_Contacts_UnitWise.xlsx"

$maxO = 4   # max office numbers per row
$maxM = 3   # max mobile numbers per row

function Clean-Num($t) {
    $t = $t.Trim().Trim('-').Trim([char]0x2014).Trim()
    $t = $t -replace '^RES-','' -replace '-F$','' -replace '\(F\)$','' -replace '^\(F\)',''
    $t = $t.Trim().Trim('-')
    if ($t -eq '' -or $t -eq '-' -or $t.Length -lt 4) { return $null }
    return $t
}

# ── Build wide CSV ────────────────────────────────────────────────────────
$data   = Import-Csv $inCsv -Encoding UTF8
$result = [System.Collections.Generic.List[PSCustomObject]]::new()

# Helper: safely get nth element from array, or empty string
function Get-Nth($arr, $n) {
    if ($arr -eq $null -or $arr.Count -lt $n) { return "" }
    $v = $arr[$n-1]
    if ($v -eq $null) { return "" }
    return [string]$v
}

foreach ($row in $data) {
    $offices = @(($row.Office_Phone -split ',') | ForEach-Object { Clean-Num $_ } | Where-Object { $_ })
    $mobiles = @(($row.Mobile       -split ',') | ForEach-Object { Clean-Num $_ } | Where-Object { $_ })

    $result.Add([PSCustomObject]@{
        Section     = [string]$row.Section
        Unit        = [string]$row.Unit
        Designation = [string]$row.Designation
        Office1     = Get-Nth $offices 1
        Office2     = Get-Nth $offices 2
        Office3     = Get-Nth $offices 3
        Office4     = Get-Nth $offices 4
        Mobile1     = Get-Nth $mobiles 1
        Mobile2     = Get-Nth $mobiles 2
        Mobile3     = Get-Nth $mobiles 3
        Email       = [string]$row.Email
    })
}

Write-Host "Records: $($result.Count)"
$result | Select-Object -First 5 | Format-Table Designation, Office1, Office2, Office3, Office4, Mobile1, Mobile2 -AutoSize

$result | Export-Csv $outCsv -NoTypeInformation -Encoding UTF8
Write-Host "CSV saved: $outCsv"

# ── Excel ─────────────────────────────────────────────────────────────────
$navyBg    = 0x1F3864
$whiteFg   = 0xFFFFFF
$altBg     = 0xDCE6F1
$officeHdr = 0x2E75B6   # blue  for office header cells
$mobileHdr = 0xC55A11   # orange for mobile header cells

$tabPalette = @(
    0x1F3864,0x375623,0x843C0C,0x3D3D6B,0x5C3E6E,0x006B5E,
    0x7B3F00,0x004080,0x5B2333,0x2F5233,0x404040,0x614B00,
    0x004B50,0x472D6E,0x8B0000,0x1A5276,0x145A32,0x7D3C98,
    0x0E4D92,0x6E2C00,0x154360,0x512E5F,0x1B4F72,0x0B5345
)

# 11 columns: A=Section B=Unit C=Designation D-G=Office1-4 H-J=Mobile1-3 K=Email
$colNames  = "Section","Unit","Designation","Office 1","Office 2","Office 3","Office 4","Mobile 1","Mobile 2","Mobile 3","Email"
$colWidths = 18,26,44,18,18,18,18,16,16,16,34
$nCols     = 11
$lastColLetter = "K"

$units = $result | Select-Object -ExpandProperty Unit | Sort-Object -Unique

$xl = New-Object -ComObject Excel.Application
$xl.Visible       = $false
$xl.DisplayAlerts = $false
$wb = $xl.Workbooks.Add()

function Write-Sheet($ws, $title, $rows, $tabColor) {
    $ws.Tab.Color = $tabColor

    # Row 1 — merged title
    $t = $ws.Range("A1:${lastColLetter}1")
    $t.Merge()
    $t.Value2              = $title
    $t.Font.Bold           = $true
    $t.Font.Size           = 13
    $t.Font.Color          = $navyBg
    $t.Interior.Color      = 0xF0F4F8
    $t.HorizontalAlignment = -4108
    $ws.Rows.Item(1).RowHeight = 26

    # Row 2 — column headers
    for ($c = 1; $c -le $nCols; $c++) {
        $cell = $ws.Cells.Item(2, $c)
        $cell.Value2  = $colNames[$c-1]
        $cell.Font.Bold  = $true
        $cell.Font.Color = $whiteFg
        $cell.Interior.Color = switch ($c) {
            { $_ -ge 4 -and $_ -le 7 } { $officeHdr }   # Office cols
            { $_ -ge 8 -and $_ -le 10} { $mobileHdr }   # Mobile cols
            default                     { $navyBg }
        }
        $cell.HorizontalAlignment = -4108
    }
    $ws.Rows.Item(2).RowHeight = 20

    # Data rows — write cell by cell using explicit property names
    $rowNum = 3
    $alt    = $false
    foreach ($r in $rows) {
        $ws.Cells.Item($rowNum,  1).Value2 = $r.Section
        $ws.Cells.Item($rowNum,  2).Value2 = $r.Unit
        $ws.Cells.Item($rowNum,  3).Value2 = $r.Designation
        $ws.Cells.Item($rowNum,  4).Value2 = $r.Office1
        $ws.Cells.Item($rowNum,  5).Value2 = $r.Office2
        $ws.Cells.Item($rowNum,  6).Value2 = $r.Office3
        $ws.Cells.Item($rowNum,  7).Value2 = $r.Office4
        $ws.Cells.Item($rowNum,  8).Value2 = $r.Mobile1
        $ws.Cells.Item($rowNum,  9).Value2 = $r.Mobile2
        $ws.Cells.Item($rowNum, 10).Value2 = $r.Mobile3
        $ws.Cells.Item($rowNum, 11).Value2 = $r.Email
        if ($alt) {
            $ws.Range($ws.Cells.Item($rowNum,1), $ws.Cells.Item($rowNum,$nCols)).Interior.Color = $altBg
        }
        $alt = -not $alt
        $rowNum++
    }

    # Borders
    if ($rowNum -gt 3) {
        $dr = $ws.Range($ws.Cells.Item(2,1), $ws.Cells.Item($rowNum-1,$nCols))
        $dr.Borders.LineStyle = 1
        $dr.Borders.Weight    = 2
    }

    # Freeze panes at row 3
    $ws.Activate()
    $xl.ActiveWindow.FreezePanes = $false
    $ws.Cells.Item(3,1).Select()
    $xl.ActiveWindow.FreezePanes = $true

    # Column widths
    $ws.Columns.AutoFit() | Out-Null
    for ($c = 1; $c -le $nCols; $c++) {
        $max = $colWidths[$c-1]
        if ($ws.Columns.Item($c).ColumnWidth -gt $max) { $ws.Columns.Item($c).ColumnWidth = $max }
        if ($ws.Columns.Item($c).ColumnWidth -lt 6)    { $ws.Columns.Item($c).ColumnWidth = 6 }
    }
}

# ALL sheet
$ws0 = $wb.Sheets.Item(1)
$ws0.Name = "ALL"
Write-Sheet $ws0 "KSP Contact Directory - All Units ($($result.Count) records)" $result 0x000080
Write-Host "Sheet 'ALL': $($result.Count) records"

# One sheet per unit
$idx = 0
foreach ($unit in $units) {
    $rows    = $result | Where-Object { $_.Unit -eq $unit }
    $ws      = $wb.Sheets.Add([System.Reflection.Missing]::Value, $wb.Sheets.Item($wb.Sheets.Count))
    $tabName = ($unit -replace '[\\\/\?\*\[\]:]','-').Trim()
    if ($tabName.Length -gt 31) { $tabName = $tabName.Substring(0,28)+"..." }
    $ws.Name = $tabName
    Write-Sheet $ws "KSP - $unit" $rows ($tabPalette[$idx % $tabPalette.Count])
    Write-Host ("  [{0,3}] {1}" -f $rows.Count, $tabName)
    $idx++
}

$wb.Sheets.Item("ALL").Activate()
if (Test-Path $xlsxPath) { Remove-Item $xlsxPath -Force -ErrorAction SilentlyContinue }
$wb.SaveAs($xlsxPath, 51)
$wb.Close($false)
$xl.Quit()
[System.Runtime.InteropServices.Marshal]::ReleaseComObject($xl) | Out-Null

$f = Get-Item $xlsxPath
Write-Host ""
Write-Host "DONE -> $($f.Name)  [$([Math]::Round($f.Length/1KB,1)) KB]"
Write-Host "Columns: Section | Unit | Designation | Office1 | Office2 | Office3 | Office4 | Mobile1 | Mobile2 | Mobile3 | Email"
