# Gestourant Backend

API de autenticación para Gestourant. Requiere Java 17 y MySQL 8.

1. Crea la base de datos: `CREATE DATABASE gestourant CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`
2. Configura, si es necesario, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` y `ADMIN_REGISTRATION_CODE`.
3. Ejecuta: `mvn spring-boot:run`

## Endpoints

- `POST /api/auth/register`: usuario, email y password. La contraseña debe tener mínimo 10 caracteres y al menos un número.
- `POST /api/auth/login`: identifier (correo o usuario) y password.
- `GET /api/tables`: consulta las mesas de la sala.
- `POST /api/tables/{id}/orders`: abre un pedido para una mesa libre.
- `GET /api/tables/{id}/orders/open`: recupera el pedido abierto de una mesa ocupada.
- `POST /api/orders/{id}/items`: agrega un producto al pedido (`productId`, `quantity`).
- `POST /api/orders/{id}/close`: cierra la cuenta (`paymentMethod`: `EFECTIVO` o `DIGITAL`).
- `GET /api/products`: consulta el inventario.
- `GET /api/reports/cash-close`: resumen de caja del día.

El frontend incluye navegación funcional para Mesas, Pedidos, Inventario y Reportes. Para ejecutar ambos módulos, inicia el backend en el puerto `8081` y luego ejecuta `npm install` y `npm run dev` dentro de `Frontend_Gestourant`.

## Arquitectura y logs

El backend usa puertos y adaptadores en los límites de auditoría: `audit/port` contiene los contratos y `audit/adapter` contiene la implementación JPA. Los servicios de aplicación no necesitan conocer cómo se persisten los eventos. Los logs técnicos se escriben en consola y en `logs/gestourant.log`; puedes cambiar la ruta con `LOG_FILE`.

El frontend separa el adaptador HTTP y el logger en `src/infrastructure`. Las vistas llaman al cliente de aplicación y no gestionan tokens ni respuestas HTTP directamente. En desarrollo, los eventos de red, autenticación y errores aparecen en la consola del navegador.

El registro acepta `role` (`EMPLEADO` o `ADMINISTRADOR`) y `adminCode`. El primer usuario es administrador automáticamente. Para registrar administradores adicionales, define `ADMIN_REGISTRATION_CODE` y escribe ese código en el formulario.

## Roles

- `ADMINISTRADOR`: configura la sala y el catálogo. Puede crear, editar, eliminar y unir mesas; también puede crear, editar y eliminar productos.
- `EMPLEADO`: opera la sala. Puede abrir mesas, agregar productos a pedidos, cerrar cuentas, consultar inventario y ver reportes.

En la pantalla **Mesas**, el administrador encuentra el bloque **Configurar sala** para crear o unir mesas. Al seleccionar una mesa puede editarla, eliminarla o separarla. Una mesa ocupada no se puede eliminar ni unir; primero debe cerrarse su pedido.

Ejemplo de registro:
```json
{"username":"maria.lopez","email":"maria@ejemplo.com","password":"ClaveSegura1"}
```
