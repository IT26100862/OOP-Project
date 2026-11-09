# EstateX – Real Estate Property Listing & Management System

EstateX is a full-stack real estate platform connecting property buyers, tenants, certified realtors, and platform administrators. Built with a **Spring Boot 3 (Java 21)** REST backend and a **Vanilla HTML5 / CSS3 / JavaScript** frontend.

---

## 📋 System Prerequisites

To run this project on a Windows PC using IntelliJ IDEA, ensure the following are installed:

1. **Java Development Kit (JDK 21 or JDK 17)**
   - Download JDK 21: [Eclipse Adoptium Temurin 21](https://adoptium.net/temurin/releases/?version=21)
   - Ensure the `JAVA_HOME` environment variable is configured and `java -version` works in Command Prompt.
2. **IntelliJ IDEA**
   - Community Edition or Ultimate Edition: [Download IntelliJ IDEA](https://www.jetbrains.com/idea/download/)
3. **Web Server for Frontend (Any ONE of the following)**
   - **Python 3** (Recommended): [python.org](https://www.python.org/downloads/) (Check *"Add Python to PATH"* during install)
   - **OR Node.js**: [nodejs.org](https://nodejs.org/) (for `npx serve`)
   - **OR IntelliJ's Built-in Browser Preview**
4. **Database (Zero Setup Required!)**
   - The application is **already connected to a live Cloud MySQL database (TiDB Serverless)** in `backend/src/main/resources/application.properties`.
   - **No local MySQL or XAMPP installation is required** to run out-of-the-box.
   - *(Optional)* If you wish to use a local MySQL server, an `init_tidb.sql` / `estatex_db.sql` schema script is included.

---

## 🚀 How to Run the Project

### 1. Running the Backend (Port 8081)

#### Method A: Using IntelliJ IDEA (Recommended)
1. Launch **IntelliJ IDEA**.
2. Select **File > Open...** and choose the `real-estate-property-management-system` folder.
3. If prompted with *"Trust Project"*, click **Trust Project**.
4. In the Project tool window (left panel), expand:
   `backend` > `src` > `main` > `java` > `com` > `estatex` > `EstateXApplication.java`
5. Right-click on `EstateXApplication.java` and click **Run 'EstateXApplication'** (or click the green **▶** play button in the editor gutter).
6. Verify the console displays:
   ```text
   Tomcat started on port 8081 (http) with context path ''
   Started EstateXApplication in ... seconds
   ```

> **Tip:** If IntelliJ doesn't automatically detect the Maven project, right-click `backend/pom.xml` > **Maven** > **Reload Project**.

#### Method B: 1-Click Windows Batch Script
- Simply double-click **`run-backend.bat`** in the project root folder.

---

### 2. Running the Frontend (Port 8080)

#### Method A: 1-Click Windows Batch Script (Easiest)
- Double-click **`run-frontend.bat`** in the project root folder.
- It will automatically launch the web server and open your default browser to `http://localhost:8080`.

#### Method B: Command Prompt / PowerShell
1. Open Command Prompt or PowerShell in the project root directory.
2. Run:
   ```cmd
   python -m http.server 8080
   ```
   *(Or if using Node.js: `npx serve -l 8080 .`)*
3. Open `http://localhost:8080` in your web browser.

#### Method C: IntelliJ IDEA Built-in Preview
1. In IntelliJ IDEA, navigate to `index.html` in the project root.
2. Right-click `index.html` > **Open In** > **Browser** > **Chrome** (or your preferred browser).

---

## 🔑 Pre-Configured Demo Accounts

You can test all 3 role-based portals using these seeded accounts:

| Portal | Role | Username | Password | Direct Dashboard Link |
| :--- | :--- | :--- | :--- | :--- |
| **Customer** | Registered Buyer / Tenant | `chethana@user` | `chethana123` | [`user/dashboard.html`](http://localhost:8080/user/dashboard.html) |
| **Customer** | Alternate Buyer | `dilshan@user` | `userPass_dilshan123` | [`user/dashboard.html`](http://localhost:8080/user/dashboard.html) |
| **Agent** | Certified Realtor | `chaminda@agent` | `agentPass_chaminda123` | [`agent/dashboard.html`](http://localhost:8080/agent/dashboard.html) |
| **Admin** | System Administrator | `admin@admin` | `adminPass_master123` | [`admin/dashboard.html`](http://localhost:8080/admin/dashboard.html) |

---

## 🌐 Application Page URLs

- **Home / Landing Page**: `http://localhost:8080/index.html`
- **Browse Properties**: `http://localhost:8080/public/properties.html`
- **Single Property Details**: `http://localhost:8080/public/property-details.html?id=1`
- **Sign In / Registration**: `http://localhost:8080/public/login.html`
- **Customer Profile**: `http://localhost:8080/user/profile.html`
- **Agent Profile**: `http://localhost:8080/agent/profile.html`
- **Admin Profile**: `http://localhost:8080/admin/profile.html`

---

## 🛠️ Troubleshooting for Windows

1. **Port 8081 is already in use:**
   - In Command Prompt, run: `netstat -ano | findstr :8081`
   - Kill the existing process: `taskkill /PID <PID_NUMBER> /F`
2. **Java Version Error (`Unsupported class file major version`):**
   - Ensure IntelliJ Project SDK is set to **JDK 21** (`File > Project Structure > Project > SDK`).
3. **CORS / Backend Not Responding:**
   - Ensure the Spring Boot backend console indicates it is running on port 8081 before submitting forms or searching.
