$excel = New-Object -ComObject Excel.Application
$excel.Visible = $false
$excel.DisplayAlerts = $false
$file = "C:\Users\ravip\AndroidStudioProjects\PoliceMobileDirectory\KSP_Contacts_Final_Directory_V3.xlsx"
$workbook = $excel.Workbooks.Open($file)

foreach ($sheet in $workbook.Worksheets) {
    $sheet.Range("A1", "Z1").Font.Bold = $true
    $sheet.Activate()
    $sheet.Cells.Item(2, 1).Select()
    $excel.ActiveWindow.FreezePanes = $true
    $sheet.Columns.AutoFit()
}

# Force the Master sheet to be the active tab on open
$masterSheet = $workbook.Sheets.Item("MASTER_MERGED_FINAL")
$masterSheet.Activate()
$masterSheet.Cells.Item(1, 1).Select()

$workbook.Save()
$workbook.Close()
$excel.Quit()
[System.Runtime.Interopservices.Marshal]::ReleaseComObject($excel)
