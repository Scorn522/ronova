param(
    [Parameter(Mandatory = $true)][string]$Zig,
    [Parameter(Mandatory = $true)][string]$JdkInclude
)

$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$Output = Join-Path $ProjectRoot 'src/main/resources/native/windows-x86_64/ronova_root.dll'
$BuildDirectory = Join-Path $PSScriptRoot 'build/windows-x86_64-root'
$BuiltDll = Join-Path $BuildDirectory 'ronova_root.dll'
New-Item -ItemType Directory -Force (Split-Path -Parent $Output) | Out-Null
New-Item -ItemType Directory -Force $BuildDirectory | Out-Null

& $Zig c++ -target x86_64-windows-gnu -shared -O2 -s -std=c++17 `
    "-I$JdkInclude" "-I$(Join-Path $JdkInclude 'win32')" `
    (Join-Path $PSScriptRoot 'src/native_root.cpp') `
    (Join-Path $PSScriptRoot 'src/native_root_jni.cpp') -o $BuiltDll
if ($LASTEXITCODE -ne 0) { throw "Zig Native root build failed: $LASTEXITCODE" }

Copy-Item -Force $BuiltDll $Output
Write-Host "Built $Output"
