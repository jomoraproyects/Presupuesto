# Presupuesto anual de Serviciudad E.S.P.

Monolito Spring Boot con frontend React para formular y aprobar el presupuesto anual. Se conservan la autenticación, áreas, vigencias, conceptos, ítems, historial y consolidados existentes.

Para instalarlo en la carpeta `~/Escritorio/presupuesto` de la VM Ubuntu, con una base MySQL nueva y sin modificar Selección, consulte [la guía de despliegue](deploy/INSTALACION_UBUNTU.md). En ese modo Spring Boot sirve tanto el frontend compilado como `/api`.

## Flujo actual

1. SUPER_ADMIN entra a **Crear presupuesto**, crea la vigencia en borrador, revisa los conceptos y puede agregar, editar, activar, desactivar u ordenar conceptos. Cada año recibe copias propias de los conceptos; nombres repetidos conservan IDs distintos.
2. **Enviar a dependencias** habilita el mismo formulario para todas las áreas activas. Las ocho dependencias iniciales se cargan por Flyway; el administrador puede gestionar otras desde **Dependencias**. Cada responsable usa el login único y solo ve su área.
3. La dependencia añade 0, 1 o varios ítems por concepto. Cada ítem guarda descripción, cantidad, valor unitario y distribución opcional; el total se calcula. Sin servicios, el valor queda General; con uno se asigna 100 %; con dos o tres, los porcentajes deben sumar 100 %.
4. Los ítems se guardan en borrador y se pueden continuar después. **Enviar a Presupuesto** envía el formulario completo, registra usuario y fecha, conserva una instantánea de lo enviado y bloquea su edición por el área.
5. Presupuesto consulta **Dependencias** y **Presupuesto General**. Este último se calcula por concepto a partir de los ítems; para ajustar un total se abre la dependencia y luego el ítem. Cantidad y valor unitario aprobados se guardan separados de los solicitados, y la distribución aprobada se guarda separada de la original.
6. **Aprobar presupuesto** cierra la revisión de la dependencia completa. Los totales de ítem, concepto, dependencia y general se recalculan automáticamente sin doble conteo.

La interfaz principal expone PENDIENTE, BORRADOR, ENVIADO, EN_REVISION y APROBADO. Se conservan los endpoints históricos para compatibilidad. Nómina, Macro final, exportación Excel, ejecución, correos, firmas y aprobaciones adicionales quedan fuera de esta iteración.

## Verificación

Con JDK 21 en `JAVA_HOME`, ejecutar `./mvnw.cmd clean verify` (en Windows se configuró un agente Mockito para evitar el bloqueo de autoanexado) y `npm run build` dentro de `frontend`. Las pruebas de integración cubren envío anual, aislamiento por área, varios ítems por concepto, distribución solicitada y aprobada, ajustes, aprobación y totales consolidados.
## Tecnologías y arquitectura

Java 21, Spring Boot 3.5, Maven, Spring Web, Security, Data JPA, Bean Validation, Flyway, MySQL, Lombok y JJWT. El código se organiza en `controller` (HTTP y validación), `dto` (contratos API), `service` (reglas y transacciones), `repository` (persistencia), `entity`, `security`, `config` y `exception`. Los controladores no exponen entidades JPA.

El esquema lo crea Flyway (`src/main/resources/db/migration`). `V3__necesidades.sql` agrega necesidades, distribuciones e historial. `V4__formulacion_anual.sql` agrega participantes por año, mapeos y las opciones de categoría y distribución; habilita las áreas que ya tenían solicitudes. Hibernate usa `ddl-auto: validate`: comprueba el mapeo, pero no modifica la base de datos. Se usan IDs `Long`, `BigDecimal` para dinero y fechas Java Time.

## Requisitos

### Inicio local listo para usar (Windows)

```powershell
.\scripts\start-local.ps1
.\scripts\prepare-local.ps1
.\scripts\configurar-subgerencias.ps1
```

`start-local.ps1` inicia backend y frontend en segundo plano y crea un SUPER_ADMIN con usuario `admin` y contraseña aleatoria. Las credenciales se guardan únicamente en `.local/settings.json`, excluido de Git. En el perfil local, `adminPassword` se sincroniza con la cuenta `admin` al arrancar y admite al menos 11 caracteres. Abrir `http://localhost:5173`; con `-Lan` el frontend también queda disponible para la red local. La base H2 de desarrollo se conserva en `.local/data/presupuesto.mv.db` entre reinicios; el backend escucha solo en este equipo y el frontend transmite `/api` mediante su proxy.

