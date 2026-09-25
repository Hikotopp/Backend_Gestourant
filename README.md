# Gestourant Backend

API de autenticación para Gestourant. Requiere Java 17 y MySQL 8.

1. Crea la base de datos: `CREATE DATABASE gestourant CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`
2. Configura, si es necesario, `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET`.
3. Ejecuta: `mvn spring-boot:run`

## Endpoints

- `POST /api/auth/register`: usuario, email y password. La contraseña debe tener mínimo 10 caracteres y al menos un número.
- `POST /api/auth/login`: identifier (correo o usuario) y password.
- `GET /api/auth/config`: indica si Turnstile y proveedores OAuth están configurados.
- `POST /api/auth/oauth/exchange`: canjea un código de inicio OAuth de un solo uso y 60 segundos por un JWT.
- `POST /api/auth/oauth/register`: completa el alta OAuth usando el código temporal recibido después de autenticar con el proveedor y registra el consentimiento expreso antes de crear la cuenta.
- `GET /api/tables`: consulta las mesas de la sala.
- `PATCH /api/tables/{id}/position`: guarda la ubicación del croquis (solo administrador; coordenadas porcentuales `x` y `y`).
- `GET /api/guest/{token}`: carta de la mesa asociada al QR; no devuelve la cuenta compartida ni el historial de pedidos.
- `GET /api/guest/{token}/requests`: solicitudes de la pestaña actual; requiere el encabezado `X-Guest-Session`.
- `POST /api/guest/{token}/requests`: envía productos e indicaciones para que el equipo los revise; requiere el encabezado `X-Guest-Session`.
- `POST /api/guest/{token}/bill-request`: avisa al equipo que el cliente quiere pagar en caja.
- `GET /api/guest-requests`: solicitudes pendientes del equipo autenticado.
- `POST /api/guest-requests/{id}/approve` y `/reject`: confirma y agrega el pedido abierto a la cuenta, o rechaza la solicitud.
- `GET /api/kitchen/requests`: lista comandas confirmadas pendientes de preparación.
- `PATCH /api/kitchen/requests/{id}/advance`: avanza la comanda de `EN_COCINA` a `PREPARANDO` y luego a `LISTO`.
- `POST /api/tables/{id}/orders`: abre un pedido para una mesa libre.
- `GET /api/tables/{id}/orders/open`: recupera el pedido abierto de una mesa ocupada.
- `POST /api/orders/{id}/items`: agrega un producto al pedido (`productId`, `quantity`).
- `PATCH /api/orders/{orderId}/items/{itemId}`: guarda una indicación por producto (`removedIngredients`, hasta 500 caracteres; por ejemplo, `"maní"`).
- `DELETE /api/orders/{orderId}/items/{itemId}`: retira un producto del pedido y devuelve su cantidad al inventario.
- `POST /api/orders/{id}/close`: cierra la cuenta (`paymentMethod`: `EFECTIVO` o `DIGITAL`).
- `GET /api/products`: consulta el inventario.
- `POST /api/products` y `PUT /api/products/{id}`: administra productos con `name`, `description`, `category` (`PLATO`, `BEBIDA` u `OTRO`), `price`, `stock` y `active`.
- `GET /api/reports/cash-close`: resumen de caja del día.

El croquis guarda las posiciones de las mesas y el administrador puede acomodarlas con arrastre. Cada mesa tiene un QR imprimible generado localmente en el navegador. El portal QR muestra la carta, pero no expone la cuenta compartida ni los pedidos de otros clientes. Cada pestaña crea una clave aleatoria de sesión; el backend guarda únicamente su hash y solo devuelve las solicitudes vinculadas a esa pestaña. Al enviar solicitudes y notas de ingredientes, quedan pendientes hasta que un empleado las revise y confirme. Al aprobarlas se agregan a la cuenta, se descuenta el inventario y aparecen como comanda en **Cocina**; el equipo puede actualizarla de `EN_COCINA` a `PREPARANDO` y `LISTO`. Para consultar la cuenta compartida o pedirla, el cliente debe hablar con el equipo; el pago se recibe y registra en caja como efectivo o digital después de verificarlo. Los estados del pedido y la sala se actualizan cada cinco segundos.

