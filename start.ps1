$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$back = Join-Path $root "dragontrainer-bigbank"
$front = Join-Path $root "DragonTrainer"

$envExample = Join-Path $back ".env.example"
$envFile = Join-Path $back ".env"
if (-not (Test-Path $envFile)) {
  Copy-Item $envExample $envFile
  Write-Host "Created dragontrainer-bigbank/.env from .env.example"
}

function Test-Port([int] $port) {
  if (Get-Command Get-NetTCPConnection -ErrorAction SilentlyContinue) {
    $listening = @(Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)
    if ($listening.Count -gt 0) {
      return $true
    }
  }
  $client = New-Object System.Net.Sockets.TcpClient
  try {
    $client.Connect("127.0.0.1", $port)
    return $true
  } catch {
    return $false
  } finally {
    $client.Dispose()
  }
}

function Find-NodePort([int] $from, [int] $to) {
  if (-not (Get-Command Get-NetTCPConnection -ErrorAction SilentlyContinue)) {
    return $null
  }
  foreach ($port in $from..$to) {
    $connections = @(Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)
    foreach ($connection in $connections) {
      $process = Get-Process -Id $connection.OwningProcess -ErrorAction SilentlyContinue
      if ($process -and $process.ProcessName -eq "node") {
        return $port
      }
    }
  }
  return $null
}

function Find-FreePort([int] $from, [int] $to) {
  foreach ($port in $from..$to) {
    if (-not (Test-Port $port)) {
      return $port
    }
  }
  return $null
}

function Wait-Port([int] $port, [string] $name) {
  $deadline = (Get-Date).AddMinutes(5)
  while (-not (Test-Port $port)) {
    if ((Get-Date) -gt $deadline) {
      throw "$name did not open port $port"
    }
    Start-Sleep -Seconds 2
  }
}

function Wait-Frontend([int] $preferred) {
  $deadline = (Get-Date).AddMinutes(5)
  while ((Get-Date) -lt $deadline) {
    if (Test-Port $preferred) {
      return $preferred
    }
    $opened = Find-NodePort 4200 4210
    if ($opened) {
      return $opened
    }
    Start-Sleep -Seconds 2
  }
  throw "Frontend did not open a port between 4200 and 4210"
}

Push-Location $back
try {
  $state = docker inspect -f "{{.State.Running}}" dragontrainer-postgres 2>$null
  if ($LASTEXITCODE -ne 0 -or $state -ne "true") {
    Write-Host "Starting Postgres..."
    docker compose up -d
    if ($LASTEXITCODE -ne 0) {
      throw "Docker Compose could not start Postgres"
    }
  } else {
    Write-Host "Postgres is already running."
  }
} finally {
  Pop-Location
}

Wait-Port 5432 "Postgres"

if (Test-Port 8080) {
  Write-Host "Backend is already listening on port 8080."
} else {
  Write-Host "Starting backend..."
  Start-Process -FilePath "cmd.exe" -ArgumentList "/c", "gradlew.bat bootRun" -WorkingDirectory $back
  Wait-Port 8080 "Backend"
}

$frontPort = Find-NodePort 4200 4210
if ($frontPort) {
  Write-Host "Frontend is already listening on port $frontPort."
} else {
  $frontPort = Find-FreePort 4200 4210
  if (-not $frontPort) {
    throw "No free port for the frontend between 4200 and 4210"
  }
  Write-Host "Starting frontend on port $frontPort..."
  Start-Process -FilePath "cmd.exe" -ArgumentList "/c", "npm start -- --port $frontPort" -WorkingDirectory $front
  $startedOn = $frontPort
  $frontPort = Wait-Frontend $frontPort
  if ($frontPort -ne $startedOn) {
    Write-Host "Frontend opened on port $frontPort."
  }
}

Write-Host ""
Write-Host "Front: http://localhost:$frontPort"
Write-Host "Back:  http://localhost:8080"
