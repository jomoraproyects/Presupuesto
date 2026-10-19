$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$config = Get-Content -Raw -LiteralPath (Join-Path $projectRoot '.local/settings.json') | ConvertFrom-Json
$loginBody = @{ username = $config.adminUsername; password = $config.adminPassword } | ConvertTo-Json
$login = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -ContentType 'application/json' -Body $loginBody
$headers = @{ Authorization = 'Bearer ' + $login.token }
function LocalApi([string]$route, $body) {
    Invoke-RestMethod -Uri ('http://localhost:8080/api/' + $route) -Headers $headers -Method Post -ContentType 'application/json' -Body ($body | ConvertTo-Json -Depth 6)
}
$years = @(Invoke-RestMethod -Uri 'http://localhost:8080/api/vigencias' -Headers $headers)
$year = $years | Where-Object { $_.anio -eq 2027 } | Select-Object -First 1
if (!$year) {
    $year = LocalApi 'vigencias' @{ anio = 2027; nombre = 'Presupuesto 2027'; fechaInicio = '2027-01-01'; fechaFin = '2027-12-31'; estado = 'ABIERTA'; presupuestoDisponible = 0 }
}
$rubros = @(Invoke-RestMethod -Uri 'http://localhost:8080/api/rubros' -Headers $headers)
$root = $rubros | Where-Object { $_.codigo -eq 'FORM' -and $_.vigenciaId -eq $year.id } | Select-Object -First 1
if (!$root) { $root = LocalApi 'rubros' @{ codigo = 'FORM'; nombre = 'FORMULACIÓN ANUAL'; vigenciaId = $year.id; permiteMovimiento = $false; categoriaFormulacion = $false } }
$categories = @(
    @{ codigo = 'FORM.PERSONAL'; nombre = 'CONTRATACIÓN PERSONAL ADMINISTRATIVO' },
    @{ codigo = 'FORM.HARDWARE'; nombre = 'ADQUISICIÓN DE HARDWARE' },
    @{ codigo = 'FORM.SOFTWARE'; nombre = 'ADQUISICIÓN DE SOFTWARE' },
    @{ codigo = 'FORM.MATERIALES'; nombre = 'COMPRA MATERIALES Y SUMINISTROS' },
    @{ codigo = 'FORM.VIATICOS'; nombre = 'VIÁTICOS' },
    @{ codigo = 'FORM.CAPACITACION'; nombre = 'CAPACITACIÓN' },
    @{ codigo = 'FORM.BIENESTAR'; nombre = 'BIENESTAR' }
)
foreach ($category in $categories) {
    if (!($rubros | Where-Object { $_.codigo -eq $category.codigo -and $_.vigenciaId -eq $year.id })) {
        $null = LocalApi 'rubros' @{ codigo = $category.codigo; nombre = $category.nombre; vigenciaId = $year.id; rubroPadreId = $root.id; permiteMovimiento = $true; categoriaFormulacion = $true }
    }
}
Write-Output 'Vigencia 2027 y siete categorías editables preparadas. El techo presupuestal inicia en cero para que lo defina Presupuesto.'
