# Ecommerce API 🛒

> **Novedades de `feat/order`**: agregado completo de **Orders** (dominio + API), mapeos DTO, repositorio, servicio, controller y suite de tests de servicio.

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

### 1. Variables de entorno
Crea un archivo `.env` en la raíz del proyecto con las siguientes variables (⚠️ el archivo está en `.gitignore` y no se versiona):

```dotenv
MYSQL_DATABASE=ecommerce
MYSQL_USER=ecommerce_user
MYSQL_PASSWORD=ecommerce_pass
MYSQL_ROOT_PASSWORD=rootpass
````

### 2. Levantar la base de datos con Docker
En la raíz del proyecto:
````bash
docker-compose up -d
````
Esto levantará:

- MySQL en el puerto 3306
- (Opcional) phpMyAdmin en http://localhost:8081

### 3. Ejecutar la aplicación
````bash
./mvnw spring-boot:run
````

---

## 📚 Endpoints actuales

### 🛍️ Products
- **POST** `/products` → Crear un producto  
  Ejemplo de body:
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
- **GET** `/products` → Listar todos los productos
- **GET** `/products/{sku}` → Obtener un producto por SKU

---

### 🧑‍🤝‍🧑 Customers

- **POST** `/customers` → Crear un customer  
  Ejemplo de body:
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

- **GET** `/customers` → Listar todos los customers
- **GET** `/customers/{externalId}` → Obtener un customer por su ExternalId

---

### 🧾 Orders

> Requisitos: el `customerExternalId` y los `productSku` deben existir previamente.

- **POST** `/orders` → Crear un pedido  
  **Body ejemplo:**
  ```json
  {
    "customerExternalId": "a1f4e12c-8d5c-4c1b-b3e1-7e2c1d123456",
    "currency": "EUR",
    "items": [
      {
        "productSku": "TSHIRT-BASIC-002",
        "quantity": 2
      }
    ]
  }
  ```
- **GET** `/orders/{externalId}` → Obtener un pedido por su `externalId`
 **Nota**: actualmente, si no existe lanza `NoSuchElementException("Order not found")`.
 (En una fase posterior se mapeará a HTTP 404 con ProblemDetail).

- **GET** `/orders` → Listar todos los pedidos (con items y customer precargados)

---

## 🧪 Tests

El proyecto incluye tests unitarios con **JUnit 5** y **Mockito**.
> Nota: Hay un `@SpringBootTest` de arranque de contexto (`EcommerceApiApplicationTests`) que **requiere la BD levantada**.

Cobertura actual:

- **`ProductServiceImpl`**
  - `createProduct`: guarda y mapea correctamente.
  - `getProductBySku`: recupera por SKU (existe / no existe).
  - `getAllProducts`: lista vacía si no hay productos.

- **`CustomerServiceImpl`**
  - `createCustomer`: guarda y mapea correctamente.
  - `getCustomerByExternalId`: recupera por externalId (existe / no existe).
  - `getAllCustomers`: lista vacía.

- **`OrderServiceImpl`** (Mockito, sin contexto Spring)
  - `getOrderByExternalId`: encontrado → DTO con items y totales
  - `getOrderByExternalId`: no encontrado → lanza `NoSuchElementException("Order not found")`
  - `getAllOrders`: lista vacía
  - `getAllOrders`: con datos → valida `externalId` y `totalAmount`
  - `createOrder`: happy-path → devuelve DTO con items y `totalAmount`
  - `createOrder`: customer no existe → lanza y **no** guarda
  - `createOrder`: product no existe → lanza y **no** guarda

### Ejecutar tests
Asegúrate de tener la base de datos en marcha:
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

---