`prepare-local.ps1` prepara la vigencia 2027 y la plantilla inicial, sin necesidades ni importes. La base actual ya tiene el catálogo de **Ff.xlsx**, hoja **final**: 449 códigos presupuestales originales y 116 categorías de la iteración anterior, con 264 destinos por servicio. El formulario actual usa los 65 conceptos reales cargados por V7. No se importaron valores históricos del Excel. El techo queda en cero para que lo configure Presupuesto.

El catálogo reproducible está en `docs/catalogo-final.json`: conserva las 495 filas originales, incluidas 46 repeticiones. Un código repetido representa un solo rubro; se usa el primer nombre y orden, y todas las variantes permanecen en el registro de origen. Las tres cabeceras numéricas se almacenan como su texto visible (`2.2`, `2.3`, `2.4`). Los espacios, comas y guiones de los demás códigos se conservan exactamente.

Los títulos finales tienen `permiteMovimiento=false`. Las categorías reutilizan el modelo existente y claves internas `FORM.*`, que no son códigos presupuestales y no aparecen en el consolidado final. Sin distribución, su importe alimenta el rubro general padre y no crea valores por servicio. Con distribución, solo alimenta los destinos originales configurados. El consolidado distingue el valor general sin asignar a servicios de los hijos de Acueducto, Alcantarillado y Aseo, sin contarlo dos veces. No se implementa el cálculo de nómina; el formulario actual conserva el concepto genérico de sueldo pedido en la lista.

Para reproducir la carga en otra base **local de desarrollo**, prepare primero la vigencia, detenga el backend y ejecute `scripts/importar-final-local.ps1`. Guarda respaldo y aplica todo en una transacción. Para releer el archivo fuente: `python scripts/extraer-final.py C:/ruta/Ff.xlsx docs/catalogo-final.json` (requiere `openpyxl`). No modifica el Excel ni las solicitudes. La plantilla anterior vacía se desactiva; los rubros con historial se conservan.

`configurar-subgerencias.ps1` habilita Financiera, Comercial, Planeación, Técnica, Secretaría general, Control interno gestión, Control interno disciplinario y Gerencia, cada una con un responsable y el mismo formulario. La revisión y administración se realizan con el único perfil central SUPER_ADMIN. Guarda las contraseñas iniciales en `.local/perfiles-subgerencias.csv`, excluido de Git; las ejecuciones siguientes conservan los perfiles y sus credenciales. Desactiva únicamente el ejemplo Operativa creado durante la validación.

En **Subgerencias**, SUPER_ADMIN puede crear en una sola operación: código/nombre de área, responsable, usuario, contraseña inicial y participación opcional en una vigencia abierta o borrador. El responsable obtiene automáticamente el rol `RESPONSABLE_AREA` y queda limitado a esa área. Si falla cualquier paso, se revierte la operación completa. Las contraseñas se guardan como hash y no se devuelven en la respuesta.

En **Usuarios**, SUPER_ADMIN crea perfiles con nombre visible en dashboard, usuario o correo como identificador de ingreso, contraseña inicial, rol y área cuando corresponde. Los responsables usan la misma interfaz y plantilla de categorías, con sus propios datos y nombre. El correo se utiliza como identificador; no se envían correos ni se exige verificación de correo en esta fase.

Ruta frontend: `/admin/subgerencias`. Endpoint exclusivo de SUPER_ADMIN: `POST /api/areas/con-responsable`.

```json
{
  "area": {"codigo": "OP", "nombre": "Operativa"},
  "responsable": {"nombre": "Responsable Operativa", "username": "operativa", "password": "<contraseña-de-al-menos-11-caracteres>"},
  "vigenciaId": 1
}
```

`vigenciaId` puede ser `null` para crear primero y habilitar después. Las pantallas **Áreas**, **Usuarios** y **Configuración anual** permiten editar, activar/desactivar y gestionar la participación de registros existentes.

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
| `FRONTEND_ORIGIN` | Origen CORS autorizado; por defecto `http://localhost:5173` |

