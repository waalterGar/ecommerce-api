# Ecommerce API 🛒

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

## 🧪 Tests

El proyecto incluye tests unitarios escritos con **JUnit 5** y **Mockito**.

Actualmente se han implementado tests mínimos para:
- **`ProductServiceImpl`**:
    - `createProduct`: valida que un producto se guarde y se mapee correctamente.
    - `getProductBySku`: comprueba la recuperación de un producto por SKU (cuando existe y cuando no existe).
    - `getAllProducts`: verifica que se devuelva una lista vacía cuando no hay productos.
- **`CustomerServiceImpl`**:
    - `createCustomer`: valida que un customer se guarde y se mapee correctamente.
    - `getCustomerByExternalId`: comprueba la recuperación de un customer por externalId (cuando existe y cuando no existe).
    - `getAllCustomers`: verifica que se devuelva una lista vacía cuando no hay customers.

Para ejecutar los tests, usa:
```bash
./mvnw test
```

---

## 📈 Próximos pasos
- Añadir más endpoints (pedidos, categorías).
- Implementar seguridad con Spring Security y JWT.
- Implementar manejo avanzado de errores y validaciones.
- Ampliar la cobertura de tests.

---
