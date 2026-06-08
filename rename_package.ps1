$base = "FeatherFrame\app\src\main\java\com\featherframe\app"
Get-ChildItem -Path $base -Recurse -Filter *.kt | ForEach-Object {
    $c = Get-Content $_.FullName -Raw
    $c = $c -replace 'com\.ornitrack\.raw', 'com.featherframe.app'
    $c = $c -replace 'OrniTrackApp', 'FeatherFrameApp'
    $c = $c -replace 'OrniTrack RAW', 'FeatherFrame'
    $c = $c -replace 'OrniTrackRAW', 'FeatherFrame'
    Set-Content -Path $_.FullName -Value $c
    Write-Host "  Updated: $($_.Name)" -ForegroundColor Green
}

# Also update the non-Kotlin files
$files = @(
    "FeatherFrame\settings.gradle.kts",
    "FeatherFrame\build.gradle.kts",
    "FeatherFrame\app\build.gradle.kts",
    "FeatherFrame\app\proguard-rules.pro",
    "FeatherFrame\app\src\main\AndroidManifest.xml",
    "FeatherFrame\app\src\main\res\values\strings.xml",
    "FeatherFrame\app\src\main\res\values\themes.xml"
)
foreach ($f in $files) {
    if (Test-Path $f) {
        $c = Get-Content $f -Raw
        $c = $c -replace 'com\.ornitrack\.raw', 'com.featherframe.app'
        $c = $c -replace 'OrniTrackApp', 'FeatherFrameApp'
        $c = $c -replace 'OrniTrack RAW', 'FeatherFrame'
        $c = $c -replace 'OrniTrackRAW', 'FeatherFrame'
        $c = $c -replace 'Theme\.OrniTrackRAW', 'Theme.FeatherFrame'
        Set-Content -Path $f -Value $c
        Write-Host "  Updated: $(Split-Path $f -Leaf)" -ForegroundColor Yellow
    }
}

Write-Host "`nAll FeatherFrame files updated!" -ForegroundColor Cyan