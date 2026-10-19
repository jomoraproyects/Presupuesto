$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$config = Get-Content -Raw -LiteralPath (Join-Path $projectRoot '.local/settings.json') | ConvertFrom-Json
$login = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -ContentType 'application/json' -Body (@{ username = $config.adminUsername; password = $config.adminPassword } | ConvertTo-Json)
$headers = @{ Authorization = 'Bearer ' + $login.token }
function ConfigApi([string]$method, [string]$route, $body) {
    $params = @{ Uri = 'http://localhost:8080/api/' + $route; Method = $method; Headers = $headers }
    if ($null -ne $body) { $params.ContentType = 'application/json'; $params.Body = $body | ConvertTo-Json -Depth 6 }
    $result = Invoke-RestMethod @params
    foreach ($item in $result) { $item }
}
$year = @(ConfigApi 'Get' 'vigencias' $null) | Where-Object { $_.activa -and $_.estado -eq 'ABIERTA' } | Sort-Object anio -Descending | Select-Object -First 1
if (!$year) { throw 'Abra una vigencia antes de habilitar las subgerencias.' }
$areas = @(ConfigApi 'Get' 'areas' $null)
$users = @(ConfigApi 'Get' 'usuarios' $null)
$credentialsPath = Join-Path $projectRoot '.local/perfiles-subgerencias.csv'
$credentials = @()
if (Test-Path -LiteralPath $credentialsPath) { $credentials = @(Import-Csv -LiteralPath $credentialsPath) }
$definitions = @(
    @{ codigo = 'FIN'; nombre = 'Financiera'; usuario = 'financiera' },
    @{ codigo = 'COM'; nombre = 'Comercial'; usuario = 'comercial' },
    @{ codigo = 'PLA'; nombre = 'Planeación'; usuario = 'planeacion' },
    @{ codigo = 'TEC'; nombre = 'Técnica'; usuario = 'tecnica' },
    @{ codigo = 'SG'; nombre = 'Secretaría general'; usuario = 'secretaria.general' },
    @{ codigo = 'CIG'; nombre = 'Control interno gestión'; usuario = 'control.interno.gestion' },
    @{ codigo = 'CID'; nombre = 'Control interno disciplinario'; usuario = 'control.interno.disciplinario' },
    @{ codigo = 'GER'; nombre = 'Gerencia'; usuario = 'gerencia' }
)
foreach ($definition in $definitions) {
    $area = $areas | Where-Object { $_.codigo -eq $definition.codigo -or $_.nombre -eq $definition.nombre } | Select-Object -First 1
    $user = $users | Where-Object { $_.username -eq $definition.usuario } | Select-Object -First 1
    if ($user -and (!$area -or $user.areaId -ne $area.id -or $user.rol -ne 'RESPONSABLE_AREA')) { throw ('El usuario ' + $definition.usuario + ' ya pertenece a otro perfil; no se modificó.') }
    if (!$area) {
        $password = 'Sc!' + [guid]::NewGuid().ToString('N').Substring(0, 18)
        $created = ConfigApi 'Post' 'areas/con-responsable' @{ area = @{ codigo = $definition.codigo; nombre = $definition.nombre }; responsable = @{ nombre = $definition.nombre; username = $definition.usuario; password = $password }; vigenciaId = $year.id }
        $area = $created.area; $user = $created.responsable
        $credentials += [pscustomobject]@{ subgerencia = $definition.nombre; usuario = $definition.usuario; password = $password; rol = 'RESPONSABLE_AREA' }
        $credentials | Export-Csv -LiteralPath $credentialsPath -NoTypeInformation -Encoding UTF8
    } else {
        if (!$area.activa) { $null = ConfigApi 'Patch' ('areas/' + $area.id + '/estado') @{ activo = $true } }
        if (!$user) {
            $password = 'Sc!' + [guid]::NewGuid().ToString('N').Substring(0, 18)
            $user = ConfigApi 'Post' 'usuarios' @{ username = $definition.usuario; password = $password; nombre = $definition.nombre; rol = 'RESPONSABLE_AREA'; areaId = $area.id }
            $credentials += [pscustomobject]@{ subgerencia = $definition.nombre; usuario = $definition.usuario; password = $password; rol = 'RESPONSABLE_AREA' }
            $credentials | Export-Csv -LiteralPath $credentialsPath -NoTypeInformation -Encoding UTF8
        }
        $null = ConfigApi 'Put' ('vigencias/' + $year.id + '/participantes/' + $area.id) @{ activo = $true }
    }
    Write-Output ($definition.nombre + ': perfil ' + $definition.usuario + ' conectado a vigencia ' + $year.anio)
}
# Desactivar únicamente el ejemplo creado por el asistente; conservar sus registros.
$demo = $areas | Where-Object { $_.codigo -eq 'OP-DEMO' -and $_.nombre -eq 'Operativa (ejemplo)' } | Select-Object -First 1
if ($demo) {
    foreach ($demoUser in ($users | Where-Object { $_.areaId -eq $demo.id -and $_.username -eq 'operativa@ejemplo.test' })) { $null = ConfigApi 'Patch' ('usuarios/' + $demoUser.id + '/estado') @{ activo = $false } }
    $null = ConfigApi 'Put' ('vigencias/' + $year.id + '/participantes/' + $demo.id) @{ activo = $false }
    if ($demo.activa) { $null = ConfigApi 'Patch' ('areas/' + $demo.id + '/estado') @{ activo = $false } }
}
Write-Output 'Credenciales de desarrollo guardadas en .local/perfiles-subgerencias.csv, excluido de Git.'
