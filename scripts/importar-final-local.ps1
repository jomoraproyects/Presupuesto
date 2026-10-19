param([int]$Anio = 2027)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
if (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) {
    throw 'Detenga el backend local antes de importar: la base H2 debe estar cerrada.'
}
$config = Get-Content -Raw -LiteralPath .local/settings.json | ConvertFrom-Json
$env:LOCAL_DB_PASSWORD = $config.dbPassword
try {
    Copy-Item -LiteralPath .local/data/presupuesto.mv.db -Destination ('.local/data/antes-final-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.mv.db')
    & .\mvnw.cmd dependency:build-classpath '-Dmdep.outputFile=.local/classpath.txt'
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo obtener el classpath' }
    $classpath = Get-Content -Raw -LiteralPath .local/classpath.txt
    & java --class-path $classpath scripts/ImportarRubrosFinal.java docs/catalogo-final.json $Anio
    if ($LASTEXITCODE -ne 0) { throw 'La carga fue revertida; revise el error' }
} finally { Remove-Item Env:LOCAL_DB_PASSWORD -ErrorAction SilentlyContinue }
