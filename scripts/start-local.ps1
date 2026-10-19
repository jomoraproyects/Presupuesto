param([switch]$Lan)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$localDir = Join-Path $projectRoot '.local'
$settingsPath = Join-Path $localDir 'settings.json'
$lanAddress = $null
if ($Lan) {
    $lanAddress = [Net.NetworkInformation.NetworkInterface]::GetAllNetworkInterfaces() |
        Where-Object { $_.OperationalStatus -eq 'Up' } |
        ForEach-Object {
            $properties = $_.GetIPProperties()
            if ($properties.GatewayAddresses.Count -gt 0) {
                $properties.UnicastAddresses |
                    Where-Object { $_.Address.AddressFamily -eq [Net.Sockets.AddressFamily]::InterNetwork -and $_.Address.ToString() -notlike '169.254*' } |
                    Select-Object -First 1 -ExpandProperty Address
            }
        } | Select-Object -First 1
    if ($lanAddress) { $lanAddress = $lanAddress.ToString() }
    if (!$lanAddress) { throw 'No se encontró una dirección IPv4 de red para el acceso LAN.' }
}
New-Item -ItemType Directory -Force -Path (Join-Path $localDir 'data') | Out-Null
if (!(Test-Path -LiteralPath $settingsPath)) {
    $settings = [ordered]@{
        adminUsername = 'admin'
        adminPassword = 'Sc!' + [guid]::NewGuid().ToString('N').Substring(0, 18)
        dbPassword = [guid]::NewGuid().ToString('N')
        jwtSecret = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
    }
    $settings | ConvertTo-Json | Set-Content -LiteralPath $settingsPath -Encoding UTF8
}
function Test-LocalPort([int]$port) {
    $client = New-Object Net.Sockets.TcpClient
    try { $client.Connect('localhost', $port); return $true } catch { return $false } finally { $client.Dispose() }
}
$backendOnline = Test-LocalPort 8080
$frontendOnline = Test-LocalPort 5173
if ($backendOnline -and $frontendOnline) {
    $existingConfig = Get-Content -Raw -LiteralPath $settingsPath | ConvertFrom-Json
    try {
        $null = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -ContentType 'application/json' -Body (@{ username = $existingConfig.adminUsername; password = $existingConfig.adminPassword } | ConvertTo-Json)
        if ($Lan) {
            $listeners = Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue
            if (!($listeners | Where-Object { $_.LocalAddress -in @('0.0.0.0', '::') })) {
                throw 'El frontend actual escucha solo en localhost. Deténgalo antes de iniciar con -Lan.'
            }
        }
        Write-Output 'El sistema local ya está funcionando: http://localhost:5173'
        return
    } catch { throw "No se inició otra instancia: $($_.Exception.Message)" }
}
if ($backendOnline -or $frontendOnline) {
    throw 'El puerto 8080 o 5173 ya está ocupado. Revise el proceso antes de iniciar otra instancia.'
}
if (!(Test-Path -LiteralPath (Join-Path $projectRoot 'frontend/node_modules'))) {
    Push-Location (Join-Path $projectRoot 'frontend')
    try { & npm.cmd ci; if ($LASTEXITCODE -ne 0) { throw 'npm ci falló' } } finally { Pop-Location }
}
$env:LOCAL_FRONTEND_HOST = if ($Lan) { '0.0.0.0' } else { 'localhost' }
$env:LOCAL_FRONTEND_ORIGINS = if ($Lan) { "http://localhost:5173,http://${lanAddress}:5173" } else { 'http://localhost:5173' }
$psExecutable = (Get-Process -Id $PID).Path
$backend = Start-Process -FilePath $psExecutable -ArgumentList @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', ('"' + (Join-Path $PSScriptRoot 'run-backend-local.ps1') + '"')) -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $localDir 'backend.log') -RedirectStandardError (Join-Path $localDir 'backend-error.log')
$frontend = Start-Process -FilePath $psExecutable -ArgumentList @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', ('"' + (Join-Path $PSScriptRoot 'run-frontend-local.ps1') + '"')) -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $localDir 'frontend.log') -RedirectStandardError (Join-Path $localDir 'frontend-error.log')
@{ backendLauncher = $backend.Id; frontendLauncher = $frontend.Id } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $localDir 'processes.json') -Encoding UTF8
Write-Output 'Iniciando backend y frontend. Los datos se conservan en .local/data.'
$ready = $false
for ($attempt = 0; $attempt -lt 90; $attempt++) {
    if ($backend.HasExited -or $frontend.HasExited) { throw 'Un proceso terminó antes del arranque. Consulte los logs en .local.' }
    if ((Test-LocalPort 8080) -and (Test-LocalPort 5173)) { $ready = $true; break }
    Start-Sleep -Seconds 1
}
if (!$ready) { throw 'El arranque continúa o falló. Consulte los logs en .local.' }
Write-Output 'Frontend listo: http://localhost:5173'
if ($Lan) { Write-Output "Acceso desde otro PC de la misma red: http://${lanAddress}:5173" }
Write-Output 'Credenciales locales de SUPER_ADMIN: .local/settings.json (adminUsername y adminPassword).'
