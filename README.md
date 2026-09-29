# Presupuesto Serviciudad E.S.P. — fase 1

Backend para administrar catálogos y preparar la formulación del presupuesto anual. Esta fase incluye autenticación JWT, roles, áreas, servicios, vigencias y rubros jerárquicos. Las necesidades, distribuciones por servicio, historial, nómina y frontend pertenecen a fases posteriores.

## Tecnologías y arquitectura

Java 21, Spring Boot 3.5, Maven, Spring Web, Security, Data JPA, Bean Validation, Flyway, MySQL, Lombok y JJWT. El código se organiza en `controller` (HTTP y validación), `dto` (contratos API), `service` (reglas y transacciones), `repository` (persistencia), `entity`, `security`, `config` y `exception`. Los controladores no exponen entidades JPA.

El esquema lo crea Flyway (`src/main/resources/db/migration`). Hibernate usa `ddl-auto: validate`: comprueba el mapeo, pero no modifica la base de datos. Se usan IDs `Long`, `BigDecimal` para dinero y fechas Java Time.

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

La prueba de integración usa H2 en modo MySQL para validar la aplicación, las migraciones, los mapeos JPA, el login, el acceso por rol y el árbol. El despliegue real usa MySQL; se recomienda ejecutar el arranque contra la instancia MySQL del entorno antes de ponerlo en servicio.

## API inicial

Todas las rutas excepto `POST /api/auth/login` requieren `Authorization: Bearer <token>`. Los cuatro roles pueden consultar catálogos; solo `SUPER_ADMIN` puede modificarlos en esta fase. `PRESUPUESTO` tendrá sus operaciones de revisión en la fase de necesidades. La administración de usuarios mediante API no está incluida todavía.

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
