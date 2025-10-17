# Ecommerce API 🛒

> **Novedades de `feat/orders-transactional-stock`**: creación de pedidos **transaccional** con **decremento de stock**; reglas reforzadas (pricing/moneda autoritativos desde `Product`, totales en servidor, estado inicial `CREATED`). Concurrencia con `@Version` en `Product` (optimistic locking) → **409 Conflict**; **422** para stock insuficiente.  
> **Novedades de `feat/product-maintenance`**: actualización de productos vía **PUT `/products/{sku}`** (normaliza `price` a escala 2, `name` con `trim`, `sku/currency` inmutables); activación/desactivación con **PATCH `/products/{sku}/activation`**; los pedidos ahora devuelven **422** si algún `productSku` está **inactivo** (`urn:problem:inactive-product`).
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

  Comportamiento (v2 - transaccional)
  - Valida el payload y aplica reglas de negocio.
  - **Puerta de stock**: si `quantity` > `stockQuantity` → **422** (ProblemDetail).
  - **Decrementa stock** y **persiste el pedido** en la **misma transacción**.
  - Cualquier error → **rollback** (ni pedido ni decremento).
  - **Precios/moneda autoritativos**: `unitPrice`/`currency` del item vienen de `Product` (se ignora el precio del cliente).
  - **Moneda del pedido**: la del **primer producto**; mezclas de moneda → **400**.

  Body ejemplo:
 ```json
  {
    "customerExternalId": "a1f4e12c-8d5c-4c1b-b3e1-7e2c1d123456",
    "currency": "EUR",
    "items": 
    [
      { "productSku": "TSHIRT-BASIC-002", "quantity": 2 }
    ]
  }
```

  Respuesta (201 Created)
 ```json
    { "externalId": "ord-xyz" } 
```

  **Reglas de negocio (añadido en esta fase):**
  - Si algún `productSku` del pedido está **inactivo** (`isActive = false`), el pedido se **rechaza** con **422**.

  **Respuesta de error 422 (producto inactivo)**
 ```json
  {
  "type": "urn:problem:inactive-product",
  "title": "Inactive Product",
  "status": 422,
  "detail": "Product is inactive: MUG-LOGO-001",
  "path": "/api/orders",
  "timestamp": "2025-10-17T10:00:00Z"
  }
```

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
- `400` — Petición inválida (validación o **mezcla de monedas**).
- `409` — **Optimistic lock conflict** (concurrencia; `@Version` en `Product`).
- `422` — **Insufficient stock** / **Producto inactivo** (violación de regla de negocio).

---

## ❗ Manejo global de errores (ProblemDetail)

La API estandariza los errores usando **RFC 7807 – `application/problem+json`**.  
Todas las respuestas de error incluyen: `type`, `title`, `status`, `detail`, `path`, `timestamp`.

- **400** `urn:problem:malformed-json` — cuerpo JSON mal formado.
- **404** `urn:problem:no-resource` — ruta no mapeada/recurso no encontrado.
- **404** `urn:problem:not-found` — recurso inexistente (p. ej. `NoSuchElementException`).
- **400** `urn:problem:invalid-request` — petición inválida (p. ej. `IllegalArgumentException`).
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

- **OrderController** (slice web)
  - GET `/api/orders/{id}` → 200 / 404 / 400.
  - POST `/api/orders` → 201 / 400 / 406 / 415 / **422 (inactive/stock)**.

- **OrderRepositoryIT** (JPA + **MySQL Testcontainers**)
  - Distinct roots, orphanRemoval, cascade persist.

- **OrderServiceIT** (SpringBoot + **MySQL Testcontainers**)
  - Decremento de stock (éxito), rollback (fallo), caso límite (= stock).

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

- Ordenes (v1): pagos y cancelación (restock en cancelar).
- Flyway: baseline y migraciones (precio DECIMAL(12,2), `version` NOT NULL DEFAULT 0).
- Seguridad con Spring Security + JWT.
- Paginación y filtros en listados (Products/Orders/Customers).
- Observabilidad (structured logging, métricas, tracing).
- Validaciones adicionales y manejo avanzado de errores.
- Ampliar cobertura de tests end-to-end.