Para ejecutar ambos módulos, inicia el backend en el puerto `8081` y luego ejecuta `npm install` y `npm run dev` dentro de `Frontend_Gestourant`.

## Inicio de sesión y protección

Los accesos Google y Microsoft se habilitan al definir `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `MICROSOFT_CLIENT_ID` y `MICROSOFT_CLIENT_SECRET`. Registra como URI de redirección en ambos proveedores:

- Google: `http://localhost:8081/login/oauth2/code/google`
- Microsoft: `http://localhost:8081/login/oauth2/code/microsoft`

El backend también valida la firma de los ID tokens usando las claves públicas JWK de cada proveedor.

En producción usa la URL pública del backend y define `FRONTEND_URL` con la URL pública del frontend. Las cuentas creadas mediante OAuth se identifican por proveedor e ID de usuario; la primera cuenta conserva el rol administrador inicial y las siguientes reciben rol empleado. Un correo que ya tenga cuenta local no se vincula automáticamente.

Cloudflare Turnstile se activa configurando juntas `TURNSTILE_SITE_KEY` y `TURNSTILE_SECRET_KEY`. Si solo se configura una, el backend no inicia. Añade el dominio del frontend en la configuración de Turnstile. El servidor valida cada token de CAPTCHA; el honeypot de los formularios se valida también en el backend.

La creación de cuentas locales y el alta OAuth requieren autorización explícita para el tratamiento de datos; se guarda la fecha/hora de autorización en la cuenta. OAuth permite que una identidad previamente vinculada inicie sesión directamente; si es una identidad nueva, primero se autentica con el proveedor y después se solicita el consentimiento antes de crear la cuenta. El código de registro pendiente vence a los cinco minutos y solo se puede usar una vez. La sesión de la interfaz se bloquea tras 2 minutos y 30 segundos sin actividad y se cierra tras 5 minutos desde la última actividad. La política de datos publicada en `Frontend_Gestourant/public/politica-tratamiento.html` es un borrador: completa los datos del responsable y sométela a revisión jurídica antes de usarla con clientes.

## Arquitectura y logs

El backend usa puertos y adaptadores en los límites de auditoría: `audit/port` contiene los contratos y `audit/adapter` contiene la implementación JPA. Los servicios de aplicación no necesitan conocer cómo se persisten los eventos. Los logs técnicos se escriben en consola y en `logs/gestourant.log`; puedes cambiar la ruta con `LOG_FILE`.

El frontend separa el adaptador HTTP y el logger en `src/infrastructure`. Las vistas llaman al cliente de aplicación y no gestionan tokens ni respuestas HTTP directamente. En desarrollo, los eventos de red, autenticación y errores aparecen en la consola del navegador.

El primer usuario es administrador automáticamente; los registros posteriores reciben rol empleado. Los permisos administrativos deben asignarse por el responsable del sistema.

## Roles

- `ADMINISTRADOR`: configura la sala y el catálogo. Puede crear, editar, eliminar y unir mesas; también puede crear, editar y eliminar productos.
- `EMPLEADO`: opera la sala. Puede abrir mesas, agregar productos a pedidos, cerrar cuentas, consultar inventario y ver reportes.

El personal recibe en **Pedidos** las solicitudes QR y las puede confirmar o rechazar. Los pedidos con cambios por alergias deben verificarse con cocina; la indicación no garantiza ausencia de contaminación cruzada.

En la pantalla **Mesas**, el administrador encuentra el bloque **Configurar sala** para crear o unir mesas. Al seleccionar una mesa puede editarla, eliminarla o separarla. Una mesa ocupada no se puede eliminar ni unir; primero debe cerrarse su pedido.

Al seleccionar una mesa ocupada, el pedido se carga y permite agregar productos, retirar líneas y guardar indicaciones de ingredientes que no deben incluirse. Cada acción se persiste inmediatamente; retirar una línea también repone el inventario. Las indicaciones pertenecen únicamente a esa línea del pedido y no cambian la receta general. En caso de alergias, confirma siempre la solicitud con cocina: la indicación no garantiza ausencia de contaminación cruzada.

Ejemplo de registro:
```json
{"username":"maria.lopez","email":"maria@ejemplo.com","password":"ClaveSegura1"}
```
