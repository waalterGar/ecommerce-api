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
````
docker-compose up -d
````
Esto levantará:

- MySQL en el puerto 3306
- (Opcional) phpMyAdmin en http://localhost:8081

### 3. Ejecutar la aplicación
````
./mvnw spring-boot:run
````
## 📚 Endpoints actuales
Product

- POST `/products` → Crear un producto
Ejemplo de body:
````json
{
  "sku": "TSHIRT-BASIC-001",
  "name": "Camiseta Básica Blanca",
  "description": "Camiseta unisex de algodón 100% en color blanco.",
  "price": 19.99,
  "currency": "EUR",
  "stockQuantity": 150
}
````
- **GET** `/products` → Listar todos los productos
- **GET** `/products/sku` → Obtener un producto por ID
