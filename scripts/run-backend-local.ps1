$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
$localConfig = Get-Content -Raw -LiteralPath (Join-Path $projectRoot '.local/settings.json') | ConvertFrom-Json
$env:LOCAL_DB_PASSWORD = $localConfig.dbPassword
$env:LOCAL_JWT_SECRET = $localConfig.jwtSecret
$env:LOCAL_ADMIN_USERNAME = $localConfig.adminUsername
$env:LOCAL_ADMIN_PASSWORD = $localConfig.adminPassword
$env:SPRING_PROFILES_ACTIVE = 'local'
& (Join-Path $projectRoot 'mvnw.cmd') '-Dspring-boot.run.useTestClasspath=true' spring-boot:run
exit $LASTEXITCODE
