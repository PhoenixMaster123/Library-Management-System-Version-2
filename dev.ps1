<#
.SYNOPSIS
    Starts the library stack, each service in its own window.

.DESCRIPTION
    The backend and the frontend are all you need; Notification-Service and Analytics-Service are
    optional and the library degrades rather than fails without them, so they are opt-in here too.

    Each service gets its own PowerShell window rather than being backgrounded into this one, so
    its logs stay readable and Ctrl+C stops the one you meant.

.PARAMETER All
    Also start Notification-Service (needs MySQL) and Analytics-Service (needs Kafka on 9094).

.PARAMETER Notifications
    Also start Notification-Service.

.PARAMETER Analytics
    Also start Analytics-Service.

.PARAMETER Seed
    Stock the catalogue from Open Library instead of the bundled JSON fixture. Slower, needs the
    network, and is what a non-dev run does.

.PARAMETER Stop
    Stop whatever is listening on the stack's ports. Useful because a killed Maven wrapper leaves
    its Java child running, which then holds the port.

.EXAMPLE
    .\dev.ps1
    Backend on :9092 and frontend on :5173.

.EXAMPLE
    .\dev.ps1 -All
    Everything, including the two optional services.

.EXAMPLE
    .\dev.ps1 -Stop
    Free every port the stack uses.
#>
[CmdletBinding()]
param(
    [switch]$All,
    [switch]$Notifications,
    [switch]$Analytics,
    [switch]$Seed,
    [switch]$Stop
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot

# Kafka's 9094 is deliberately absent: this script does not run a broker, and stopping someone
# else's is not its business.
$stackPorts = [ordered]@{
    '9092' = 'Library backend'
    '9093' = 'Notification-Service'
    '9095' = 'Analytics-Service'
    '5173' = 'Frontend'
}

function Get-PortOwner {
    param([int]$Port)

    $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if (-not $conn) { return $null }
    $ownerPid = ($conn.OwningProcess | Select-Object -First 1)
    return Get-Process -Id $ownerPid -ErrorAction SilentlyContinue
}

if ($Stop) {
    Write-Host ''
    Write-Host 'Stopping the library stack' -ForegroundColor Cyan
    foreach ($port in $stackPorts.Keys) {
        $name = $stackPorts[$port]
        $proc = Get-PortOwner -Port ([int]$port)
        if ($proc) {
            Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
            Write-Host ("  {0,-22} :{1} stopped (PID {2})" -f $name, $port, $proc.Id) -ForegroundColor Green
        }
        else {
            Write-Host ("  {0,-22} :{1} already free" -f $name, $port) -ForegroundColor DarkGray
        }
    }
    Write-Host ''
    return
}

if ($All) {
    $Notifications = $true
    $Analytics = $true
}

function Start-LibraryService {
    param([string]$Name, [string]$Directory, [string]$Command, [int]$Port)

    if (-not (Test-Path $Directory)) {
        Write-Host ("  {0,-22} not found at {1} - skipping" -f $Name, $Directory) -ForegroundColor Yellow
        return
    }

    $proc = Get-PortOwner -Port $Port
    if ($proc) {
        Write-Host ("  {0,-22} :{1} already in use by PID {2} ({3}) - skipping" -f $Name, $Port, $proc.Id, $proc.ProcessName) -ForegroundColor Yellow
        return
    }

    $title = "library: $Name"
    $inner = "`$Host.UI.RawUI.WindowTitle = '$title'; Set-Location '$Directory'; $Command"
    Start-Process powershell -ArgumentList '-NoExit', '-Command', $inner | Out-Null

    Write-Host ("  {0,-22} starting on :{1}" -f $Name, $Port) -ForegroundColor Green
}

Write-Host ''
Write-Host 'Starting the library stack' -ForegroundColor Cyan
Write-Host ''

$profileArg = if ($Seed) { '' } else { ' "-Dspring-boot.run.profiles=dev"' }

Start-LibraryService -Name 'Library backend' -Port 9092 `
    -Directory (Join-Path $root 'Library-Management-System-Version-2') `
    -Command "./mvnw spring-boot:run$profileArg"

if ($Notifications) {
    Start-LibraryService -Name 'Notification-Service' -Port 9093 `
        -Directory (Join-Path $root 'Notification-Service\Notification-Service') `
        -Command './mvnw spring-boot:run'
}

if ($Analytics) {
    Start-LibraryService -Name 'Analytics-Service' -Port 9095 `
        -Directory (Join-Path $root 'Analytics-Service') `
        -Command './mvnw spring-boot:run'
}

Start-LibraryService -Name 'Frontend' -Port 5173 `
    -Directory (Join-Path $root 'frontend') `
    -Command 'npm run dev'

Write-Host ''
Write-Host 'Open http://localhost:5173 and sign in as admin / admin.' -ForegroundColor Cyan
if (-not $Notifications) { Write-Host '  Notification-Service not started (add -Notifications; needs MySQL).' -ForegroundColor DarkGray }
if (-not $Analytics)     { Write-Host '  Analytics-Service not started (add -Analytics; needs Kafka on 9094).' -ForegroundColor DarkGray }
Write-Host '  Stop everything with: .\dev.ps1 -Stop' -ForegroundColor DarkGray
Write-Host ''