`ADMIN_USERNAME` y `ADMIN_PASSWORD` se deben definir juntos. En la configuración normal, se crea el administrador `SUPER_ADMIN` únicamente si no existe el usuario. La contraseña se guarda como hash BCrypt y cambiar las variables posteriormente no la modifica. El perfil local descrito arriba sí sincroniza `adminPassword` al arrancar. No se incluye ninguna contraseña real en el repositorio.

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
.\mvnw.cmd clean verify
```

Las pruebas de integración usan H2 en modo MySQL para validar las migraciones, JPA, autenticación, permisos, flujo de necesidades y consolidados. El despliegue real usa MySQL; se recomienda ejecutar el arranque contra la instancia MySQL del entorno antes de ponerlo en servicio.

## Frontend (Fase 3)

Requiere Node.js reciente y npm. En otra terminal, con el backend en marcha:

```powershell
cd frontend
npm ci
npm run dev
```

Abrir `http://localhost:5173`. Vite envía `/api` al backend en `http://localhost:8080` durante desarrollo. Para un backend remoto, copiar `frontend/.env.example` a `frontend/.env.local` y configurar `VITE_API_URL` con la URL completa terminada en `/api`. Si el origen del navegador difiere de `http://localhost:5173`, configurar `FRONTEND_ORIGIN` en el backend. La sesión se guarda en `sessionStorage`; al cerrar la pestaña se pierde el token.

Para comprobar la compilación:

```powershell
cd frontend
npm run build
```

### Rutas y roles

| Ruta | Uso | Roles |
| --- | --- | --- |
| `/login` | Autenticación | Público |
| `/dashboard` | Resumen global o del área | Todos |
| `/necesidades` y `/necesidades/:id` | Listado paginado, detalle, historial y acciones permitidas | Todos |
| `/necesidades/nueva`, `/necesidades/:id/editar` | Crear o corregir borradores y devueltas | `RESPONSABLE_AREA`, `SUPER_ADMIN` |
| `/consolidado/areas`, `/consolidado/servicios`, `/consolidado/rubros` | Totales por dimensión | `SUPER_ADMIN`, `CONSULTA` |
| `/admin/usuarios`, `/admin/areas`, `/admin/servicios`, `/admin/rubros` | Administración de catálogos | `SUPER_ADMIN` |
| `/admin/vigencias` | Crear y editar vigencias | `SUPER_ADMIN` |
| `/formulacion` | Categorías e ítems del área por año | Todos; responsable restringido a su área |
| `/configuracion-vigencia` | Participantes y mapeos | Administrador escribe participantes; Presupuesto escribe mapeos; Consulta solo lee |
| `/consolidado/maestro` | Árbol final y mapeos pendientes | `SUPER_ADMIN`, `CONSULTA` |

El frontend consume los endpoints existentes para enviar, revisar, ajustar, aprobar, rechazar, devolver y consultar historial. Se añadieron dos endpoints de lectura para esta fase: `GET /api/auth/me` devuelve nombre, rol y área de la sesión; `GET /api/presupuesto/vigencias/{vigenciaId}/mi-area` devuelve totales y conteo por estado solo al responsable de esa área. La seguridad de todas las operaciones sigue en el backend.

Para disponer del primer usuario, establecer `ADMIN_USERNAME` y `ADMIN_PASSWORD` antes del primer arranque, como se explica arriba. Desde `/admin/usuarios`, el administrador puede crear responsables de área y usuarios de Presupuesto. Para crear necesidades también debe existir una vigencia activa y abierta, un área activa y el área habilitada como participante de esa vigencia y un rubro activo, sin hijos activos, marcado para movimiento y categoría del formulario; los tres servicios iniciales se cargan con Flyway.

## API inicial

Todas las rutas excepto `POST /api/auth/login` requieren `Authorization: Bearer <token>`. Los tres roles pueden consultar catálogos. `SUPER_ADMIN` reúne administración, revisión de presupuesto, vigencias, copia de configuración y mapeos; `RESPONSABLE_AREA` formula para su propia área y `CONSULTA` conserva sus consultas existentes. `SUPER_ADMIN` crea los demás usuarios en `POST /api/usuarios`, asignándoles un rol y, para `RESPONSABLE_AREA`, un `areaId` activo. La contraseña no aparece en la respuesta y se almacena con BCrypt.

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

