
# Export_Final_Excel.ps1
# Reads KSP_Officers_App.csv (rank+station already split) and writes
# a 47-sheet Excel with columns:
# Section | Unit | District | Rank | Station | Office1 | Office2 | Mobile1 | Mobile2 | Email

$inCsv    = "c:\Users\ravip\AndroidStudioProjects\PoliceMobileDirectory\KSP_Officers_App.csv"
$xlsxPath = "c:\Users\ravip\AndroidStudioProjects\PoliceMobileDirectory\KSP_Contacts_UnitWise.xlsx"

$data  = Import-Csv $inCsv -Encoding UTF8
$units = $data | Select-Object -ExpandProperty unit | Sort-Object -Unique

Write-Host "Records: $($data.Count)   Units: $($units.Count)"

# ── Colours ───────────────────────────────────────────────────────────────
$navyBg    = 0x1F3864
$whiteFg   = 0xFFFFFF
$altBg     = 0xDCE6F1
$rankHdr   = 0x375623   # dark green  - Rank col
$stationHdr= 0x843C0C   # dark orange - Station col
$officeHdr = 0x2E75B6   # blue        - Office cols
$mobileHdr = 0xC55A11   # orange      - Mobile cols

$tabPalette = @(
    0x1F3864,0x375623,0x843C0C,0x3D3D6B,0x5C3E6E,0x006B5E,
    0x7B3F00,0x004080,0x5B2333,0x2F5233,0x404040,0x614B00,
    0x004B50,0x472D6E,0x8B0000,0x1A5276,0x145A32,0x7D3C98,
    0x0E4D92,0x6E2C00,0x154360,0x512E5F,0x1B4F72,0x0B5345
)

# 12 columns: A-L
# A=Section B=Unit C=Range D=District E=Name F=Rank G=Station H=Office1 I=Office2 J=Mobile1 K=Mobile2 L=Email
$colHeaders = @("Section","Unit","Range","District","Name","Rank","Station","Office 1","Office 2","Mobile 1","Mobile 2","Email")
$colWidths  = @(22, 28, 22, 18, 24, 18, 42, 18, 18, 16, 16, 34)
$nCols      = 12
$lastCol    = "L"

# Field names matching CSV headers
[string[]]$fields = @("office","unit","range","district","name","rank","station","landline","landline2","mobile","mobile2","email")

$xl = New-Object -ComObject Excel.Application
$xl.Visible       = $false
$xl.DisplayAlerts = $false
$wb = $xl.Workbooks.Add()

function Write-Sheet($ws, $title, $rows, $tabColor) {
    $ws.Tab.Color = $tabColor

    # Row 1 — merged title
    $t = $ws.Range("A1:${lastCol}1")
    $t.Merge()
    $t.Value2              = $title
    $t.Font.Bold           = $true
    $t.Font.Size           = 13
    $t.Font.Color          = $navyBg
    $t.Interior.Color      = 0xF0F4F8
    $t.HorizontalAlignment = -4108
    $ws.Rows.Item(1).RowHeight = 26

    # Row 2 — headers with colour coding
    for ($c = 1; $c -le $nCols; $c++) {
        $cell = $ws.Cells.Item(2, $c)
        $cell.Value2  = $colHeaders[$c-1]
        $cell.Font.Bold  = $true
        $cell.Font.Color = $whiteFg
        $cell.Interior.Color = switch ($c) {
            6       { $rankHdr }     # Rank
            7       { $stationHdr }  # Station
            { $_ -in 8,9 } { $officeHdr }  # Office 1/2
            { $_ -in 10,11 } { $mobileHdr }  # Mobile 1/2
            default { $navyBg }
        }
        $cell.HorizontalAlignment = -4108
    }
    $ws.Rows.Item(2).RowHeight = 20

    # Data rows
    $rowNum = 3
    $alt    = $false
    foreach ($r in $rows) {
        for ($c = 1; $c -le $nCols; $c++) {
            $ws.Cells.Item($rowNum, $c).Value2 = [string]($r.($fields[$c-1]))
        }
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
Write-Sheet $ws0 "KSP Contact Directory - All Units ($($data.Count) records)" $data 0x000080
Write-Host "Sheet 'ALL': $($data.Count) records"

# Unit sheets
$idx = 0
foreach ($unit in $units) {
    $rows    = @($data | Where-Object { $_.unit -eq $unit })
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
Write-Host "Columns: Section | Unit | Range | District | Name | Rank | Station | Office1 | Office2 | Mobile1 | Mobile2 | Email"
