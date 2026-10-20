# Presupuesto en el escritorio de la VM Ubuntu

Presupuesto se instala en `/home/serviciudad/Escritorio/presupuesto`, sin tocar
el proyecto `seleccion` ni sus archivos de Apache. Spring Boot sirve la API y el
frontend compilado desde esa misma carpeta por el puerto 8081. Usa una base
MySQL nueva llamada `presupuesto`; MySQL administra sus propios archivos de
datos fuera de la carpeta del proyecto. El archivo de arranque de systemd
tambien debe quedar en `/etc/systemd/system`, como requiere Ubuntu.

Direccion inicial para pruebas en la red interna:
`http://192.168.100.251:8081/login`. Antes de usar datos reales, configurar
HTTPS en el puerto propio de Presupuesto, sin modificar Seleccion.

## Estado comprobado de la VM

- Usuario `serviciudad`, IP `192.168.100.251`, Escritorio existente.
- MySQL activo; Apache y Seleccion activos en 80/443; no se modificaran.
- SSH habilitado para administrar esta VM desde otro PC.
- JDK 21 Temurin instalado dentro de `.local/jdk/runtime`.
- Node 24 instalado dentro de `.local/node/runtime` para compilar el frontend,
  sin reemplazar el Node global.

## Instalacion inicial

1. Habilitar SSH y acceso por clave publica. Confirmar que se puede entrar como
   `serviciudad@192.168.100.251` antes de seguir.
2. Comprobar que 8081 este libre, espacio en disco y acceso a Internet para
   Maven y npm. Instalar JDK 21 y Node compatible dentro de `.local`.
3. Clonar el codigo en la carpeta elegida:

   ```bash
   git clone --branch main https://github.com/jomoraproyects/Presupuesto.git /home/serviciudad/Escritorio/presupuesto
   ```

   `main` es la rama que se despliega y actualiza en la VM.
4. En MySQL crear **solo** la base y el usuario de Presupuesto:

   ```sql
   CREATE DATABASE presupuesto CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
   CREATE USER 'presupuesto_app'@'localhost' IDENTIFIED BY '<clave-nueva>';
   GRANT ALL PRIVILEGES ON presupuesto.* TO 'presupuesto_app'@'localhost';
   ```

   Flyway necesita estos permisos para crear las tablas y los catalogos al
   primer arranque. La base H2 del PC Windows no se traslada automaticamente.
5. Copiar `deploy/presupuesto.env.example` como `.env.production` dentro del
   proyecto. Completar valores y ejecutar `chmod 600 .env.production`. Git
   ignora este archivo. `ADMIN_PASSWORD` necesita 11 caracteres o mas y solo
   crea el usuario inicial cuando la base esta vacia.
6. Compilar backend y frontend desde la carpeta del proyecto:

   ```bash
   JAVA_HOME="$PWD/.local/jdk/runtime" sh ./mvnw clean verify
   cd frontend
   PATH="/home/serviciudad/Escritorio/presupuesto/.local/node/runtime/bin:$PATH" npm ci
   PATH="/home/serviciudad/Escritorio/presupuesto/.local/node/runtime/bin:$PATH" npm run build
   ```

   `frontend/dist` queda dentro de `presupuesto`. El backend lo sirve usando
   `FRONTEND_STATIC_LOCATION`. El frontend usa `/api` del mismo origen.
7. Instalar `deploy/presupuesto.service.example` como
   `/etc/systemd/system/presupuesto.service`, sin tocar el servicio `seleccion`:

   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable --now presupuesto
   sudo systemctl status presupuesto
   sudo journalctl -u presupuesto -n 100 --no-pager
   ```

8. Permitir TCP 8081 **solo desde la red interna** si el firewall esta activo.
   Probar `/login`, iniciar sesion y crear los usuarios de las areas desde
   SUPER_ADMIN. No abrir el puerto 3306 de MySQL a la red.

## Actualizaciones desde GitHub

1. En Windows: probar, hacer commit y push a la rama desplegada.
2. Antes de actualizar en Ubuntu: respaldar MySQL `presupuesto` fuera de la
   VM y comprobar que `git status --short` este limpio.
3. Dentro de `/home/serviciudad/Escritorio/presupuesto`:

   ```bash
   git pull --ff-only origin main
   JAVA_HOME="$PWD/.local/jdk/runtime" sh ./mvnw clean verify
   cd frontend
   PATH="/home/serviciudad/Escritorio/presupuesto/.local/node/runtime/bin:$PATH" npm ci
   PATH="/home/serviciudad/Escritorio/presupuesto/.local/node/runtime/bin:$PATH" npm run build
   sudo systemctl restart presupuesto
   ```

   Git cambia codigo, no borra los presupuestos en MySQL. Flyway aplica nuevas
   migraciones al arrancar. No ejecutar el perfil `local` en el servidor: usa H2.

## Respaldo y traslado futuro

Mantener una copia automatica y comprobada de la base MySQL en otro equipo.
Para pasar a un VPS, restaurar esa copia en MySQL del VPS y desplegar el mismo
codigo con nuevas variables de conexion. GitHub contiene codigo, no datos ni
secretos.