Crear usuarios `SUPER_ADMIN` o `CONSULTA` de la misma forma, omitiendo `areaId`. No se permite crear el antiguo rol separado `PRESUPUESTO`.

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

Los porcentajes deben sumar exactamente `100.00`; un único servicio se representa con una distribución del `100`. No se aceptan servicios duplicados o inactivos. Con `aplicaDistribucion: true` (valor predeterminado), un borrador puede guardarse sin distribución, pero no enviarse. Con `aplicaDistribucion: false` y `distribuciones: []` puede enviarse sin porcentajes; su total se consolida directamente en el rubro de origen y no se asigna a servicios. Los centavos se asignan según los restos decimales para que la suma distribuida coincida exactamente con el total.

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

La edición normal solo se permite en `BORRADOR` y `DEVUELTA`. Al enviar, la necesidad pasa a `ENVIADA`; Presupuesto la pasa a `EN_REVISION`. El ajuste acepta `cantidadAprobada`, `valorUnitarioAprobado` y `observacionPresupuesto`, recalcula el aprobado y mantiene intactos los valores solicitados. Aprobar sin ajustes copia lo solicitado; con diferencias resulta `APROBADA_CON_AJUSTES`; un aprobado de cero resulta `RECHAZADA`. Reducir o aumentar cantidad o valor unitario exige un motivo nuevo en `observacionPresupuesto`. Rechazar y devolver requieren `{"observacionPresupuesto":"motivo"}`. La devolución borra el aprobado provisional y permite corregir y reenviar.

`cantidadSolicitadaOriginal`, `valorUnitarioSolicitadoOriginal` y `valorTotalSolicitadoOriginal` se fijan en el primer envío y no cambian si el área corrige una necesidad devuelta. Los campos `*Solicitada` sin sufijo `Original` reflejan la solicitud vigente. El historial registra cada transición y guarda instantáneas anterior y nueva; no ofrece endpoints de modificación o borrado.

## Consolidados

Disponibles para `SUPER_ADMIN` y `CONSULTA`:

- `GET /api/presupuesto/vigencias/{vigenciaId}/resumen`
- `GET /api/presupuesto/vigencias/{vigenciaId}/por-area`
- `GET /api/presupuesto/vigencias/{vigenciaId}/por-servicio`
- `GET /api/presupuesto/vigencias/{vigenciaId}/por-rubro`

Se incluyen todos los ítems activos: borradores, devueltos, enviados y revisados. Los cancelados se excluyen. El aprobado aún no definido cuenta como cero; al enviar se inicializa con lo solicitado. El resumen usa `diferenciaContraTecho = totalAprobado - presupuestoDisponible`: positivo significa exceso sobre el techo. Los totales por servicio se calculan con las distribuciones y los de rubro acumulan los importes de todos sus descendientes sin guardar totales redundantes.

## Formulación anual y maestro final

Se reutiliza `NecesidadPresupuestal`: un rubro admite N ítems. No hay entidad de plantilla adicional ni columna editable para el total del rubro. Solo se agregan `AreaVigencia` para habilitación anual y `MapeoRubroServicio` para el destino presupuestal de cada servicio.

1. Presupuesto o el administrador crea una vigencia `ABIERTA` (o prepara una `BORRADOR`).
2. El administrador configura categorías con `permiteMovimiento: true`, `categoriaFormulacion: true` y sin hijos activos. Los rubros superiores son de agrupación. Los destinos del maestro se marcan `categoriaFormulacion: false`.
3. En **Configuración anual**, el administrador habilita las áreas participantes. Puede agregarlas en `BORRADOR` o `ABIERTA`. Deshabilitar una participante bloquea nuevos ítems; conserva los existentes y permite su corrección y reenvío mientras la vigencia siga abierta.
4. El responsable usa **Mi formulario**, agrega varios ítems por categoría, elige **NO DISTRIBUIR** o porcentajes por servicio y pulsa **ENVIAR A PRESUPUESTO**. Si un ítem no es válido, se revierte todo el envío.
5. Presupuesto entra a la subgerencia, abre el ítem y usa **Guardar aprobado**. Cantidad y valor aprobado se guardan juntos con motivo e historial. Los campos `*Original` preservan cantidades e importes del primer envío.
6. En **Configuración anual**, Presupuesto o el administrador asigna categoría + servicio → rubro final. Origen y destino deben ser hojas activas de la vigencia o globales, y el destino queda excluido de formulación. Los mapeos se administran en `BORRADOR`, `ABIERTA` o `EN_REVISION`.

