# Ecommerce API 🛒

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

---

## ⚙️ Configuración y ejecución

> **server.servlet.context-path=/api** → todos los endpoints viven bajo `/api`.

### 1. Variables de entorno
Crea un archivo `.env` en la raíz del proyecto con las siguientes variables (⚠️ el archivo está en `.gitignore` y no se versiona):

```dotenv
MYSQL_DATABASE=ecommerce
MYSQL_USER=ecommerce_user
MYSQL_PASSWORD=ecommerce_pass
MYSQL_ROOT_PASSWORD=rootpass
```

### 2. Levantar la base de datos con Docker
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
  {
    "externalId": "ord-xyz"
  }
```


- **GET** `/api/orders/{externalId}` → Obtener un pedido por su `externalId`  
  **Nota**: actualmente, si no existe lanza `NoSuchElementException("Order not found")`, que se mapea a **HTTP 404** mediante el handler global.

- **GET** `/api/orders` → Listar todos los pedidos (con items y customer precargados)


**Validación de entrada (createOrder)**  
Se aplica validación de Bean Validation en el payload:
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



- **404 Not Found** (recurso no encontrado, p.ej. `NoSuchElementException`)
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

- **400 Bad Request** (entrada inválida, p.ej. `IllegalArgumentException`)
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

- **400 Bad Request** (errores de validación en el cuerpo de la petición)
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

**415 Unsupported Media Type** (cabecera `Content-Type` no soportada)
```json
{
  "type": "urn:problem:unsupported-media-type",
  "title": "Unsupported Media Type",
  "status": 415,
  "detail": "Content type 'text/plain' not supported",
  "path": "/api/orders",
  "timestamp": "2025-10-02T18:00:00Z"
}
```

**406 Not Acceptable** (cabecera `Accept` no negociable)
```json
{
  "type": "urn:problem:not-acceptable",
  "title": "Not Acceptable",
  "status": 406,
  "detail": "Could not find acceptable representation",
  "path": "/api/orders",
  "timestamp": "2025-10-02T18:00:00Z"
}
```

> **Nota**: si se produce una configuración errónea de una constraint (p. ej., usar `@NotBlank` en un enum), el sistema devuelve **400** con `type: urn:problem:validation` gracias al handler para `UnexpectedTypeException`.

---

## 🧪 Tests

El proyecto incluye tests unitarios con **JUnit 5** y **Mockito**.
> Nota: hay un `@SpringBootTest` de arranque de contexto (`EcommerceApiApplicationTests`) que **requiere la BD levantada**.

Cobertura actual:

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
  - `getOrderByExternalId`: no encontrado → lanza `NoSuchElementException("Order not found")`.
  - `getAllOrders`: lista vacía.
  - `getAllOrders`: con datos → valida `externalId` y `totalAmount`.
  - `createOrder`: happy-path → devuelve DTO con items y `totalAmount`.
  - `createOrder`: customer no existe → lanza y **no** guarda.
  - `createOrder`: product no existe → lanza y **no** guarda.


- **`OrderController`** (slice web, `@WebMvcTest` + handler global)
  - `GET /api/orders/{id}` → **200** y JSON de pedido.
  - `GET /api/orders/{id}` (no existe) → **404** `application/problem+json`.
  - `GET /api/orders/{id}` (input inválido) → **400** `application/problem+json`.
  - `POST /api/orders` **válido** → **201** y JSON con `externalId`.
  - `POST /api/orders` **mal JSON** → **400** `application/problem+json`.
  - `POST /api/orders` **Content-Type no soportado** → **415** `application/problem+json`.
  - `POST /api/orders` **Accept no negociable** → **406** `application/problem+json`.
  - Ruta desconocida → **404** `application/problem+json`.


### Ejecutar tests
- **Sólo el slice web (no requiere BD):**
  ```bash
  ./mvnw -Dtest=*ControllerTest test
  ```
- **Suite completa** (requiere MySQL con Docker Compose):
  ```bash
  docker-compose up -d
  ./mvnw test
  ```

---

## 📈 Próximos pasos
- Añadir más endpoints (pedidos, categorías).
- Implementar seguridad con Spring Security y JWT.
- Implementar manejo avanzado de errores y validaciones.
- Ampliar la cobertura de tests.
