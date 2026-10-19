$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath (Join-Path (Split-Path -Parent $PSScriptRoot) 'frontend')
$listenAddress = if ($env:LOCAL_FRONTEND_HOST) { $env:LOCAL_FRONTEND_HOST } else { 'localhost' }
if ($listenAddress -eq '0.0.0.0') {
    & npm.cmd run build
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    & npm.cmd run preview -- --host $listenAddress --port 5173 --strictPort
} else {
    & npm.cmd run dev -- --host $listenAddress --strictPort
}
exit $LASTEXITCODE