| Endpoint adicional | Contrato |
| --- | --- |
| `GET /api/formulacion/vigencias/{id}?areaId=1` | Área, posibilidad de crear, categorías, ítems y sumas solicitadas/aprobadas/diferencia. El responsable obtiene únicamente su área. Otros roles deben indicar `areaId`. |
| `POST /api/formulacion/vigencias/{id}/enviar?areaId=1` | Envía todos los borradores y devueltos del área en una transacción. Solo responsable de su área y SUPER_ADMIN. |
| `GET /api/vigencias/{id}/participantes` | Todas las áreas y su habilitación anual. |
| `PUT /api/vigencias/{id}/participantes/{areaId}` | Administrador; cuerpo `{"activo":true}` o `false`. |
| `GET /api/vigencias/{id}/mapeos` | Configuración de destinos, incluidos los inactivos. |
| `POST /api/vigencias/{id}/mapeos` | Crear; cuerpo `{"rubroOrigenId":2,"servicioId":1,"rubroDestinoId":5}`. |
| `PUT /api/vigencias/{id}/mapeos/{mapeoId}` | Editar el mismo contrato. |
| `PATCH /api/vigencias/{id}/mapeos/{mapeoId}/estado` | Activar/desactivar con `{"activo":true}` o `false`. |
| `GET /api/presupuesto/vigencias/{id}/maestro` | Totales y árbol `{codigo,nombre,nivel,solicitado,aprobado,diferencia,hijos}`. |
| `POST /api/vigencias/{id}/copiar-configuracion` | Crear año siguiente copiando rubros/categorías/mapeos, con participantes opcionales. |

El formulario agrupado incluye borradores y devueltas para permitir diligenciar todo el año. Los consolidados y el maestro usan los mismos estados de inclusión descritos arriba. Los ajustes impactan los valores de revisión inmediatamente, antes de aprobar. `diferencia = aprobado - solicitado`.

El maestro lleva los importes distribuidos a los destinos configurados y suma sus ancestros. Un importe se incorpora una sola vez. Cuando falta un mapeo, conserva el importe provisionalmente en el origen y devuelve `mapeoCompleto: false` y `mapeosPendientes`; la pantalla señala que el resultado requiere completar la configuración. Los ítems sin distribución permanecen en su categoría. La consulta `por-rubro` conserva la estructura de origen del formulario; `maestro` aplica los destinos por servicio.

Ejemplo de copia de configuración (los IDs se regeneran y los totales empiezan en cero):

```json
{
  "vigencia": {
    "anio": 2028,
    "nombre": "Presupuesto 2028",
    "fechaInicio": "2028-01-01",
    "fechaFin": "2028-12-31",
    "presupuestoDisponible": 60000000,
    "estado": "BORRADOR"
  },
  "copiarParticipantes": true
}
```

La copia incluye estructura global y propia del año como rubros propios de la nueva vigencia. Si un código ya tiene versión anual, esta prevalece sobre la plantilla global; las solicitudes históricas se conservan. No copia necesidades, distribuciones monetarias, aprobados, ajustes ni historial. El techo de la nueva vigencia proviene del cuerpo enviado. Esta operación está preparada en backend; su formulario de copia queda para una iteración posterior.

Se cargó el catálogo de **Ff.xlsx**, hoja **final**, confirmado por el usuario. La estructura y los mapeos existentes son administrables. Exportación Excel y nómina permanecen fuera del alcance actual.

## Evidencia de validación

`./mvnw.cmd clean verify`: pruebas de integración con migraciones Flyway, autenticación y permisos, categorías con múltiples ítems (50 M solicitados y 36 M aprobados), conservación de originales/historial, reparto de uno/dos/tres servicios, participantes y desactivación lógica, mapeos, sumas jerárquicas y copia anual sin valores. `npm run build` valida TypeScript y genera el frontend.
