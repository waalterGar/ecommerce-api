# Ecommerce API 🛒

> **Novedades de `test/jpa-with-testcontainers`**: tests de persistencia con **MySQL Testcontainers**; ITs del repositorio de Orders (distinct roots, orphanRemoval, cascade persist); builders de test ajustados (listas mutables + back-refs vía `Order.addItem/removeItem`); smoke `@SpringBootTest` configurado para usar el contenedor.  
> **Novedades de `feat/validation`**: validación mínima en DTOs de Orders (`@Valid`, `@NotBlank`, `@NotEmpty`, `@Positive`, `@NotNull`) y test **POST inválido → 400**; además, `UnexpectedTypeException` mapeada a **400** como guardarraíl.  
> **Novedades de `feat/orders-transactional-stock`**: creación de pedidos **transaccional** con **decremento de stock**; reglas reforzadas (pricing/moneda autoritativos desde `Product`, totales en servidor, estado inicial `CREATED`). Concurrencia con `@Version` en `Product` (optimistic locking) → **409 Conflict**; **422** para stock insuficiente.  
> **Novedades de `feat/product-update`**: actualización de productos vía **PUT `/products/{sku}`** (normaliza `price` a escala 2, `name` con `trim`, `sku/currency` inmutables); activación/desactivación con **PATCH `/products/{sku}/activation`**; los pedidos devuelven **422** si algún `productSku` está **inactivo** (`urn:problem:inactive-product`).
> **Novedades de `feat/order-lifecycle-v1`**: ciclo de vida de pedido (**Pay + Cancel**). `POST /orders/{id}/pay` marca **PAID** (idempotente por `transactionReference`, verifica `amount/currency` si se envían); `POST /orders/{id}/cancel` marca **CANCELED** y **restituye stock** de forma transaccional. Tests de servicio, WebMvc y un IT de cancelación con Testcontainers.
---

## 📋 Descripción

API REST de un **ecommerce ficticio**, desarrollada con **Spring Boot**, conectada a **MySQL** mediante **Spring Data JPA**.  
Este proyecto sirve como base para demostrar mis habilidades en backend, aplicando buenas prácticas y ampliándolo progresivamente con seguridad, testing e integración en la nube.

---

## 🚀 Stack técnico

- **Java 21**
- **Spring Boot 3**
- **Spring Data JPA**
- **MySQL 8** (contenedor Docker)
- **Docker Compose**
- **Maven**
- **Testcontainers** (tests de persistencia con MySQL real en contenedor)
---

## ⚙️ Configuración y ejecución

> `server.servlet.context-path=/api` → todos los endpoints bajo `/api`.

### 1. Variables de entorno
Crea un archivo `.env` en la raíz del proyecto con las siguientes variables (⚠️ el archivo está en `.gitignore` y no se versiona):

```dotenv
MYSQL_DATABASE=ecommerce
MYSQL_USER=ecommerce_user
MYSQL_PASSWORD=ecommerce_pass
MYSQL_ROOT_PASSWORD=rootpass
```

### 2. Levantar la base de datos para ejecutar la **aplicación**
En la raíz del proyecto:
```bash
docker-compose up -d
```
Esto levantará:
- MySQL en el puerto 3306
- (Opcional) phpMyAdmin en http://localhost:8081

### 3. Ejecutar la aplicación
```bash
./mvnw spring-boot:run
```

_Tras iniciar la app, puedes navegar a **Swagger UI** en `http://localhost:8080/api/swagger-ui/index.html`._

> 🔎 **Tests**: no necesitas `docker-compose` para ejecutar los tests de persistencia; **Testcontainers** arranca un MySQL efímero automáticamente.

---

## 📖 API Docs (OpenAPI / Swagger)

La API expone documentación OpenAPI y una UI interactiva:

- **Swagger UI**: `http://localhost:8080/api/swagger-ui/index.html`
- **Esquema OpenAPI (JSON)**: `http://localhost:8080/api/v3/api-docs`

Notas:
- La documentación se genera automáticamente a partir de los **controladores** y **DTOs**.
- Los **errores** siguen **RFC 7807** (`application/problem+json`) mediante el **handler global**.

> Próxima fase: centralizar documentación de respuestas (201/400/409/422) sin añadir ruido a los controladores.

---
## 📚 Endpoints actuales
> **Prefijo de API**: todos los endpoints están bajo el prefijo **`/api`**.

### 🛍️ Products

- **POST** `/api/products` → Crear un producto 


  Body ejemplo:
  ```json
  {
  "sku": "TSHIRT-BASIC-001",
  "name": "Camiseta Básica Blanca",
  "description": "Camiseta unisex de algodón 100% en color blanco.",
  "price": 19.99,
  "currency": "EUR",
  "stockQuantity": 150
  }
  ```

- **GET** `/api/products` → Listar todos los productos

- **GET** `/api/products/{sku}` → Obtener un producto por SKU

- **PUT** `/api/products/{sku}` → Actualizar un producto (campos mutables)
  - Campos permitidos: `name`, `description`, `price`, `stockQuantity`, `isActive`
  - **Inmutables:** `sku`, `currency`
  - **Códigos:** `200` OK · `400` Validación · `404` No existe · `409` Conflicto (optimistic lock)
    
  
