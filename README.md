# Gestourant Backend

API de autenticación para Gestourant. Requiere Java 17 y MySQL 8.

1. Crea la base de datos: `CREATE DATABASE gestourant CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`
2. Configura, si es necesario, `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET`.
3. Ejecuta: `mvn spring-boot:run`

## Endpoints

- `POST /api/auth/register`: usuario, email y password. La contraseña debe tener mínimo 10 caracteres y al menos un número.
- `POST /api/auth/login`: identifier (correo o usuario) y password.

Ejemplo de registro:
```json
{"username":"maria.lopez","email":"maria@ejemplo.com","password":"ClaveSegura1"}
```
