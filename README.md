# KalingaLumière-Backend

Production-grade ERP for Agarbatti Manufacturing Company — **KalingaLumière**

---

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Security | Spring Security + JWT (access 15min / refresh 7days) |
| ORM | Spring Data JPA + Hibernate |
| Database | MySQL 8.x |
| Migrations | Flyway (23 migrations) |
| Utilities | Lombok, MapStruct |
| Documentation | OpenAPI / Swagger UI |
| Build | Maven |

---

## Architecture

- **Modular Monolith** with Domain-Driven package structure
- **Service + ServiceImpl** pattern on every module
- **Interface-based injection** throughout
- **@Slf4j** logging on every class with pattern: `ClassName:methodName :: message`
- **Layered Architecture**: Controller → Service → Repository → Entity

---

## Package Structure

```
com.business.erp
├── common/         audit, config, exception, response, sequence
├── auth/           JWT, refresh tokens, user management
├── settings/       Configurable business rules (DB-stored)
├── employee/       Employee lifecycle + salary history
├── attendance/     Daily check-in/out + scheduled checkout detection
├── leave/          Paid leave allocation + approval workflow
├── holiday/        Factory & national holidays
├── overtime/       OT request approval + leave conversion
├── payroll/        Full salary engine with loan deductions
├── loan/           Simple interest loans + ledger
├── expense/        Expense approval pipeline
├── supplier/       Supplier ledger + outstanding payables
├── customer/       Customer ledger + credit terms
├── inventory/      Material master + never-negative stock
├── purchase/       Purchase orders + supplier payment
├── production/     Batch system + input/output tracking
├── sales/          Wholesale invoicing + customer payment
├── cashbook/       Cash/bank accounts + auto-entries
├── monthclosing/   Period locking with pre-close validation
├── notification/   In-app notifications (7 types)
├── report/         Attendance, Payroll, P&L reports
├── dashboard/      Admin real-time dashboard
└── audit/          Immutable audit trail
```

---

## Quick Start

### 1. Database Setup

```sql
CREATE DATABASE kalinga_lumiere_erp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'erp_user'@'localhost' IDENTIFIED BY 'StrongPass@2024';
GRANT ALL PRIVILEGES ON kalinga_lumiere_erp.* TO 'erp_user'@'localhost';
FLUSH PRIVILEGES;
```

### 2. Environment Variables

```bash
export DB_URL=jdbc:mysql://localhost:3306/kalinga_lumiere_erp?useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true
export DB_USERNAME=erp_user
export DB_PASSWORD=StrongPass@2024
export JWT_SECRET=S2FsaW5nYUx1bWllcmVTdXBlclNlY3VyZUpXVFNlY3JldEtleTIwMjQ=
export SERVER_PORT=8080
```

### 3. Build & Run

```bash
mvn clean install -DskipTests
mvn spring-boot:run
```

### Environment Profiles

The backend provides two explicit Spring profiles:

| Profile | Configuration | Purpose |
|---------|---------------|---------|
| `prod` | `application-prod.yml` | Production database, secrets, mail, CORS, durable storage, restricted API documentation |
| `test` | `application-test.yml` | Isolated MySQL test database, local test storage, local mail catcher, Swagger enabled |

Use `.env.prod.example` and `.env.test.example` as variable templates. These
files are not loaded automatically by Spring Boot; load their values through
your shell, IDE, container platform, or secret manager.

Run the production profile only after supplying every required `PROD_*` secret:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

Run the isolated test environment with a dedicated MySQL database:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

Never point `TEST_DB_URL` at the production database. Flyway schema validation
and migrations remain enabled in both profiles; Flyway clean is disabled.

### 4. Default Admin Login

```
POST /api/v1/auth/login
{
  "username": "admin",
  "password": "Admin@123"
}
```

### 5. Swagger UI

```
http://localhost:8080/swagger-ui/index.html
```

---

## Roles & Permissions

| Role | Key Permissions |
|------|----------------|
| `ROLE_ADMIN` | Full access — all approvals, payroll, settings, month closing |
| `ROLE_MANAGER` | Production entry, sales entry, inventory view, reports |
| `ROLE_SUPERVISOR` | Attendance entry, production view |
| `ROLE_EMPLOYEE` | View own payslip, attendance, leave balance, apply leave |

---

## API Modules

| Module | Base Path |
|--------|-----------|
| Authentication | `/api/v1/auth` |
| User Management | `/api/v1/users` |
| Settings | `/api/v1/settings` |
| Employees | `/api/v1/employees` |
| Attendance | `/api/v1/attendance` |
| Leave | `/api/v1/leaves` |
| Holidays | `/api/v1/holidays` |
| Overtime | `/api/v1/overtime` |
| Payroll | `/api/v1/payroll` |
| Loans | `/api/v1/loans` |
| Expenses | `/api/v1/expenses` |
| Suppliers | `/api/v1/suppliers` |
| Customers | `/api/v1/customers` |
| Materials | `/api/v1/materials` |
| Inventory | `/api/v1/inventory` |
| Purchases | `/api/v1/purchases` |
| Production | `/api/v1/production/batches` |
| Sales | `/api/v1/sales` |
| Cashbook | `/api/v1/cashbook` |
| Month Closing | `/api/v1/month-closing` |
| Dashboard | `/api/v1/dashboard/admin` |
| Reports | `/api/v1/reports` |
| Notifications | `/api/v1/notifications` |
| Audit Logs | `/api/v1/audit` |

---

## Scheduled Jobs

| Scheduler | Cron | Description |
|-----------|------|-------------|
| `AttendanceScheduler` | `0 55 23 * * *` | Mark forgotten checkouts as PENDING_CHECKOUT |
| `LeaveScheduler` | `0 5 0 1 * *` | Allocate monthly paid leaves on 1st of month |
| `LeaveScheduler` | `0 0 9 25 * *` | Send payroll pending reminder on 25th |

---

## Logging Pattern

Every log entry follows:
```
ClassName:methodName :: message [key=value ...]
```

Examples:
```
EmployeeServiceImpl:create :: Creating employee name=Ravi by=admin
EmployeeServiceImpl:create :: SUCCESS code=EMP-00001 id=1
AttendanceServiceImpl:checkOut :: Overtime detected empId=3 extra=30min
PayrollServiceImpl:generate :: SUCCESS year=2024 month=6 employees=12
```

---

## Key Business Rules Enforced

| Rule | Enforcement |
|------|------------|
| Salary never negative | Capped at ₹0, `salary_capped=true` flag raised |
| Stock never negative | Operation blocked with `InsufficientStockException` |
| One active loan per employee | Service-layer validation before creation |
| Simple interest only | Compound interest prohibited by design |
| Attendance editable within 7 days | Service-layer date check |
| HALF_DAY status prohibited | Enum excludes HALF_DAY |
| Unused leaves expire monthly | No carry-forward by design |
| Settings effective next payroll cycle | `effectiveFromDate` enforced |
| Month closing pre-checks | Blocks if pending checkouts/OT or no payroll |
| All reference numbers concurrency-safe | DB sequence tables with `FOR UPDATE` |
