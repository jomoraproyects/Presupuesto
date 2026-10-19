# Presupuesto en la VM Ubuntu de Serviciudad

Este aplicativo se instala de manera independiente de `seleccion`: otro repositorio,
otro servicio systemd, otra base MySQL y otro sitio de Nginx. El codigo puede estar
en el Escritorio del usuario de Ubuntu. Nginx recibe solo la copia compilada de React.

## Datos por confirmar antes de ejecutar

- Usuario de Ubuntu y ruta real de su Escritorio (`Escritorio`, `Desktop` u otra).
- IP/nombre de la VM, acceso SSH y rama de Git que se publicara.
- Que los puertos 8081 y 443 esten disponibles y el nombre DNS interno elegido.
- Ubicacion del certificado HTTPS y configuracion actual de Nginx.
- Estado y version de MySQL y destino externo para respaldos.

Los ejemplos usan `/home/USUARIO_UBUNTU/Escritorio/Presupuesto` y
`presupuesto.interno.empresa.com`. Sustituirlos por los valores comprobados.

## Instalacion inicial

1. Comprobar Java 21, MySQL 8.x, Git, Nginx y Node compatible con Vite (20.19+
   o 22.12+). No instalar ni cambiar servicios sin revisar los existentes.
2. Clonar el repositorio como el usuario que ejecutara el servicio:

   ```bash
   git clone --branch codex/fase-3-frontend https://github.com/jomoraproyects/Presupuesto.git /home/USUARIO_UBUNTU/Escritorio/Presupuesto
   ```

   La rama actual publicada es `codex/fase-3-frontend`; `main` aun tiene la
   version inicial. Al integrar la rama, cambiar el seguimiento a `main`.
3. Crear **solo** la base nueva en MySQL y su usuario; no modificar `seleccion`:

   ```sql
   CREATE DATABASE presupuesto CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
   CREATE USER 'presupuesto_app'@'localhost' IDENTIFIED BY '<clave-nueva>';
   GRANT ALL PRIVILEGES ON presupuesto.* TO 'presupuesto_app'@'localhost';
   ```

   Flyway necesita permisos de estructura para las migraciones. En la primera
   ejecucion crea las tablas y carga los catalogos; no importa los datos H2 del PC.
4. Crear `/etc/presupuesto`, copiar `deploy/presupuesto.env.example` a
   `/etc/presupuesto/presupuesto.env`, completar sus secretos fuera de Git y
   restringirlo con `sudo chmod 600`. `ADMIN_PASSWORD` requiere 12 o mas
   caracteres y solo crea `admin` si aun no existe. No guardar claves en el repo.
5. Compilar desde el directorio del repositorio:

   ```bash
   ./mvnw clean verify
   cd frontend && npm ci && npm run build
   ```

   No definir `VITE_API_URL` con `localhost`: el frontend debe llamar a `/api`
   bajo el mismo dominio. `frontend/.env.example` ya usa esa ruta.
6. Ajustar `deploy/presupuesto.service.example` a la ruta y usuario reales,
   instalarlo como `/etc/systemd/system/presupuesto.service` y activar:

   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable --now presupuesto
   sudo systemctl status presupuesto
   sudo journalctl -u presupuesto -n 100 --no-pager
   ```

   El backend escucha solo en `127.0.0.1:8081`; no publicar ese puerto en la red.
7. Crear `/var/www/presupuesto-web` para los archivos publicos y copiar alli el
   contenido de `frontend/dist` (por ejemplo, `sudo cp -a frontend/dist/. /var/www/presupuesto-web/`
   desde la raiz del repositorio). Ajustar el sitio de ejemplo de Nginx al DNS y
   certificado existentes. La ruta `/api/` va al backend; el resto sirve React.
   Antes de recargar, ejecutar `sudo nginx -t`. No modificar el sitio de `seleccion`.
8. Entrar por HTTPS, crear los usuarios de cada area desde SUPER_ADMIN y probar
   el flujo de presupuesto. Revisar `systemctl status presupuesto mysql nginx`.

## Actualizaciones desde GitHub

1. En Windows: probar, hacer commit y push a la rama desplegada.
2. En Ubuntu: hacer respaldo de la base `presupuesto` **fuera de la VM** y verificar
   que el repositorio no tenga cambios locales (`git status --short`).
3. En la carpeta del repositorio:

   ```bash
   git pull --ff-only origin codex/fase-3-frontend
   ./mvnw clean verify
   cd frontend && npm ci && npm run build
   ```

4. Copiar la nueva carpeta `frontend/dist` al directorio exclusivo
   `/var/www/presupuesto-web`, reiniciar `presupuesto` y comprobar Nginx y login:

   ```bash
   sudo systemctl restart presupuesto
   sudo systemctl status presupuesto
   sudo nginx -t
   ```

   `git pull` no cambia los presupuestos guardados en MySQL. Flyway aplica las
   migraciones nuevas al arrancar el backend. No ejecutar el perfil `local` en
   el servidor: ese perfil usa H2, no la base de produccion.

## Respaldo y traslado a otro servidor

Respaldar la base MySQL con la herramienta elegida por el administrador (por
ejemplo `mysqldump`), guardar la copia en otro equipo y probar una restauracion
en una base aislada. Para pasar a un VPS se restaura esa copia en MySQL y se
configura el mismo codigo con nuevas variables de conexion. GitHub contiene
codigo, no presupuestos ni claves.
