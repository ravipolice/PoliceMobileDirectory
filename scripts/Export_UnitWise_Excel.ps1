
# Export_UnitWise_Excel.ps1
# Creates one Excel sheet per Unit from KSP_Contacts_Master_Clean.csv
# Source has 46 units ~ 3800 records

$csvPath  = "c:\Users\ravip\AndroidStudioProjects\PoliceMobileDirectory\KSP_Contacts_Master_Clean.csv"
$xlsxPath = "c:\Users\ravip\AndroidStudioProjects\PoliceMobileDirectory\KSP_Contacts_UnitWise_Final.xlsx"

# ── Colour palette ─────────────────────────────────────────────────────────
$headerBg = 0x1F3864   # dark navy
$headerFg = 0xFFFFFF   # white
$altRowBg  = 0xDCE6F1  # light blue-grey

# Tab colours cycling through a set of professional colours
$tabPalette = @(
    0x1F3864, 0x375623, 0x843C0C, 0x3D3D6B, 0x5C3E6E, 0x006B5E,
    0x7B3F00, 0x004080, 0x5B2333, 0x2F5233, 0x404040, 0x614B00,
    0x004B50, 0x472D6E, 0x8B0000, 0x1A5276, 0x145A32, 0x7D3C98,
    0x0E4D92, 0x6E2C00, 0x154360, 0x512E5F, 0x1B4F72, 0x0B5345
)

# ── Load CSV ──────────────────────────────────────────────────────────────
$data  = Import-Csv $csvPath -Encoding UTF8
$units = $data | Select-Object -ExpandProperty Unit | Sort-Object -Unique

Write-Host "Units: $($units.Count)   Records: $($data.Count)"

# ── Start Excel ───────────────────────────────────────────────────────────
$xl = New-Object -ComObject Excel.Application
$xl.Visible       = $false
$xl.DisplayAlerts = $false
$wb = $xl.Workbooks.Add()

$defaultSheet = $wb.Sheets.Item(1)

# ── Helper: write one sheet ───────────────────────────────────────────────
function Write-Sheet($ws, $sheetTitle, $rows, $tabColor) {
    $ws.Tab.Color = $tabColor

    # Title row (row 1, merged A:E)
    $t = $ws.Range("A1:E1")
    $t.Merge()
    $t.Value2              = $sheetTitle
    $t.Font.Bold           = $true
    $t.Font.Size           = 13
    $t.Font.Color          = $headerBg
    $t.Interior.Color      = 0xF0F4F8   # very light blue
    $t.HorizontalAlignment = -4108      # xlCenter
    $ws.Rows.Item(1).RowHeight = 26

    # Header row (row 2)
    $cols = @("Section","Unit","Designation","Phone","Email")
    for ($c = 1; $c -le 5; $c++) {
        $cell = $ws.Cells.Item(2, $c)
        $cell.Value2              = $cols[$c-1]
        $cell.Font.Bold           = $true
        $cell.Font.Color          = $headerFg
        $cell.Interior.Color      = $headerBg
        $cell.HorizontalAlignment = -4108
    }
    $ws.Rows.Item(2).RowHeight = 18

    # Data rows
    $rowNum = 3
    $alt    = $false
    foreach ($r in $rows) {
        $ws.Cells.Item($rowNum, 1).Value2 = $r.Section
        $ws.Cells.Item($rowNum, 2).Value2 = $r.Unit
        $ws.Cells.Item($rowNum, 3).Value2 = $r.Designation
        $ws.Cells.Item($rowNum, 4).Value2 = $r.Phone
        $ws.Cells.Item($rowNum, 5).Value2 = $r.Email
        if ($alt) {
            $ws.Range($ws.Cells.Item($rowNum,1), $ws.Cells.Item($rowNum,5)).Interior.Color = $altRowBg
        }
        $alt = -not $alt
        $rowNum++
    }

    # Borders
    if ($rowNum -gt 3) {
        $dr = $ws.Range($ws.Cells.Item(2,1), $ws.Cells.Item($rowNum-1, 5))
        $dr.Borders.LineStyle = 1
        $dr.Borders.Weight    = 2
    }

    # Freeze panes below header
    $ws.Activate()
    $xl.ActiveWindow.FreezePanes = $false
    $ws.Cells.Item(3,1).Select()
    $xl.ActiveWindow.FreezePanes = $true

    # Auto-fit columns (cap at 55)
    $ws.Columns.AutoFit() | Out-Null
    for ($c = 1; $c -le 5; $c++) {
        if ($ws.Columns.Item($c).ColumnWidth -gt 55) {
            $ws.Columns.Item($c).ColumnWidth = 55
        }
        if ($ws.Columns.Item($c).ColumnWidth -lt 10) {
            $ws.Columns.Item($c).ColumnWidth = 10
        }
    }

    return $rowNum - 3   # records written
}

# ── Create ALL sheet first (index 0) ──────────────────────────────────────
$allWs = $defaultSheet
$allWs.Name = "ALL"
$allWs.Tab.Color = 0x000080
$written = Write-Sheet $allWs "KSP Contact Directory - All Units ($($data.Count) records)" $data 0x000080
Write-Host "Sheet 'ALL': $written records"

# ── Create one sheet per unit ─────────────────────────────────────────────
$idx = 0
foreach ($unit in $units) {
    $rows = $data | Where-Object { $_.Unit -eq $unit }

    # Add new sheet at end
    $ws = $wb.Sheets.Add([System.Reflection.Missing]::Value, $wb.Sheets.Item($wb.Sheets.Count))

    # Tab name: max 31 chars, no special chars
    $tabName = ($unit -replace '[\\\/\?\*\[\]:]', '-').Trim()
    if ($tabName.Length -gt 31) { $tabName = $tabName.Substring(0, 28) + "..." }
    $ws.Name = $tabName

    $tabColor = $tabPalette[$idx % $tabPalette.Count]
    $written  = Write-Sheet $ws "KSP - $unit" $rows $tabColor

    Write-Host ("  [{0,3}] Sheet '{1}'" -f $written, $tabName)
    $idx++
}

# ── Activate ALL sheet ────────────────────────────────────────────────────
$wb.Sheets.Item("ALL").Activate()

# ── Save ──────────────────────────────────────────────────────────────────
if (Test-Path $xlsxPath) { Remove-Item $xlsxPath -Force }
$wb.SaveAs($xlsxPath, 51)   # 51 = xlOpenXMLWorkbook (.xlsx)
$wb.Close($false)
$xl.Quit()
[System.Runtime.InteropServices.Marshal]::ReleaseComObject($xl) | Out-Null

Write-Host ""
Write-Host "DONE -> $xlsxPath"
Write-Host "Sheets: 1 (ALL) + $($units.Count) unit sheets = $($units.Count + 1) total"
