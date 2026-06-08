# FeatherFrame Rename Script - Fixed paths

$sourceBase = "OrniTrackRAW\app\src\main"
$destBase = "FeatherFrame\app\src\main"

# Create all directories for new package structure
$newPackage = "java\com\featherframe\app"
$subDirs = @(
    "data\database",
    "data\drive",
    "data\processing",
    "domain\ai",
    "domain\auth",
    "domain\camera",
    "domain\location",
    "ui\screens",
    "ui\workers"
)

foreach ($sub in $subDirs) {
    New-Item -ItemType Directory -Force -Path "$destBase\$newPackage\$sub" | Out-Null
}
New-Item -ItemType Directory -Force -Path "$destBase\res\values" | Out-Null

# Copy and rename all .kt files
$oldPackage = "java\com\ornitrack\raw"
Get-ChildItem -Path "$sourceBase\$oldPackage" -Recurse -Filter *.kt | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    $content = $content -replace 'com\.ornitrack\.raw', 'com.featherframe.app'
    $content = $content -replace 'OrniTrackApp', 'FeatherFrameApp'
    $content = $content -replace 'OrniTrack RAW', 'FeatherFrame'
    $content = $content -replace 'OrniTrackRAW', 'FeatherFrame'

    # Build new path: replace old package path with new one, keep filename
    $relPath = $_.FullName.Substring(("$sourceBase\$oldPackage").Length + 1)
    $fileName = $relPath -replace '.*\\', ''
    $relDir = $relPath.Substring(0, $relPath.Length - $fileName.Length).TrimEnd('\')
    
    $newDir = "$destBase\$newPackage\$relDir"
    if (!(Test-Path $newDir)) { New-Item -ItemType Directory -Force -Path $newDir | Out-Null }
    
    # Rename OrniTrackApp.kt to FeatherFrameApp.kt
    $newFileName = $fileName -replace 'OrniTrackApp', 'FeatherFrameApp'
    $newPath = "$newDir\$newFileName"

    Set-Content -Path $newPath -Value $content
    Write-Host "  $newFileName -> FeatherFrame"
}

# AndroidManifest.xml
$manifest = Get-Content "$sourceBase\AndroidManifest.xml" -Raw
$manifest = $manifest -replace 'com\.ornitrack\.raw', 'com.featherframe.app'
$manifest = $manifest -replace 'OrniTrack RAW', 'FeatherFrame'
$manifest = $manifest -replace '\.OrniTrackApp', '.FeatherFrameApp'
$manifest = $manifest -replace 'ornitrack_credentials', 'featherframe_credentials'
Set-Content -Path "$destBase\AndroidManifest.xml" -Value $manifest
Write-Host "  AndroidManifest.xml -> FeatherFrame"

# strings.xml
$strings = Get-Content "$sourceBase\res\values\strings.xml" -Raw
$strings = $strings -replace 'OrniTrack RAW', 'FeatherFrame'
Set-Content -Path "$destBase\res\values\strings.xml" -Value $strings
Write-Host "  strings.xml -> FeatherFrame"

# themes.xml
Copy-Item "$sourceBase\res\values\themes.xml" "$destBase\res\values\themes.xml" -Force
$themes = Get-Content "$destBase\res\values\themes.xml" -Raw
$themes = $themes -replace 'OrniTrackRAW', 'FeatherFrame'
Set-Content "$destBase\res\values\themes.xml" -Value $themes
Write-Host "  themes.xml -> FeatherFrame"

# typography.xml
Copy-Item "$sourceBase\res\values\typography.xml" "$destBase\res\values\typography.xml" -Force
Write-Host "  typography.xml -> copied"

# proguard-rules.pro
Copy-Item "OrniTrackRAW\app\proguard-rules.pro" "FeatherFrame\app\proguard-rules.pro" -Force
$proguard = Get-Content "FeatherFrame\app\proguard-rules.pro" -Raw
$proguard = $proguard -replace 'com\.ornitrack\.raw', 'com.featherframe.app'
Set-Content "FeatherFrame\app\proguard-rules.pro" -Value $proguard
Write-Host "  proguard-rules.pro -> FeatherFrame"

Write-Host "`n✅ FeatherFrame rename complete!"