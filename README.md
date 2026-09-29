# Presupuesto Serviciudad E.S.P. — fases 1 y 2

Backend para formular y revisar el presupuesto anual. Incluye autenticación JWT, usuarios y roles, catálogos, necesidades por área, distribución entre servicios, historial y consolidados. Nómina, frontend, exportaciones e integraciones pertenecen a fases posteriores.

## Tecnologías y arquitectura

Java 21, Spring Boot 3.5, Maven, Spring Web, Security, Data JPA, Bean Validation, Flyway, MySQL, Lombok y JJWT. El código se organiza en `controller` (HTTP y validación), `dto` (contratos API), `service` (reglas y transacciones), `repository` (persistencia), `entity`, `security`, `config` y `exception`. Los controladores no exponen entidades JPA.

El esquema lo crea Flyway (`src/main/resources/db/migration`). `V3__necesidades.sql` agrega necesidades, distribuciones e historial. Hibernate usa `ddl-auto: validate`: comprueba el mapeo, pero no modifica la base de datos. Se usan IDs `Long`, `BigDecimal` para dinero y fechas Java Time.

## Requisitos

- JDK 21.
- MySQL 8.x disponible en `localhost:3306` o en otro host configurado.
- Acceso a Maven Central la primera vez que se ejecute Maven Wrapper.

No se requiere Maven instalado globalmente. En Windows se usa `mvnw.cmd`; en macOS o Linux, `./mvnw`.

## Base de datos y variables de entorno

Crear una base y un usuario MySQL con permisos sobre ella. Ejemplo para ejecutar en MySQL como administrador (sustituir nombre de usuario y contraseña por valores propios):

```sql
CREATE DATABASE presupuesto CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'presupuesto_app'@'localhost' IDENTIFIED BY '<contraseña-propia>';
GRANT ALL PRIVILEGES ON presupuesto.* TO 'presupuesto_app'@'localhost';
```

Configurar antes de iniciar la aplicación:

| Variable | Uso |
| --- | --- |
| `DB_HOST` | Host MySQL; por defecto `localhost` |
| `DB_PORT` | Puerto; por defecto `3306` |
| `DB_NAME` | Nombre de la base; por defecto `presupuesto` |
| `DB_USERNAME` | Usuario MySQL; obligatorio |
| `DB_PASSWORD` | Contraseña MySQL; obligatoria |
| `JWT_SECRET` | Clave HMAC UTF-8 de al menos 32 bytes; obligatoria |
| `JWT_EXPIRATION_MINUTES` | Duración del token; por defecto 60 |
| `ADMIN_USERNAME` | Nombre del administrador inicial; opcional |
| `ADMIN_PASSWORD` | Contraseña inicial de al menos 12 caracteres; opcional |
| `SERVER_PORT` | Puerto HTTP; por defecto 8080 |

`ADMIN_USERNAME` y `ADMIN_PASSWORD` se deben definir juntos. En el arranque, se crea el administrador `SUPER_ADMIN` únicamente si no existe el usuario. La contraseña se guarda como hash BCrypt. Cambiar las variables posteriormente no cambia su contraseña. No se incluye ninguna contraseña real en el repositorio.

Ejemplo en PowerShell, solicitando los secretos sin escribirlos en el historial:

```powershell
$env:DB_USERNAME = 'presupuesto_app'
$env:DB_PASSWORD = Read-Host 'Contraseña MySQL'
$env:JWT_SECRET = Read-Host 'Clave JWT de al menos 32 caracteres'
$env:ADMIN_USERNAME = 'admin'
$env:ADMIN_PASSWORD = Read-Host 'Contraseña inicial del administrador'
.\mvnw.cmd spring-boot:run
```

Para usar otro host o base, definir también `DB_HOST`, `DB_PORT` y `DB_NAME`.

