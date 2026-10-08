param(
    [Parameter(Mandatory = $true)][string]$Zig,
    [Parameter(Mandatory = $true)][string]$JdkInclude
)

$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$Output = Join-Path $ProjectRoot 'src/main/resources/native/windows-x86_64/ronova.dll'
$BuildDirectory = Join-Path $PSScriptRoot 'build/windows-x86_64'
$BuiltDll = Join-Path $BuildDirectory 'ronova.dll'
New-Item -ItemType Directory -Force (Split-Path -Parent $Output) | Out-Null
New-Item -ItemType Directory -Force $BuildDirectory | Out-Null

& $Zig c++ -target x86_64-windows-gnu -shared -O2 -s -std=c++17 `
    "-I$JdkInclude" "-I$(Join-Path $JdkInclude 'win32')" `
    (Join-Path $PSScriptRoot 'src/ronova_native.cpp') -lbcrypt -o $BuiltDll
if ($LASTEXITCODE -ne 0) { throw "Zig native build failed: $LASTEXITCODE" }

& python (Join-Path $PSScriptRoot 'normalize_pe_timestamp.py') $BuiltDll
if ($LASTEXITCODE -ne 0) { throw "Native timestamp normalization failed: $LASTEXITCODE" }

Copy-Item -Force $BuiltDll $Output

Write-Host "Built $Output"
