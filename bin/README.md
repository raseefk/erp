# Lēvanto Flooring — Management System

A complete, production-ready Spring Boot 3.x application for billing, inventory, CRM, HR and expense management for Lēvanto Flooring.

---

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+

### Run locally
```bash
# 1. Clone / unzip the project
cd levanto-flooring

# 2. Run
mvn spring-boot:run

# 3. Open browser
http://localhost:8080          # Public website
http://localhost:8080/login    # Admin login
```

**Default credentials:**
| Username | Password | Role |
|---|---|---|
| `admin` | `Admin@1234` | ROLE_ADMIN |

> ⚠️ Change the password immediately after first login (via application.properties or H2 console).

---

## 🐳 Docker

```bash
# Build and start
docker-compose up --build -d

# View logs
docker-compose logs -f

# Stop
docker-compose down
```

The H2 database file is persisted in a Docker volume (`levanto_data`), so data survives container restarts.

---

## 🏗️ Architecture

```
com.levanto.flooring/
├── config/
│   ├── CompanyProperties.java      # app.company.* from application.properties
│   ├── DataInitializer.java        # Seeds admin + sample data on first run
│   └── SecurityConfig.java         # RBAC Spring Security
├── controller/                     # Web + REST controllers
├── dto/                            # Request/Response DTOs
├── entity/                         # JPA Entities
├── enums/                          # Role, TransactionStatus, ItemType, etc.
├── repository/                     # Spring Data JPA repositories
├── security/
│   └── CustomUserDetailsService.java
├── service/
│   ├── BillingService.java         # ⭐ Smart GST Engine
│   ├── PdfService.java             # iText PDF generation
│   ├── EmployeeService.java        # Pay Salary → auto Expense
│   └── ...
└── util/
    └── NumberGenerator.java        # QUO-2412-0001 / INV-2412-0001
```

---

## 💡 Key Features

### Smart GST Engine (BillingService)
```
Toggle A — gstEnabled:
  OFF → 0% GST on ALL items regardless of type

Toggle B — taxAllItems (only relevant when A is ON):
  OFF → GST applied only to SERVICE items
  ON  → GST applied to PRODUCT + SERVICE items
```

### Quotation → Final Bill Flow
1. Create a **Quotation** (no stock deducted)
2. Click **"Convert to Final Bill"**
3. System deducts stock for PRODUCT line items
4. Generates unique Invoice Number (`INV-YYMM-XXXX`)

### Square Feet Billing
- Each line item has a **Square Feet** field
- `Total = Square Feet × Rate per SqFt`
- Supports mixed items: SQF tiles + RFT skirting + NOS items

### Pay Salary
- Go to **Employees → Pay** button
- Enter amount and date
- System automatically creates an **Expense** record (category: SALARY)

### PDF Generation
- Download branded PDFs for Quotations and Final Bills
- Includes: Company header, Customer info, Line items table, CGST/SGST split, Totals, Terms
- Quotations have a "QUOTATION" watermark

---

## 🔐 Role-Based Access

| Module | ADMIN | EMPLOYEE |
|---|---|---|
| Dashboard | ✅ | ✅ |
| Enquiries | ✅ | ✅ |
| Billing & Quotations | ✅ | ✅ |
| Customers | ✅ | ✅ |
| Inventory | ✅ | ✅ |
| Vendors | ✅ | ❌ |
| Employees | ✅ | ❌ |
| Expenses | ✅ | ❌ |
| H2 Console | ✅ | ❌ |

---

## ⚙️ Configuration

Edit `src/main/resources/application.properties`:

```properties
# Company branding (used in PDFs and website)
app.company.name=Lēvanto Flooring
app.company.address=Your Shop Address, City, State — PIN
app.company.phone=+91 XXXXX XXXXX
app.company.email=info@levantoflooring.com
app.company.gstNumber=YOUR_GSTIN_HERE
app.company.cgstRate=9
app.company.sgstRate=9

# Default admin (change after first run)
app.admin.username=admin
app.admin.password=Admin@1234
```

---

## 🗄️ Database

- **Engine:** H2 (file-persistent)
- **File location:** `./data/levanto_db.mv.db`
- **Console:** http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:file:./data/levanto_db`
  - Username: `sa`
  - Password: `Levanto@2024!`

---

## 📄 API Endpoints (REST)

### Public
| Method | URL | Description |
|---|---|---|
| POST | `/api/enquiries/submit` | Submit enquiry from website |

### Billing (requires auth)
| Method | URL | Description |
|---|---|---|
| POST | `/admin/billing/quotation` | Create quotation (JSON) |
| POST | `/admin/billing/finalbill` | Create final bill (JSON) |
| POST | `/admin/billing/{id}/convert` | Convert quotation → bill |
| POST | `/admin/billing/{id}/payment` | Update payment status |
| GET  | `/admin/billing/{id}/pdf` | Download PDF |
| DELETE | `/admin/billing/{id}` | Delete quotation |

### Inventory
| Method | URL | Description |
|---|---|---|
| POST | `/admin/inventory/{id}/stock?qty=N` | Add stock |
| POST | `/admin/inventory/{id}/price?price=X` | Update price |
| POST | `/admin/inventory/{id}/toggle` | Activate/deactivate |

### Employee
| Method | URL | Description |
|---|---|---|
| POST | `/admin/employees/{id}/pay?amount=X&payDate=Y` | Pay salary |

---

## 🏗️ Building for Production

```bash
# Build fat JAR
mvn clean package -DskipTests

# Run JAR
java -jar target/flooring-1.0.0.jar \
  --app.admin.password=YOUR_SECURE_PASSWORD \
  --app.company.gstNumber=YOUR_GSTIN
```

---

## 📦 Tech Stack

| Component | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.2 |
| Security | Spring Security (Session, BCrypt) |
| Database | H2 (file-persistent) |
| ORM | Spring Data JPA / Hibernate |
| UI | Thymeleaf + Bootstrap 5 |
| PDF | iText 5.5 |
| Build | Maven |
| Container | Docker + docker-compose |

---

*Generated for Lēvanto Flooring. Update `application.properties` with real company details before going live.*