## Compilar y probar

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd package
```

Las pruebas de integración usan H2 en modo MySQL para validar las migraciones, JPA, autenticación, permisos, flujo de necesidades y consolidados. El despliegue real usa MySQL; se recomienda ejecutar el arranque contra la instancia MySQL del entorno antes de ponerlo en servicio.

## API inicial

Todas las rutas excepto `POST /api/auth/login` requieren `Authorization: Bearer <token>`. Los cuatro roles pueden consultar catálogos; solo `SUPER_ADMIN` puede modificarlos. `SUPER_ADMIN` crea los demás usuarios en `POST /api/usuarios`, asignándoles un rol y, para `RESPONSABLE_AREA`, un `areaId` activo. La contraseña no aparece en la respuesta y se almacena con BCrypt.

| Recurso | Lectura | Escritura |
| --- | --- | --- |
| Áreas | `GET /api/areas`, `GET /api/areas/{id}` | `POST /api/areas`, `PUT /api/areas/{id}`, `PATCH /api/areas/{id}/estado` |
| Servicios | `GET /api/servicios`, `GET /api/servicios/{id}` | `POST /api/servicios`, `PUT /api/servicios/{id}`, `PATCH /api/servicios/{id}/estado` |
| Vigencias | `GET /api/vigencias`, `GET /api/vigencias/{id}` | `POST /api/vigencias`, `PUT /api/vigencias/{id}`, `PATCH /api/vigencias/{id}/estado` |
| Rubros | `GET /api/rubros`, `GET /api/rubros/{id}`, `GET /api/rubros/arbol` | `POST /api/rubros`, `PUT /api/rubros/{id}`, `PATCH /api/rubros/{id}/estado` |

El cuerpo de `/estado` es `{"activo":false}` o `{"activo":true}`. La desactivación es lógica; los registros siguen disponibles en las consultas de lista y detalle. Los rubros inactivos se omiten del árbol. `GET /api/rubros/arbol` muestra los rubros globales; `GET /api/rubros/arbol?vigenciaId=1` muestra los de esa vigencia. Una raíz tiene nivel 0. Los cambios de padre que produzcan ciclos se rechazan y los niveles descendientes se recalculan.

Ejemplo de login:

```http
POST /api/auth/login
Content-Type: application/json

