# Java Books - Sistema de Gestión de Libros con OAuth2

Sistema de gestión de libros con autenticación y autorización OAuth2 usando Spring Security Authorization Server.

## Características

- **Authorization Server** con Authorization Code Flow
- **Resource Server** para la API de libros
- **JWT personalizados** con claims de categorías y autores
- **Autorización granular** por roles, categorías y autores
- **Interfaz web** para autenticación
- **Almacenamiento en memoria** para datos de prueba

## Arquitectura de Seguridad

### Roles y Permisos

- **ADMIN**: Puede crear y eliminar libros (scopes: `libros.read`, `libros.write`)
- **CLIENT**: Solo puede consultar libros (scope: `libros.read`)

### Autorización por Categorías y Autores

Cada usuario tiene acceso limitado a:
- **Categorías específicas**: PROGRAMMING, FRAMEWORKS, ARCHITECTURE
- **Autores específicos**: Lista personalizada por usuario

### JWT Claims Personalizados

```json
{
  "sub": "admin",
  "scope": "libros.read libros.write",
  "roles": ["ADMIN"],
  "categorias": ["PROGRAMMING", "FRAMEWORKS", "ARCHITECTURE"],
  "autores": ["Robert C. Martin", "Joshua Bloch", "..."],
  "iss": "http://localhost:9000",
  "exp": 1703003600,
  "iat": 1703000000
}
```

## Usuarios de Prueba

| Usuario | Contraseña | Rol | Categorías | Autores |
|---------|------------|-----|------------|---------|
| admin | password | ADMIN | Todas | Todos |
| client1 | password | CLIENT | PROGRAMMING | Robert C. Martin, Joshua Bloch |
| client2 | password | CLIENT | FRAMEWORKS, ARCHITECTURE | Craig Walls, Chris Richardson, Eric Evans |

## Endpoints de la API

### Gestión de Libros

- `POST /api/book` - Crear libro (requiere ADMIN + `libros.write`)
- `DELETE /api/book/{id}` - Eliminar libro (requiere ADMIN + `libros.write`)
- `GET /api/book/title/{title}` - Buscar por título (requiere `libros.read`)
- `GET /api/book/author/{author}` - Buscar por autor (requiere `libros.read`)

### OAuth2 Endpoints

- `GET /oauth2/authorize` - Endpoint de autorización
- `POST /oauth2/token` - Intercambio de código por token
- `GET /.well-known/openid_configuration` - Configuración OpenID Connect

## Instalación y Ejecución

### Prerrequisitos

- Java 17+
- Maven 3.6+

### Ejecutar la aplicación

```bash
mvn spring-boot:run
```

La aplicación estará disponible en:
- **Servidor de Autorización**: http://localhost:9000
- **API de Libros**: http://localhost:9000/api/book

## Flujo de Autorización OAuth2

### 1. Obtener Código de Autorización

Acceder en el navegador:
```
http://localhost:9000/oauth2/authorize?response_type=code&client_id=javabooks-client&scope=libros.read%20libros.write&redirect_uri=http://localhost:8080/authorized
```

### 2. Intercambiar Código por Token

```bash
curl -X POST http://localhost:9000/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "Authorization: Basic amF2YWJvb2tzLWNsaWVudDpzZWNyZXQ=" \
  -d "grant_type=authorization_code&code=CODIGO_OBTENIDO&redirect_uri=http://localhost:8080/authorized&client_id=javabooks-client"
```

### 3. Usar Token para Acceder a la API

```bash
curl -H "Authorization: Bearer TOKEN_JWT" \
  http://localhost:9000/api/book/title/Clean%20Code
```

## Ejemplos de Uso

### Crear un Libro (como ADMIN)

```bash
curl -X POST http://localhost:9000/api/book \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN_JWT" \
  -d '{
    "titulo": "Spring Boot in Action",
    "autor": "Craig Walls",
    "cantidadPaginas": 472,
    "categoria": "FRAMEWORKS",
    "contenido": "Contenido del libro"
  }'
```

### Buscar Libros por Autor

```bash
curl -H "Authorization: Bearer TOKEN_JWT" \
  "http://localhost:9000/api/book/author/Robert%20C.%20Martin"
```

## Libros de Prueba Precargados

- Clean Code (Robert C. Martin) - PROGRAMMING
- Effective Java (Joshua Bloch) - PROGRAMMING
- The Pragmatic Programmer (David Thomas) - PROGRAMMING
- Design Patterns (Gang of Four) - PROGRAMMING
- Spring in Action (Craig Walls) - FRAMEWORKS
- Microservices Patterns (Chris Richardson) - ARCHITECTURE
- Domain-Driven Design (Eric Evans) - ARCHITECTURE

## Validaciones de Seguridad

### Por Scope
- `libros.read`: Requerido para todas las operaciones GET
- `libros.write`: Requerido para operaciones POST y DELETE

### Por Rol
- Solo usuarios con rol `ADMIN` pueden crear/eliminar libros
- Usuarios `CLIENT` solo pueden realizar consultas

### Por Categoría
- Los usuarios solo pueden acceder a libros de sus categorías asignadas

### Por Autor
- Los usuarios solo pueden acceder a libros de sus autores asignados

## Estructura del Proyecto

```
src/main/java/com/javabooks/
├── config/                 # Configuraciones de seguridad
├── controller/            # Controladores REST y web
├── model/                 # Modelos de datos
├── repository/            # Repositorios en memoria
├── security/              # Componentes de seguridad personalizados
├── service/               # Servicios de negocio
└── JavaBooksApplication.java
```

## Tecnologías Utilizadas

- **Spring Boot 3.2.0**
- **Spring Security 6**
- **Spring Authorization Server**
- **Spring OAuth2 Resource Server**
- **Thymeleaf** (para páginas web)
- **JWT/JWK** (para tokens)
- **Bootstrap** (para UI)

## Consideraciones de Seguridad

1. **Tokens JWT firmados** con claves RSA
2. **Validación de scopes** en cada endpoint
3. **Autorización granular** por categorías y autores
4. **Separación clara** entre Authorization Server y Resource Server
5. **Claims personalizados** para control de acceso detallado

## Posibles Extensiones

- Persistencia en base de datos
- Refresh tokens
- Client credentials flow
- Rate limiting
- Audit logging
- Interfaz web completa para gestión de libros