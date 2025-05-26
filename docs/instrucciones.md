# Instrucciones para Usar el OAuth2 Authorization Server

## 1. Ejecutar la Aplicación

```bash
mvn spring-boot:run
```

La aplicación se ejecutará en `http://localhost:9000`

## 2. Usuarios Configurados

- **Usuario normal**: `user` / `password`
- **Administrador**: `admin` / `admin`

## 3. Clientes OAuth2 Configurados

### Cliente OIDC
- **Client ID**: `oidc-client`
- **Client Secret**: `secret`
- **Redirect URIs**: 
  - `http://127.0.0.1:8080/login/oauth2/code/oidc-client`
  - `http://127.0.0.1:8080/authorized`

### Cliente Web
- **Client ID**: `web-client`
- **Client Secret**: `web-secret`
- **Redirect URIs**:
  - `http://localhost:3000/callback`
  - `http://localhost:8081/callback`

## 4. Probar el Authorization Code Flow

### Paso 1: Iniciar el flujo de autorización

Navega a esta URL en tu navegador:

```
http://localhost:9000/oauth2/authorize?response_type=code&client_id=web-client&redirect_uri=http://localhost:8081/callback&scope=libros.read+libros.write&state=xyz&code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM&code_challenge_method=S256
```

### Paso 2: Autenticarse

- Serás redirigido a `/login`
- Ingresa credenciales: `user` / `password` o `admin` / `admin`

### Paso 3: Autorizar

- Verás la página de consentimiento
- Autoriza los scopes solicitados

### Paso 4: Obtener el código

- Serás redirigido a tu `redirect_uri` con un código de autorización
- URL de ejemplo: `http://localhost:8081/callback?code=ABC123&state=xyz`

### Paso 5: Intercambiar código por tokens

Usa curl o Postman:

```bash
curl -X POST 'http://localhost:9000/oauth2/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'Authorization: Basic d2ViLWNsaWVudDp3ZWItc2VjcmV0' \
  -d 'grant_type=authorization_code&code=TU_CODIGO_AQUI&redirect_uri=http://localhost:8081/callback&code_verifier=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk'
```

**Nota**: El header Authorization contiene `web-client:web-secret` en Base64.

## 5. Endpoints Disponibles

### Endpoints OAuth2
- `GET /oauth2/authorize` - Autorización
- `POST /oauth2/token` - Obtener tokens
- `GET /oauth2/jwks` - JSON Web Key Set
- `POST /oauth2/introspect` - Validar tokens
- `POST /oauth2/revoke` - Revocar tokens

### Endpoints OpenID Connect
- `GET /userinfo` - Información del usuario
- `GET /.well-known/openid_configuration` - Configuración OIDC

### Endpoints de Prueba
- `GET /api/info` - Información del servidor
- `GET /api/public` - Endpoint público
- `GET /api/protected` - Endpoint protegido
- `GET /api/admin` - Solo administradores

## 6. Probar con Postman

### Configurar OAuth2 en Postman:

1. **Authorization Type**: OAuth 2.0
2. **Grant Type**: Authorization Code (With PKCE)
3. **Callback URL**: `http://localhost:8081/callback`
4. **Auth URL**: `http://localhost:9000/oauth2/authorize`
5. **Access Token URL**: `http://localhost:9000/oauth2/token`
6. **Client ID**: `web-client`
7. **Client Secret**: `web-secret`
8. **Scope**: `read write`
9. **State**: `xyz`

## 7. Validar Tokens

### Usando el endpoint de introspección:

```bash
curl -X POST 'http://localhost:9000/oauth2/introspect' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'Authorization: Basic d2ViLWNsaWVudDp3ZWItc2VjcmV0' \
  -d 'token=TU_ACCESS_TOKEN_AQUI'
```

## 8. Obtener Información del Usuario

```bash
curl -X GET 'http://localhost:9000/userinfo' \
  -H 'Authorization: Bearer TU_ACCESS_TOKEN_AQUI'
```

## 9. Base de Datos H2

Puedes acceder a la consola H2 en:
- URL: `http://localhost:9000/h2-console`
- JDBC URL: `jdbc:h2:mem:testdb`
- Usuario: `sa`
- Password: `password`

## 10. Logs Útiles

La aplicación está configurada para mostrar logs detallados de:
- Spring Security
- OAuth2
- SQL queries
- Token generation/validation

## Consideraciones de Seguridad

1. **PKCE habilitado** - Para clientes públicos
2. **HTTPS requerido** - En producción
3. **Scopes granulares** - Control de permisos
4. **Token expiration** - Tokens de corta duración
5. **Refresh tokens** - Para renovación segura

## Próximos Pasos

1. Implementar persistencia de clientes en base de datos
2. Agregar custom scopes y claims
3. Implementar logout
4. Agregar rate limiting
5. Configurar HTTPS para producción