Body ejemplo:
  ```json
    {
    "name": "Camiseta Básica Blanca (v2)",
    "description": "Camiseta unisex 100% algodón. Nueva descripción.",
    "price": 21.49,
    "stockQuantity": 180,
    "isActive": true
    }
  ```

- **PATCH** `/api/products/{sku}/activation` → Activar/Desactivar un producto
  - Body: `{ "isActive": true | false }`
  - **Códigos:** `200` OK · `400` Validación · `404` No existe · `409` Conflicto
  

  Body ejemplo:
  ```json
    { "isActive": false }
  ```

> Detalles técnicos: `price` se normaliza a 2 decimales (HALF_UP). Concurrencia protegida por `@Version` en `Product`.

---

### 🧑‍🤝‍🧑 Customers
- **POST** `/api/customers` → Crear un customer  
  **Body ejemplo:**
  ```json
  {
    "externalId": "aaaaaaaaaaaaaaaaaaaa",
    "firstName": "Walter",
    "lastName": "Garcia",
    "email": "walter@example.com",
    "phoneNumber": "+34 600123456",
    "address": "Calle Falsa 123",
    "city": "Madrid",
    "state": "Madrid",
    "zipCode": "28001",
    "countryCode": "ES",
    "isActive": true
  }
  ```
- **GET** `/api/customers` → Listar todos los customers

- **GET** `/api/customers/{externalId}` → Obtener un customer por su `externalId`
---

### 🧾 Orders
> Requisitos: el `customerExternalId` y los `productSku` deben existir previamente.

- **POST** `/api/orders` → Crear un pedido

  **Comportamiento (v2 - transaccional)**
  - Valida el payload y aplica reglas de negocio.
  - **Puerta de stock**: si `quantity` > `stockQuantity` → **422** (ProblemDetail).
  - **Decrementa stock** y **persiste el pedido** en la **misma transacción**.
  - Cualquier error → **rollback** (ni pedido ni decremento).
  - **Precios/moneda autoritativos**: `unitPrice`/`currency` del item vienen de `Product` (se ignora el precio del cliente).
  - **Moneda del pedido**: la del **primer producto**; mezclas de moneda → **400**.
  - **Producto inactivo**: si algún item tiene `isActive = false` → **422** `urn:problem:inactive-product`.

  **Body ejemplo:**
  ```json
  {
    "customerExternalId": "a1f4e12c-8d5c-4c1b-b3e1-7e2c1d123456",
    "currency": "EUR",
    "items": [
      { "productSku": "TSHIRT-BASIC-002", "quantity": 2 }
    ]
  }
  ```

  Respuesta (201 Created)
  ```json
     { "externalId": "ord-xyz" } 
  ```

- **POST** `/api/orders/{externalId}/pay` → **Marcar como pagado**
  - Estado permitido: `CREATED` → `PAID`.
  - **Idempotencia** (v1): si se envía `transactionReference`, la operación es **idempotente por pedido** (única para `(orderId, transactionReference)`).
  - Validaciones opcionales (si se incluyen en el body):
    - `amount` (escala 2) debe igualar `totalAmount` del pedido.
    - `currency` debe igualar la moneda del pedido.
  - **Códigos:**` 200 OK` · `400` (mismatch/estado inválido) · `404` (no existe) · `409` (conflicto optimista).

**Body ejemplo** (**opcional**):
  ```json
  {
  "amount": 39.98,
  "currency": "EUR",
  "provider": "stripe",
  "transactionReference": "tx-1A2B3C"
  }
  ```

- **POST** `/api/orders/{externalId}/cancel` → Cancelar pedido (con restitución de stock)
  - Estado permitido: `CREATED` → `CANCELED`.
  - Efectos: repone el `stockQuantity` de cada `Product` por la cantidad del item, de forma transaccional.
  - Idempotencia: si ya está `CANCELED`, la operación es no-op y devuelve 200.
  - Códigos: `200` OK · `400` (estado inválido) · `404` (no existe) · `409` (conflicto optimista).


- **GET** `/api/orders/{externalId}` → Obtener un pedido por su `externalId`  
  Nota: si no existe, lanza `NoSuchElementException("Order not found")`, que se mapea a **HTTP 404** mediante el handler global.


- **GET** `/api/orders` → Listar todos los pedidos (con items y customer precargados)

Validación de entrada (createOrder)
- `customerExternalId`: **@NotBlank**
- `items`: **@NotEmpty**
  - cada item:
    - `productSku`: **@NotBlank**
    - `quantity`: **@Positive** (mayor que 0)
- `currency`: **@NotNull** (enum soportado: `EUR`)

Si la validación falla, se devuelve **400** con `application/problem+json` y un mapa `errors` por campo.

Códigos de respuesta (RFC 7807 para errores)
- `201` — Creado.
- `400` — Petición inválida (validación, mezcla de monedas, transición no permitida).
- `404` — Recurso no encontrado (customer/product/order).
- `409` — **Optimistic lock conflict** (`@Version`).
- `422` — **Insufficient stock** / **Producto inactivo** (violación de regla de negocio).

