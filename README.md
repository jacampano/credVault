# CredVault (Spring Boot)

Herramienta web colaborativa para gestionar credenciales, similar a SysPass, con:

- Spring Boot + Hibernate/JPA
- Cifrado de campos sensibles en base de datos (AES-GCM)
- Clave de cifrado por instancia (`APP_ENCRYPTION_KEY`)
- Autenticación configurable por entorno (`local` u `oauth`)
- Roles de autorización: `APP_USER` y `ADMIN`
- Perfil local con H2 embebida
- Perfil `prod` para PostgreSQL/MySQL/Oracle (o cualquier BBDD compatible con Hibernate)
- Dockerfile y `docker-compose` de ejemplo

## Requisitos

- Java 21
- Maven 3.9+

## Clave de cifrado de instancia

La aplicación exige una clave AES en Base64 en la variable de entorno `APP_ENCRYPTION_KEY`.

Generar una clave de 32 bytes:

```bash
openssl rand -base64 32
```

## Ejecución local (H2)

```bash
export APP_ENCRYPTION_KEY="<TU_CLAVE_BASE64>"
export APP_AUTH_MODE="local"
export APP_AUTH_LOCAL_ADMIN_USERNAME="admin"
export APP_AUTH_LOCAL_ADMIN_PASSWORD="<PASSWORD_SEGURO>"
mvn spring-boot:run
```

- URL app: `http://localhost:8080`
- H2 console: `http://localhost:8080/h2-console`

En modo `local`, los usuarios se almacenan en la misma BBDD de la aplicación (`app_users`).
Si defines `APP_AUTH_LOCAL_ADMIN_USERNAME` y `APP_AUTH_LOCAL_ADMIN_PASSWORD`, se crea ese usuario con roles `APP_USER` y `ADMIN` en el primer arranque.

## Autenticación y autorización

Variable principal:

- `APP_AUTH_MODE=local|oauth`
- La configuración se puede guardar en panel de administración: `/admin/authentication`
- Precedencia de configuración: `Variables de entorno > Configuración guardada en BBDD > Valor por defecto (local)`
- Tras cambiar el modo o proveedor en el panel, reinicia la aplicación para aplicar completamente el cambio.

Roles:

- `ROLE_APP_USER`: acceso de consulta a la aplicación.
- `ROLE_ADMIN`: gestión de credenciales (crear/editar/eliminar).

Permisos por ruta:

- `GET /credentials` y `/`: `APP_USER` o `ADMIN`
- `GET /profile`: `APP_USER` o `ADMIN`
- `GET /admin`: `ADMIN`
- `POST /credentials`, edición y borrado: `ADMIN`

Navegación de cabecera:

- En la esquina superior derecha se muestra el nombre del usuario autenticado.
- Al pulsar el nombre se accede a `/profile`.
- Si el usuario tiene rol de administración, se muestra además el icono de engranaje con enlace a `/admin`.

Gestión de usuarios (`/admin/users`):

- Listado paginado de usuarios.
- Ordenación por columnas: `username`, `firstName`, `lastName`, `email`, `enabled`.
- Búsqueda global por texto en: `username`, `firstName`, `lastName`, `email`.
- En modo `local`, botón `Nuevo usuario` para alta manual con perfil completo y roles.
- Acciones por usuario: `Editar`, `Activar/Desactivar`, `Eliminar usuario`.

Perfil de usuario (`/profile`):

- Se muestran: `Nombre de usuario`, `Nombre completo`, `Email`.
- En modo `local`: `Nombre completo` y `Email` son editables y se guardan en BBDD.
- En modo `oauth`: los datos se muestran en solo lectura y se actualizan con la información recibida del proveedor OAuth.

### Modo OAuth

Para OAuth puedes configurar desde `/admin/authentication` o por variables de entorno.
Si defines variables de entorno, tienen prioridad sobre lo guardado en el panel.

Variables OAuth soportadas:

