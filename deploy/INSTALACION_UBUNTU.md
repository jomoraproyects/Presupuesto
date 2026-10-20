# Presupuesto en la VM Ubuntu de Serviciudad

Este aplicativo se instala de manera independiente de `seleccion`: otro repositorio,
otro servicio systemd, otra base MySQL y otro sitio web. El codigo y el frontend
compilado quedan en `/home/serviciudad/Escritorio/presupuesto`. El archivo de
configuracion privada `.env.production` tambien queda ahi, excluido de Git.
MySQL administra sus archivos de datos fuera de la carpeta del proyecto.

## Datos por confirmar antes de ejecutar

- Acceso SSH a `serviciudad@192.168.100.251` (ahora el servicio SSH esta inactivo).
- Que los puertos 8081 y 443 esten disponibles y el nombre DNS interno elegido.
- Ubicacion del certificado HTTPS y configuracion Apache actual de `seleccion`.
- Estado y version de MySQL y destino externo para respaldos.

La ruta y el usuario ya estan confirmados. El dominio de ejemplo
`presupuesto.interno.empresa.com` aun debe sustituirse por el real.

## Instalacion inicial

1. Instalar Java 21 y Node compatible con Vite (20.19+ o 22.12+). En la VM se
   encontro MySQL activo, Java ausente, Node 18.19.1 y SSH inactivo. Apache
   ocupa 80/443 y sirve `seleccion`; Nginx esta inactivo. Revisar primero los
   sitios Apache existentes; no activar Nginx ni cambiar Node global.
2. Clonar el repositorio como el usuario que ejecutara el servicio:

   ```bash
   git clone --branch codex/fase-3-frontend https://github.com/jomoraproyects/Presupuesto.git /home/serviciudad/Escritorio/presupuesto
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
4. Copiar `deploy/presupuesto.env.example` a `.env.production` en la raiz del
   proyecto, completar sus secretos y restringirlo con `chmod 600`. Este archivo
   esta excluido de Git. `ADMIN_PASSWORD` requiere 12 o mas
   caracteres y solo crea `admin` si aun no existe. No guardar claves en el repo.
5. Compilar desde el directorio del repositorio:

   ```bash
   ./mvnw clean verify
   cd frontend && npm ci && npm run build
   ```

   No definir `VITE_API_URL` con `localhost`: el frontend debe llamar a `/api`
   bajo el mismo dominio. `frontend/.env.example` ya usa esa ruta.
6. Instalar `deploy/presupuesto.service.example` como
   `/etc/systemd/system/presupuesto.service` y activar:

   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable --now presupuesto
   sudo systemctl status presupuesto
   sudo journalctl -u presupuesto -n 100 --no-pager
   ```

   El backend escucha solo en `127.0.0.1:8081`; no publicar ese puerto en la red.
7. Ajustar el sitio de ejemplo de Apache al DNS y certificado existentes. La ruta
   `/api/` va al backend; el resto sirve `frontend/dist` directamente desde la
   carpeta del proyecto. Dar al usuario `www-data` permiso de atravesar las
   carpetas superiores y leer `dist`, sin abrir el resto del hogar:

   ```bash
   sudo setfacl -m u:www-data:--x /home/serviciudad /home/serviciudad/Escritorio /home/serviciudad/Escritorio/presupuesto /home/serviciudad/Escritorio/presupuesto/frontend
   sudo setfacl -R -m u:www-data:rX /home/serviciudad/Escritorio/presupuesto/frontend/dist
   sudo setfacl -d -m u:www-data:rX /home/serviciudad/Escritorio/presupuesto/frontend/dist
   ```

   Si `setfacl` no existe, instalar el paquete `acl`. Antes de recargar Apache,
   ejecutar `sudo apache2ctl configtest`. No modificar el sitio de `seleccion`.
8. Entrar por HTTPS, crear los usuarios de cada area desde SUPER_ADMIN y probar
   el flujo de presupuesto. Revisar `systemctl status presupuesto mysql apache2`.

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

4. Verificar que `www-data` puede leer el nuevo `frontend/dist`, reiniciar
   `presupuesto` y comprobar el sitio y el login:

   ```bash
   sudo systemctl restart presupuesto
   sudo systemctl status presupuesto
   sudo apache2ctl configtest
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
