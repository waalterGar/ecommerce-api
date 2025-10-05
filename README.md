# Ecommerce API 🛒

> **Novedades de `test/jpa-with-testcontainers`**: tests de persistencia con **MySQL Testcontainers**; ITs del repositorio de Orders (**distinct roots**, **orphanRemoval**, **cascade persist**); builders de test ajustados (listas **mutables** + back-refs vía `Order.addItem/removeItem`); smoke `@SpringBootTest` configurado para usar el contenedor.
>
> **Novedades de `feat/validation`**: validación mínima en DTOs de Orders (`@Valid`, `@NotBlank`, `@NotEmpty`, `@Positive`, `@NotNull`) y habilitado el test **POST inválido → 400**; además, se mapea `UnexpectedTypeException` a **400** como guardarraíl de validación.

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

> `server.servlet.context-path=/api` → todos los endpoints viven bajo `/api`.

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

> 🔎 **Tests**: no necesitas `docker-compose` para ejecutar los tests de persistencia; **Testcontainers** arranca un MySQL efímero automáticamente.

---

## 📚 Endpoints actuales
> **Prefijo de API**: todos los endpoints están bajo el prefijo **`/api`**.

### 🛍️ Products
- **POST** `/api/products` → Crear un producto  
  **Body ejemplo:**
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
  **Respuesta (201 Created)**
  ```json
  { "externalId": "ord-xyz" }
  ```

- **GET** `/api/orders/{externalId}` → Obtener un pedido por su `externalId`  
  **Nota**: si no existe, lanza `NoSuchElementException("Order not found")`, que se mapea a **HTTP 404** mediante el handler global.

- **GET** `/api/orders` → Listar todos los pedidos (con items y customer precargados)

**Validación de entrada (createOrder)**  
Se aplica Bean Validation en el payload:
- `customerExternalId`: **@NotBlank**
- `items`: **@NotEmpty**
  - cada item:
    - `productSku`: **@NotBlank**
    - `quantity`: **@Positive** (mayor que 0)
- `currency`: **@NotNull** (enum soportado: `EUR`)

Si la validación falla, se devuelve **400** con `application/problem+json` y un mapa `errors` por campo.

---

## ❗ Manejo global de errores (ProblemDetail)

La API estandariza los errores usando **RFC 7807 – `application/problem+json`**.  
Todas las respuestas de error incluyen: `type`, `title`, `status`, `detail`, `path`, `timestamp`.

- **400** `urn:problem:malformed-json` — cuerpo JSON mal formado.
- **404** `urn:problem:no-resource` — ruta no mapeada/recurso no encontrado.
- **404** `urn:problem:not-found` — recurso inexistente (p. ej. `NoSuchElementException`).
  ```json
  {
    "type": "urn:problem:not-found",
    "title": "Resource Not Found",
    "status": 404,
    "detail": "Order not found",
    "path": "/api/orders/ord-does-not-exist",
    "timestamp": "2025-10-01T18:00:00Z"
  }
  ```
- **400** `urn:problem:invalid-request` — petición inválida (p. ej. `IllegalArgumentException`).
  ```json
  {
    "type": "urn:problem:invalid-request",
    "title": "Invalid Request",
    "status": 400,
    "detail": "Invalid externalId",
    "path": "/api/orders/bad",
    "timestamp": "2025-10-01T18:00:00Z"
  }
  ```
- **400** `urn:problem:validation` — errores de validación en el cuerpo.
  ```json
  {
    "type": "urn:problem:validation",
    "title": "Validation Failed",
    "status": 400,
    "detail": "One or more fields are invalid.",
    "path": "/api/orders",
    "timestamp": "2025-10-02T18:00:00Z",
    "errors": {
      "customerExternalId": "customerExternalId is required",
      "items": "items must not be empty"
    }
  }
  ```
- **415** `urn:problem:unsupported-media-type` — `Content-Type` no soportado.
- **406** `urn:problem:not-acceptable` — `Accept` no negociable.

> **Nota**: si se configura mal una constraint (p. ej., `@NotBlank` en un enum), el sistema devuelve **400** con `type: urn:problem:validation` gracias al handler de `UnexpectedTypeException`.

---

## 🧪 Tests

El proyecto incluye tests unitarios (JUnit 5 + Mockito), slice web y **tests de integración JPA con Testcontainers**.

- **`ProductServiceImpl`**
  - `createProduct`: guarda y mapea correctamente.
  - `getProductBySku`: recupera por SKU (existe / no existe).
  - `getAllProducts`: lista vacía si no hay productos.

- **`CustomerServiceImpl`**
  - `createCustomer`: guarda y mapea correctamente.
  - `getCustomerByExternalId`: recupera por `externalId` (existe / no existe).
  - `getAllCustomers`: lista vacía.

- **`OrderServiceImpl`** (Mockito, sin contexto Spring)
  - `getOrderByExternalId`: encontrado → DTO con items y totales.
  - `getOrderByExternalId`: no encontrado → `NoSuchElementException`.
  - `getAllOrders`: lista vacía / con datos (valida `externalId` y `totalAmount`).
  - `createOrder`: happy-path → devuelve DTO con items y `totalAmount`.
  - `createOrder`: customer no existe → lanza y **no** guarda.
  - `createOrder`: product no existe → lanza y **no** guarda.

- **`OrderController`** (slice web, `@WebMvcTest` + handler global)
  - `GET /api/orders/{id}` → **200** y JSON de pedido.
  - `GET /api/orders/{id}` (no existe) → **404** problem.
  - `GET /api/orders/{id}` (input inválido) → **400** problem.
  - `POST /api/orders` **válido** → **201** JSON con `externalId`.
  - `POST /api/orders` **mal JSON** → **400** problem.
  - `POST /api/orders` **Content-Type no soportado** → **415** problem.
  - `POST /api/orders` **Accept no negociable** → **406** problem.
  - Ruta desconocida → **404** problem.

- **`OrderRepositoryIT`** (JPA + **MySQL Testcontainers**)
  - **Distinct roots**: `findAllWithItemsAndCustomer` devuelve órdenes **sin duplicar** y con conteo correcto de items.
  - **Orphan removal**: eliminar un item del agregado lo borra en DB (`orphanRemoval = true`).
  - **Cascade persist**: guardar sólo el `Order` persiste también sus `OrderItem`s; se validan IDs y back-refs.

### Ejecutar tests
- **Sólo el slice web (no requiere BD):**
  ```bash
  ./mvnw -Dtest=*ControllerTest test
  ```
- **Sólo los IT de repositorio (arranca Testcontainers automáticamente):**
  ```bash
  ./mvnw -Dtest='*OrderRepositoryIT' test
  ```
- **Suite completa**:
  ```bash
  ./mvnw test
  ```

> Testcontainers descarga imágenes la primera vez; posteriores ejecuciones son más rápidas.

---

## 📈 Próximos pasos
- Reglas de negocio (v1): stock mínimo, decremento de stock, totales “autoritativos”, estado inicial `NEW`, transaccionalidad, 422 (insufficient stock) con ProblemDetail.
- Añadir más endpoints (categorías, etc.).
- Seguridad con Spring Security + JWT.
- Validaciones adicionales y manejo avanzado de errores.
- Ampliar la cobertura de tests end-to-end.