---

## ❗ Manejo global de errores (ProblemDetail)

La API estandariza los errores usando **RFC 7807 – `application/problem+json`**.  
Todas las respuestas de error incluyen: `type`, `title`, `status`, `detail`, `path`, `timestamp`.

- **400** `urn:problem:malformed-json` — cuerpo JSON mal formado.
- **404** `urn:problem:no-resource` — ruta no mapeada/recurso no encontrado.
- **404** `urn:problem:not-found` — recurso inexistente (p. ej. `NoSuchElementException`).
- **400** `urn:problem:invalid-request` — petición inválida (validación semántica, transición de estado no permitida, amount/currency mismatch en pago).
- **400** `urn:problem:validation` — errores de validación en el cuerpo.
- **415** `urn:problem:unsupported-media-type` — `Content-Type` no soportado.
- **406** `urn:problem:not-acceptable` — `Accept` no negociable.
- **409** `urn:problem:conflict` — conflicto de actualización concurrente (optimistic locking).
- **422** `urn:problem:insufficient-stock` — cantidad solicitada excede el stock disponible.
- **422** `urn:problem:inactive-product` — el pedido incluye un `productSku` con `isActive = false`.


> **Nota**: si se configura mal una constraint (p. ej., `@NotBlank` en un enum), el sistema devuelve **400** con `type: urn:problem:validation` gracias al handler de `UnexpectedTypeException`.

---
## 🧪 Tests

El proyecto incluye tests unitarios (JUnit 5 + Mockito), slice web y **tests de integración JPA con Testcontainers**.

- **ProductServiceImpl**
  - createProduct: guarda y mapea correctamente.
  - getProductBySku: recupera por SKU (existe / no existe).
  - getAllProducts: lista vacía si no hay productos.
  - updateProduct: recorta `name`, normaliza `price` (escala 2), conserva `sku/currency`.
  - setProductActive (DTO): happy / notFound / blankSku / nullBody.

- **ProductController** (`@WebMvcTest` + handler global)
  - PUT `/api/products/{sku}` → 200/400/404/409/malformed/415.
  - PATCH `/api/products/{sku}/activation` → 200/400/404/409.

- **OrderServiceImpl** (Mockito)
  - getOrderByExternalId: encontrado / no encontrado.
  - getAllOrders: vacío / con datos.
  - createOrder: happy → totales; customer/product missing → no guarda.
  - createOrder: stock insuficiente → **422**; mezclas de moneda → **400**.
  - createOrder: producto inactivo → **422** (no guarda).
  - `pay`: `CREATED → PAID` (happy), idempotencia por `(order, transactionReference)`, `amount/currency` mismatch → 400, `not found` → 404, `invalid state` → 400.
  - `cancel`: `CREATED → CANCELED` con **restitución de stock**, idempotente si ya está cancelado, `not found`/`invalid state`.


- **OrderController** (slice web)
  - GET `/api/orders/{id}` → 200 / 404 / 400.
  - POST `/api/orders` → 201 / 400 / 406 / 415 / **422 (inactive/stock)**.
  - `POST /api/orders/{id}/pay` → 200 (happy/idempotente), 400 (mismatch/transición), 404.
  - `POST /api/orders/{id}/cancel` → 200 (happy/idempotente), 400 (transición), 404.


- **OrderRepositoryIT** (JPA + **MySQL Testcontainers**)
  - Distinct roots, orphanRemoval, cascade persist.

- **OrderServiceIT** (SpringBoot + **MySQL Testcontainers**)
  - Decremento de stock (éxito), rollback (fallo), caso límite (= stock).
  - `cancel` **restaura stock** correctamente (create → decrementa; cancel → repone).
  - `cancel` **idempotente**: dos cancelaciones no duplican la reposición.

### Ejecutar tests
- Sólo slice web:

  ```bash
  ./mvnw -Dtest=*ControllerTest test
  ```

- Sólo ITs de repositorio:
  ```bash
  ./mvnw -Dtest='*OrderRepositoryIT' test
  ```

- Ejecutar solo ITs de servicio:
  ```bash
  ./mvnw -Dtest="**/service/*ServiceIT.java" test
  ```

- Suite completa:
  ```bash
  ./mvnw test
  ```

> Testcontainers descarga imágenes la primera vez; posteriores ejecuciones son más rápidas.


---
## 📈 Próximos pasos
- Order lifecycle v2: `PAID → SHIPPED → DELIVERED`, reglas de cancelación tardía y (opcional) reembolso.
- Flyway: baseline y migraciones (precio DECIMAL(12,2), `product.version` NOT NULL DEFAULT 0, claves únicas compuestas en `payments`).
- Seguridad con Spring Security + JWT (roles básicos para endpoints de mantenimiento).
- Paginación y filtros en listados (Products/Orders/Customers).
- Observabilidad (logs estructurados, métricas, tracing).
- Validaciones adicionales y pulido de Problem Details.
- Tests end-to-end adicionales.