{"username":"admin","password":"<contraseña-configurada>"}
```

Ejemplo de creación de rubros:

```json
{"codigo":"2","nombre":"GASTOS","orden":0}
```

El ID devuelto se usa en `rubroPadreId` del hijo:

```json
{"codigo":"2.1","nombre":"FUNCIONAMIENTO","rubroPadreId":1,"orden":0}
```

Los errores de la API usan `timestamp`, `status`, `error`, `message` y `path`. Las validaciones reciben 400, los recursos inexistentes 404, los conflictos de negocio 409, la falta de autenticación 401 y la falta de permisos 403.

## Usuarios

Solo `SUPER_ADMIN` puede usar `GET /api/usuarios`, `GET /api/usuarios/{id}`, `POST /api/usuarios`, `PUT /api/usuarios/{id}` y `PATCH /api/usuarios/{id}/estado`. El `PUT` cambia nombre, rol y área; no modifica la contraseña. El `PATCH` activa o desactiva al usuario. Un usuario desactivado no puede autenticarse ni usar un token existente.

Ejemplo de creación de responsable:

```json
{"username":"responsable.operativa","password":"<contraseña-segura-de-12-o-más-caracteres>","nombre":"Responsable Operativa","rol":"RESPONSABLE_AREA","areaId":1}
```

Crear usuarios `PRESUPUESTO` y `CONSULTA` de la misma forma, omitiendo `areaId`.

## Necesidades y revisión

Una necesidad pertenece a una vigencia, un área y un rubro activo que permite movimiento. `RESPONSABLE_AREA` crea y edita únicamente para su área; `SUPER_ADMIN` puede hacerlo para cualquier área. La vigencia debe estar activa y `ABIERTA`. El total solicitado se calcula como `cantidadSolicitada × valorUnitarioSolicitado`, redondeado a dos decimales.

```http
POST /api/necesidades
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "vigenciaId": 1,
  "areaId": 1,
  "rubroPresupuestalId": 2,
  "descripcion": "Compra de computadores",
  "justificacion": "Renovación tecnológica",
  "unidadMedida": "unidad",
  "cantidadSolicitada": 100,
  "valorUnitarioSolicitado": 1000000,
  "distribuciones": [
    {"servicioId": 1, "porcentaje": 33},
    {"servicioId": 2, "porcentaje": 33},
    {"servicioId": 3, "porcentaje": 34}
  ]
}
```

Los porcentajes deben sumar exactamente `100.00`; un único servicio se representa con una distribución del `100`. No se aceptan servicios duplicados o inactivos. Un borrador puede guardarse sin distribución, pero no enviarse. Los centavos se asignan según los restos decimales para que la suma distribuida coincida exactamente con el total.

| Operación | Ruta | Rol |
| --- | --- | --- |
| Listar con filtros y paginación | `GET /api/necesidades` | Todos; el responsable solo ve su área |
| Consultar y ver historial | `GET /api/necesidades/{id}`, `GET /api/necesidades/{id}/historial` | Todos, con restricción de área |
| Crear o corregir | `POST /api/necesidades`, `PUT /api/necesidades/{id}` | Responsable de área, administrador |
| Cancelar borrador lógico | `DELETE /api/necesidades/{id}` | Responsable de área, administrador |
| Enviar | `POST /api/necesidades/{id}/enviar` | Responsable de área, administrador |
| Iniciar revisión | `POST /api/necesidades/{id}/iniciar-revision` | Presupuesto, administrador |
| Ajustar | `PATCH /api/necesidades/{id}/ajuste` | Presupuesto, administrador |
| Aprobar | `POST /api/necesidades/{id}/aprobar` | Presupuesto, administrador |
| Rechazar o devolver | `POST /api/necesidades/{id}/rechazar`, `POST /api/necesidades/{id}/devolver` | Presupuesto, administrador |

`GET /api/necesidades` acepta `vigenciaId`, `areaId`, `servicioId`, `rubroId`, `estado`, `texto`, `page` y `size` (máximo 100 registros por página). El `areaId` enviado por un responsable se ignora y se aplica siempre su área autenticada.

La edición normal solo se permite en `BORRADOR` y `DEVUELTA`. Al enviar, la necesidad pasa a `ENVIADA`; Presupuesto la pasa a `EN_REVISION`. El ajuste acepta `cantidadAprobada`, `valorUnitarioAprobado` y `observacionPresupuesto`, recalcula el aprobado y mantiene intactos los valores solicitados. Aprobar sin ajustes copia lo solicitado; con diferencias resulta `APROBADA_CON_AJUSTES`; un aprobado de cero resulta `RECHAZADA`. Rechazar y devolver requieren `{"observacionPresupuesto":"motivo"}`. La devolución borra el aprobado provisional y permite corregir y reenviar.

`cantidadSolicitadaOriginal`, `valorUnitarioSolicitadoOriginal` y `valorTotalSolicitadoOriginal` se fijan en el primer envío y no cambian si el área corrige una necesidad devuelta. Los campos `*Solicitada` sin sufijo `Original` reflejan la solicitud vigente. El historial registra cada transición y guarda instantáneas anterior y nueva; no ofrece endpoints de modificación o borrado.

## Consolidados

Disponibles para `PRESUPUESTO`, `SUPER_ADMIN` y `CONSULTA`:

- `GET /api/presupuesto/vigencias/{vigenciaId}/resumen`
- `GET /api/presupuesto/vigencias/{vigenciaId}/por-area`
- `GET /api/presupuesto/vigencias/{vigenciaId}/por-servicio`
- `GET /api/presupuesto/vigencias/{vigenciaId}/por-rubro`

Se incluyen necesidades `ENVIADA`, `EN_REVISION`, `APROBADA`, `APROBADA_CON_AJUSTES` y `RECHAZADA`. Los borradores, las devueltas y las canceladas se excluyen. El aprobado aún no definido cuenta como cero. El resumen usa `diferenciaContraTecho = totalAprobado - presupuestoDisponible`: positivo significa exceso sobre el techo. Los totales por servicio se calculan con las distribuciones y los de rubro acumulan los importes de todos sus descendientes sin guardar totales redundantes.
