# EstateX Backend – User Management & Authentication

Spring Boot REST backend with SQL database persistence for the EstateX Real Estate Property Listings Portal.

---

## 🏗️ Architecture & Component Design

The backend follows a clean layered enterprise architecture:

```
backend/
├── pom.xml                               # Maven project configuration (Java 21, Spring Boot 3.2.5)
├── run-backend.bat                       # Single-click launcher script
└── src/main/
    ├── java/com/estatex/
    │   ├── EstateXApplication.java        # Main Spring Boot Entrypoint
    │   ├── model/                        # Domain Models & Entities
    │   │   ├── Role.java                 # Enum: CUSTOMER, AGENT, ADMIN with portal URLs
    │   │   └── User.java                 # JPA Entity (id, username, password, email, fullName, role)
    │   ├── repository/                   # Data Access Layer (Spring Data JPA SQL queries)
    │   │   └── UserRepository.java       # findByUsername, findByEmail, existsByUsername, existsByPassword
    │   ├── service/                      # Business Logic Layer
    │   │   └── UserService.java          # Authenticate, Register, Role Resolution, Password Uniqueness
    │   ├── controller/                   # REST API Controllers (Exposes HTTP endpoints)
    │   │   ├── AuthController.java       # /api/auth/login, /api/auth/register, /api/auth/check-username
    │   │   └── UserController.java       # /api/users (CRUD operations for Admin management)
    │   ├── dto/                          # Data Transfer Objects
    │   │   ├── LoginRequest.java         # Username and Password payload
    │   │   ├── LoginResponse.java        # Authentication status, User profile, Target Redirect URL
    │   │   ├── RegisterRequest.java      # New account details & role selection
    │   │   ├── UserDto.java              # Safe user serialization (excludes password)
    │   │   └── ApiResponse.java          # Standard status response
    │   └── config/
    │       └── WebConfig.java            # CORS configuration for frontend origins
    └── resources/
        ├── application.properties        # Server port (8081) and SQL database settings
        ├── schema.sql                    # SQL DDL script creating `users` table with unique constraints
        └── data.sql                      # SQL DML seed script with pre-configured role users
```

---

## 🏷️ Username Format Convention & Role Identification

Usernames follow designated role formats so that access level and portal routing are clear:

* **Standard User (Customer)**: `name@user` (e.g. `john@user`, `dilshan@user`) &rarr; Navigates to **Customer Portal** (`../user/dashboard.html`)
* **Real Estate Agent**: `name@agent` (e.g. `chaminda@agent`) &rarr; Navigates to **Agent Portal** (`../agent/dashboard.html`)
* **Platform Administrator**: `name@admin` (e.g. `admin@admin`) &rarr; Navigates to **Admin Portal** (`../admin/dashboard.html`)

### 🔒 Password Uniqueness
Passwords are uniquely enforced across all user accounts in both the SQL database schema (`password VARCHAR(255) NOT NULL UNIQUE`) and the `UserService` business layer.

---

## 🗄️ SQL Database Configuration

### 1. Default (Zero-Configuration SQL Mode)
By default, the application runs with an embedded SQL database configured in `application.properties`:
- Automatically runs `schema.sql` and `data.sql` on startup.
- Visual Web SQL Console: **[http://localhost:8081/h2-console](http://localhost:8081/h2-console)**
  - JDBC URL: `jdbc:h2:mem:estatex_db`
  - User: `sa`
  - Password: *(blank)*

### 2. Switching to MySQL Server 8.0
To connect to your installed MySQL Server instance:
1. Open `backend/src/main/resources/application.properties`.
2. Comment out the H2 block.
3. Uncomment the MySQL block:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/estatex_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   spring.datasource.driverClassName=com.mysql.cj.jdbc.Driver
   spring.datasource.username=root
   spring.datasource.password=your_mysql_password
   spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
   ```

---

## 🔑 Pre-Configured Test Accounts

| Role | Username | Password (Unique) | Email | Redirect Destination |
| :--- | :--- | :--- | :--- | :--- |
| **Standard User (Customer)** | `dilshan@user` | `userPass_dilshan123` | `dilshan.perera@gmail.com` | `../user/dashboard.html` |
| **Real Estate Agent** | `chaminda@agent` | `agentPass_chaminda123` | `chaminda.agent@estatex.lk` | `../agent/dashboard.html` |
| **Administrator** | `admin@admin` | `adminPass_master123` | `admin@estatex.lk` | `../admin/dashboard.html` |

---

## 🚀 How to Run the Backend

### Option A: Helper Script (Recommended)
Double-click `run-backend.bat` in the `backend/` folder.

### Option B: Terminal Command
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
& "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2\plugins\maven\lib\maven3\bin\mvn.cmd" spring-boot:run
```

The server starts on port `8081`. The sign-in page (`public/login.html`) automatically communicates with this backend.