- `APP_AUTH_OAUTH_CLIENT_ID`
- `APP_AUTH_OAUTH_CLIENT_SECRET`
- `APP_AUTH_OAUTH_AUTHORIZATION_URI`
- `APP_AUTH_OAUTH_TOKEN_URI`
- `APP_AUTH_OAUTH_USER_INFO_URI`
- `APP_AUTH_OAUTH_USER_NAME_ATTRIBUTE`
- `APP_AUTH_OAUTH_SCOPES` (CSV, por ejemplo: `profile,email,read_user,read_api`)
- `APP_AUTH_OAUTH_REDIRECT_URI`
- `APP_AUTH_OAUTH_ADMIN_GROUPS` (CSV de grupos GitLab que serán `ADMIN`)

URL de retorno OAuth implementada:

- Endpoint de callback: `/oauth2/callback/{registrationId}`
- `registrationId` usado por la aplicación: `credvault`
- Valor recomendado de redirect URI: `{baseUrl}/oauth2/callback/{registrationId}`
- Ejemplo en local: `http://localhost:8080/oauth2/callback/credvault`

Ejemplo:

```bash
export APP_AUTH_MODE="oauth"
export APP_AUTH_OAUTH_CLIENT_ID="mi-client-id"
export APP_AUTH_OAUTH_CLIENT_SECRET="mi-client-secret"
export APP_AUTH_OAUTH_AUTHORIZATION_URI="https://idp.example.com/oauth2/authorize"
export APP_AUTH_OAUTH_TOKEN_URI="https://idp.example.com/oauth2/token"
export APP_AUTH_OAUTH_USER_INFO_URI="https://idp.example.com/oauth2/userinfo"
export APP_AUTH_OAUTH_USER_NAME_ATTRIBUTE="email"
export APP_AUTH_OAUTH_SCOPES="profile,email,read_user,read_api"
export APP_AUTH_OAUTH_REDIRECT_URI="{baseUrl}/oauth2/callback/{registrationId}"
export APP_AUTH_OAUTH_ADMIN_GROUPS="credvault-admin,secops-admin"
```

## Ejecución en producción (perfil prod)

Variables mínimas:

- `SPRING_PROFILES_ACTIVE=prod`
- `APP_ENCRYPTION_KEY`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `DB_DRIVER` (opcional)
- `HIBERNATE_DIALECT` (opcional)
- `APP_AUTH_MODE` (`local` u `oauth`)

### Ejemplos

PostgreSQL:

```bash
export SPRING_PROFILES_ACTIVE=prod
export APP_ENCRYPTION_KEY="<TU_CLAVE_BASE64>"
export DB_URL="jdbc:postgresql://localhost:5432/credvault"
export DB_USERNAME="credvault"
export DB_PASSWORD="credvault"
export DB_DRIVER="org.postgresql.Driver"
export HIBERNATE_DIALECT="org.hibernate.dialect.PostgreSQLDialect"
```

MySQL:

```bash
export DB_URL="jdbc:mysql://localhost:3306/credvault"
export DB_DRIVER="com.mysql.cj.jdbc.Driver"
export HIBERNATE_DIALECT="org.hibernate.dialect.MySQLDialect"
```

Oracle:

```bash
export DB_URL="jdbc:oracle:thin:@//localhost:1521/XEPDB1"
export DB_DRIVER="oracle.jdbc.OracleDriver"
export HIBERNATE_DIALECT="org.hibernate.dialect.OracleDialect"
```

## Docker

Build:

```bash
docker build -t credvault:latest .
```

Run:

```bash
docker run --rm -p 8080:8080 \
  -e APP_ENCRYPTION_KEY="<TU_CLAVE_BASE64>" \
  -e APP_AUTH_MODE="local" \
  -e APP_AUTH_LOCAL_ADMIN_USERNAME="admin" \
  -e APP_AUTH_LOCAL_ADMIN_PASSWORD="<PASSWORD_SEGURO>" \
  credvault:latest
```

## Docker Compose (con PostgreSQL)

```bash
export APP_ENCRYPTION_KEY="<TU_CLAVE_BASE64>"
docker compose up --build
```

## Pruebas

```bash
mvn test
```
