$src = "OrniTrackRAW\app\src\main\java\com\ornitrack\raw"
$dst = "FeatherFrame\app\src\main\java\com\featherframe\app"

Get-ChildItem -Path $src -Recurse -Filter *.kt | ForEach-Object {
    $rel = $_.FullName.Substring($src.Length + 1)
    $newName = $rel -replace 'OrniTrackApp', 'FeatherFrameApp'
    $newPath = Join-Path $dst $newName
    $dir = Split-Path $newPath -Parent
    if (!(Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    
    $content = Get-Content $_.FullName -Raw
    $content = $content -replace 'com\.ornitrack\.raw', 'com.featherframe.app'
    $content = $content -replace 'OrniTrackApp', 'FeatherFrameApp'
    $content = $content -replace 'OrniTrack RAW', 'FeatherFrame'
    $content = $content -replace 'OrniTrackRAW', 'FeatherFrame'
    
    Set-Content -Path $newPath -Value $content
    Write-Host ("  " + $_.Name)
}

Write-Host "Done! All files copied."