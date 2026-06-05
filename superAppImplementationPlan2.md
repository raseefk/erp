# Super ERP — Super App Implementation Plan

> **Stack:** Java 17 · Spring Boot 3.2.5 · PostgreSQL · Liquibase · Thymeleaf · Caffeine Cache · JWT  
> **Architecture:** Multi-tenant (RLS) · RBAC · Modular Monolith (migrate to microservices in Phase 5+)  
> **Target:** Universal Enterprise Platform covering all domains, industries, and sectors

---

## Current Baseline (Already Built)

| Module | Status |
|--------|--------|
| HR — Attendance, Leave, Salary, Employees | ✅ Done |
| Finance — Billing, Payments, Expenses, P&L | ✅ Done |
| Inventory Management | ✅ Done |
| Projects & Construction (BOQ, Job Cards, Daily Logs, Subcontractor Bills) | ✅ Done |
| Asset Management (Maintenance, Depreciation, Breakdowns) | ✅ Done |
| Purchase Orders | ✅ Done |
| CRM — Enquiries, Customers, Vendors | ✅ Done |
| Multi-Tenancy with PostgreSQL RLS | ✅ Done |
| RBAC with Permission Manifests | ✅ Done |
| Audit Logs | ✅ Done |
| JWT Auth + Token Blacklist | ✅ Done |

---

## Performance Optimisations (Pre-requisite — Do Before Any New Feature)

> These must be done first. They affect every request and every new module will inherit the same problems.

| # | Issue | Fix | Effort |
|---|-------|-----|--------|
| P1 | `HibernateTenantFilterAspect` fires 3 SQL queries on every service/repo method | Move `set_config` calls to a `OncePerRequestFilter`; set Hibernate filter via `CurrentTenantIdentifierResolver` | 2 days |
| P2 | `AuditLogRepository` returns unbounded `List<AuditLog>` | Add `Pageable` parameter; add DB index on `(tenant_id, created_at DESC)` | 0.5 days |
| P3 | `HrService.getAttendanceReport()` loads all employees into memory then paginates in Java | Push pagination to DB layer; replace `.stream().filter()` loops with `HashMap` lookups | 1 day |
| P4 | JWT blacklist check hits DB on every request | Add `@Cacheable("tokenBlacklist")` | 0.5 days |
| P5 | `CompanySettingsService.getSettings()` uncached, called on every request | Add `@Cacheable` + `@CacheEvict` on update | 0.5 days |
| P6 | `ProjectAnalyticsService` fires 30 separate queries for 30-day chart | Single grouped aggregate query | 0.5 days |
| P7 | `AssetManagementService` loads full lists just to call `.size()` | Replace with `countByStatus()` | 0.5 days |
| P8 | `DataInitializer` BCrypt re-encodes password on every startup | Only re-encode if hash does not match | 0.5 days |
| P9 | No HikariCP configuration — default pool of 10 connections | Add explicit Hikari config in `application.properties` | 0.5 days |
| P10 | Multiple unbounded `List<>` returns (JobCards, LeaveApplications, AdvancePayments) | Add `Pageable` or projections | 1 day |
| P11 | `spring.thymeleaf.cache=false` in base properties | Set `true` in base; override `false` in dev profile only | 0.5 days |

**Total pre-requisite effort: ~8 days**

---

## Phase 1 — Core ERP Completion (Weeks 1–12)

*Fill the must-have gaps that every enterprise expects.*

---

### 1.1 Full Accounting & General Ledger

**Why first:** Everything financial in the app (billing, expenses, payroll) is currently isolated. A GL unifies them all and is non-negotiable for any enterprise customer.

**Entities to create:**
- `AccountGroup` (Assets, Liabilities, Equity, Income, Expenses)
- `ChartOfAccount` (code, name, type, parent, isSystem)
- `JournalEntry` (date, narration, reference, status: DRAFT/POSTED/REVERSED)
- `JournalEntryLine` (account, debit, credit, costCenter, projectRef)
- `FiscalYear` (startDate, endDate, status: OPEN/CLOSED)
- `CostCenter` (department-level cost allocation)
- `BankAccount` (linked to ChartOfAccount, ifscCode, accountNumber)
- `BankReconciliation` (statement upload, matched/unmatched lines)

**Services:**
- `GeneralLedgerService` — post entries, reverse entries, period close
- `TrialBalanceService` — grouped by account type with opening/closing balances
- `BalanceSheetService` — assets = liabilities + equity assertion
- `CashFlowService` — operating / investing / financing categorisation
- `BankReconciliationService` — auto-match by amount + date ± 3 days

**Auto-posting rules (integrate with existing modules):**
- Invoice raised → Dr Accounts Receivable, Cr Revenue
- Payment received → Dr Bank/Cash, Cr Accounts Receivable
- Expense approved → Dr Expense Account, Cr Accounts Payable
- Salary processed → Dr Salary Expense, Cr Payable / Bank

**India-specific:**
- GST ledger accounts (CGST Payable, SGST Payable, IGST Payable, Input Tax Credit)
- GSTR-1 export (outward supplies)
- GSTR-3B summary generation
- TDS payable ledger with section-wise tracking (194C, 194J, etc.)

**UI pages:**
- Chart of Accounts tree view
- Journal Entry form with multi-line debit/credit validation
- Ledger view per account (paginated, date-filtered)
- Trial Balance report
- Balance Sheet report
- P&L Statement (already partial — wire to GL)
- Cash Flow Statement
- Bank Reconciliation workspace

**Estimated effort:** 6 weeks

---

### 1.2 Complete Sales & CRM Pipeline

**Why:** You have enquiries and customers but no structured sales funnel. Revenue is tracked but not driven.

**Entities to create:**
- `Lead` (source, status: NEW/CONTACTED/QUALIFIED/LOST)
- `Opportunity` (linked to Lead or Customer, estimatedValue, closeDate, stage, probability)
- `Quotation` (linked to Customer/Opportunity, lineItems, taxes, validity, status)
- `QuotationItem` (inventoryItem or custom description, qty, rate, tax)
- `SalesOrder` (from accepted Quotation, deliveryDate, status)
- `SalesTarget` (employee/team, period, targetAmount)
- `SalesActivity` (call, meeting, email log against Lead/Opportunity)

**Services:**
- `LeadService` — lead capture, assignment, scoring
- `OpportunityService` — pipeline management, win/loss tracking
- `QuotationService` — build quote, apply discount rules, generate PDF
- `SalesOrderService` — confirm order, trigger inventory reservation
- `SalesDashboardService` — funnel metrics, conversion rates, rep performance

**UI pages:**
- Lead list with quick-add and bulk-import (CSV)
- Opportunity Kanban board (drag stages)
- Quotation builder with live tax calculation
- Quotation PDF preview and email-send
- Sales Order list → Invoice conversion (one-click)
- Sales dashboard: pipeline value, win rate, top reps, monthly trend

**Estimated effort:** 3 weeks

---

### 1.3 Full Payroll & Statutory Compliance Engine

**Why:** You have `EmployeeSalary` and basic salary records but no CTC structure, no statutory deductions, no payslip generation.

**Entities to create:**
- `SalaryStructure` (template: Basic%, HRA%, DA%, Special Allowance%)
- `PayrollRun` (month, year, status: DRAFT/APPROVED/DISBURSED)
- `PayslipLine` (component name, type: EARNING/DEDUCTION, amount)
- `Payslip` (linked to PayrollRun + Employee, grossPay, netPay, status)
- `PfAccount` (UAN, employerContribution, employeeContribution)
- `EsiRecord` (esicNumber, monthly contribution)
- `TdsSalary` (section 192, monthly projected tax, TDS deducted)

**Services:**
- `PayrollRunService` — compute all payslips in one batch for the month
- `StatutoryService` — PF (12%+12%), ESI (3.25%+0.75%), PT slab-wise by state
- `Form16Service` — generate Form 16 Part A and Part B per employee per FY
- `PayslipPdfService` — extend existing `PdfService`
- `BankDisbursementService` — generate NEFT bulk file (HDFC/SBI format)

**Payroll computation flow:**
1. Lock attendance for the month
2. Apply LOP for unapproved leaves
3. Apply salary structure components
4. Compute PF, ESI, PT, TDS
5. Generate draft payslips
6. Manager approval
7. Generate bank disbursement file
8. Post GL entries automatically

**UI pages:**
- Salary structure builder per employee grade
- Payroll run wizard (month selection → compute → review → approve → disburse)
- Payslip view per employee (printable PDF)
- Statutory reports: PF ECR file, ESI return, PT challan
- Form 16 bulk generation and download

**Estimated effort:** 4 weeks

---

### 1.4 Procurement — 3-Way Matching & Complete SCM

**Why:** You have Purchase Orders but no Goods Receipt, no vendor invoice matching, no RFQ.

**Entities to create:**
- `RFQ` (Request for Quotation — linked to multiple vendors)
- `RFQItem` (description, qty, unit)
- `VendorQuote` (RFQ response per vendor, lineItems with rates)
- `GoodsReceiptNote` (GRN — linked to PO, receivedDate, warehouseLocation)
- `GrnItem` (poItem reference, orderedQty, receivedQty, rejectedQty, reason)
- `VendorInvoice` (linked to GRN + PO — 3-way match)
- `VendorRating` (quality, delivery, price score per PO)

**Services:**
- `RfqService` — create RFQ, send to vendors, compare quotes, convert to PO
- `GrnService` — receive goods, update inventory stock on GRN approval
- `ThreeWayMatchService` — validate PO qty/rate = GRN qty = Vendor Invoice amount
- `VendorScorecardService` — compute rolling rating per vendor

**UI pages:**
- RFQ creation and multi-vendor comparison matrix
- GRN form (scan PO, enter received quantities)
- 3-way match workspace (side-by-side PO / GRN / Invoice)
- Vendor scorecard dashboard

**Estimated effort:** 3 weeks

---

## Phase 2 — Operations & Warehouse (Weeks 13–22)

---

### 2.1 Warehouse Management System (WMS)

**Entities to create:**
- `Warehouse` (name, address, type: MAIN/TRANSIT/STORE)
- `WarehouseZone` (zone within warehouse)
- `StorageBin` (bin code, zone, capacity)
- `StockLedger` (item, warehouse, bin, batchNo, serialNo, qty, valuationMethod)
- `StockTransferOrder` (from warehouse → to warehouse, status)
- `StockTransferItem` (item, qty, sourceBin, destinationBin)
- `PhysicalStockCount` (date, warehouse, status: OPEN/IN-PROGRESS/COMPLETED)
- `StockCountLine` (item, systemQty, countedQty, variance)

**Services:**
- `WarehouseService` — manage locations, zones, bins
- `StockLedgerService` — FIFO/LIFO/weighted-average costing engine
- `StockTransferService` — inter-warehouse movements with GL impact
- `ReorderService` — check reorder points, auto-generate draft POs
- `PhysicalCountService` — cycle counting, variance analysis

**Estimated effort:** 3 weeks

---

### 2.2 Document Management System (DMS)

**Entities to create:**
- `DocumentFolder` (hierarchical, tenant-scoped)
- `Document` (name, folder, tags, mimeType, storagePath, version, expiryDate)
- `DocumentVersion` (versionNumber, uploadedBy, uploadedAt, changeNote)
- `DocumentAccess` (documentId, roleId, permission: VIEW/EDIT/DELETE)
- `DocumentSignature` (signatoryUserId, signedAt, ipAddress)

**Services:**
- `DocumentService` — upload, version, tag, search, share
- `DocumentExpiryAlertService` — scheduled job to notify 30/15/7 days before expiry
- `OcrService` — extract text from scanned PDFs (Apache PDFBox + Tesseract integration)

**Estimated effort:** 2 weeks

---

### 2.3 Workflow & Business Process Automation Engine

**Entities to create:**
- `WorkflowDefinition` (name, triggerEntity, triggerEvent, steps JSON)
- `WorkflowStep` (stepNumber, approverRole or specific user, condition, timeoutHours, escalateTo)
- `WorkflowInstance` (definition, entityId, currentStep, status: PENDING/APPROVED/REJECTED/ESCALATED)
- `WorkflowAction` (instanceId, userId, action, comment, timestamp)
- `NotificationTemplate` (channel: EMAIL/SMS/WHATSAPP, subject, body with placeholders)
- `ScheduledJob` (cronExpression, jobClass, enabled)

**Services:**
- `WorkflowEngine` — evaluate conditions, advance steps, handle timeouts
- `EscalationService` — scheduled job to check overdue approvals and escalate
- `NotificationService` — send via email (JavaMail), SMS (Twilio), WhatsApp (Meta Cloud API)
- `SchedulerService` — dynamic cron job management (Spring `@Scheduled` + DB-driven)

**Configurable triggers (examples):**
- Invoice > ₹5,00,000 → 2-level approval (Manager → CFO)
- Leave application → Direct Manager → HR
- Purchase Order → Department Head → Finance → MD (above threshold)
- New Vendor → Procurement Head → Finance

**Estimated effort:** 3 weeks

---

### 2.4 Budgeting & Planning Module

**Entities to create:**
- `Budget` (name, fiscalYear, department/costCenter, status: DRAFT/APPROVED)
- `BudgetLine` (account, period: monthly, budgetedAmount)
- `BudgetRevision` (original budget, revised budget, reason, approvedBy)
- `CapexBudget` (asset category, amount, justification, linked to Asset module)

**Services:**
- `BudgetService` — create, approve, revise budgets
- `BudgetVarianceService` — compare actual GL spend vs budget in real time
- `ForecastService` — rolling 3-month forecast based on actuals trend

**Estimated effort:** 2 weeks

---

## Phase 3 — Industry Verticals (Weeks 23–52)

*Each vertical is a self-contained module. Enable per tenant via feature flag.*

---

### 3.1 Manufacturing Module

**Target industries:** Factories, workshops, fabrication shops, food processing, pharma.

**Entities to create:**
- `BillOfMaterials` (BOM — product, version, status)
- `BomComponent` (rawMaterial item, qty, unit, wastagePercent)
- `WorkOrder` (product, qty, plannedStartDate, plannedEndDate, status)
- `WorkOrderOperation` (sequence, workCenter, standardTimeMinutes)
- `WorkCenter` (machine or workstation, capacityPerDay)
- `ProductionEntry` (workOrder, date, producedQty, scrapQty, operator)
- `QualityCheck` (workOrder or productionBatch, parameter, expectedValue, actualValue, result: PASS/FAIL)
- `MachineMaintenance` (linked to existing Asset module)

**Services:**
- `BomService` — multi-level BOM explosion
- `WorkOrderService` — plan, release, complete work orders
- `ProductionService` — record daily output, auto-consume raw materials from inventory
- `QualityService` — inspection at receiving, in-process, final
- `OeeService` — compute Availability × Performance × Quality per machine

**UI:**
- BOM tree builder
- Work order Kanban (planned → in-progress → completed)
- Production floor entry (mobile-friendly)
- Quality inspection form
- OEE dashboard per machine

**Estimated effort:** 5 weeks

---

### 3.2 Real Estate & Property Management

**Target industries:** Builders, developers, property managers, co-working spaces.

**Entities to create:**
- `Project` extension: type = REAL_ESTATE
- `PropertyBlock` (tower/block within a project)
- `PropertyUnit` (flat/plot/shop, area, facing, floor, status: AVAILABLE/BOOKED/SOLD/HELD)
- `Booking` (customer, unit, bookingDate, bookingAmount, status)
- `PaymentSchedule` (linked to Booking — instalment-based or construction-linked)
- `DemandLetter` (auto-generated based on milestone completion)
- `Allotment` (formal allotment letter after booking confirmation)
- `Possession` (checklist, possession date, handover officer)
- `Broker` (name, RERA reg, commissionPercent)
- `SocietyMaintenance` (monthly charges post-possession)

**Services:**
- `PropertyService` — unit availability, floor plan management
- `BookingService` — booking lifecycle, cancellation, refund
- `DemandLetterService` — auto-trigger on milestone progress (links to BOQ milestone)
- `BrokerCommissionService` — compute and track payouts
- `RERAComplianceService` — project registration details, quarterly update reports

**Estimated effort:** 5 weeks

---

### 3.3 Retail & Point of Sale

**Target industries:** Retail stores, showrooms, pharmacies, supermarkets, restaurant chains.

**Entities to create:**
- `PosTerminal` (terminalCode, warehouseId, tenantId)
- `PosSession` (terminal, cashierUser, openingCash, closingCash, status)
- `PosOrder` (session, customer optional, orderLines, paymentMethod, status)
- `PosOrderItem` (item, qty, rate, discount, tax)
- `PosCashMovement` (type: SALE/REFUND/FLOAT/WITHDRAWAL, amount)
- `LoyaltyProgram` (pointsPerRupee, redemptionRate, expiryDays)
- `CustomerLoyaltyAccount` (customer, points, tier)
- `Promotion` (type: FLAT/PERCENT/BOGO, conditions, validFrom, validTo)

**Services:**
- `PosSessionService` — open/close sessions, Z-report
- `PosOrderService` — fast checkout, barcode scan, split payment
- `LoyaltyService` — earn/redeem points, tier upgrades
- `PromotionEngine` — evaluate applicable promotions per cart
- `DayEndReportService` — cash reconciliation, sales summary

**Estimated effort:** 4 weeks

---

### 3.4 Healthcare Module

**Target industries:** Clinics, hospitals, diagnostic centres, dental practices.

**Entities to create:**
- `Patient` (UHID, name, DOB, blood group, allergies)
- `Appointment` (doctor, patient, slot, type: OPD/REVIEW, status)
- `DoctorSchedule` (doctor employee, dayOfWeek, slots)
- `Consultation` (appointment, chiefComplaint, diagnosis ICD-10 code, prescription)
- `Prescription` (consultation, drugItems)
- `PrescriptionItem` (drug/inventory item, dose, frequency, duration)
- `LabOrder` (consultation, tests)
- `LabResult` (test, value, unit, referenceRange, flag: NORMAL/HIGH/LOW)
- `IpdAdmission` (patient, ward, bed, admitDate, dischargeDate)
- `Ward` (name, type, beds)
- `Bed` (bedNumber, ward, status: AVAILABLE/OCCUPIED)
- `InsuranceClaim` (patient, tpa, claimAmount, status)

**Services:**
- `AppointmentService` — slot booking, waitlist, SMS reminders
- `ConsultationService` — SOAP notes, diagnosis, prescription
- `PharmacyService` — dispense against prescription, deduct from inventory
- `LabService` — test ordering, result entry, report PDF
- `IpdService` — admission, ward round notes, discharge summary
- `BillingService` extension — OPD/IPD billing with insurance/TPA

**Estimated effort:** 6 weeks

---

### 3.5 Education & Institution Management

**Target industries:** Schools, colleges, coaching centres, training institutes.

**Entities to create:**
- `AcademicYear` (startDate, endDate, current)
- `Course` / `Class` / `Section`
- `Student` (name, rollNumber, DOB, guardian contacts, admissionDate)
- `Enrollment` (student, class, academicYear, status)
- `FeeStructure` (class, feeHeads: tuition/transport/hostel/exam, amount)
- `FeeCollection` (student, feeHead, amount, paymentMode, receiptNumber)
- `FeeInstalment` (due date, amount, status: PENDING/PAID/OVERDUE)
- `Timetable` (class, dayOfWeek, period, subject, teacher)
- `StudentAttendance` (date, class, presentStudentIds)
- `ExamSchedule` (exam, class, subject, date, maxMarks)
- `MarkEntry` (student, exam, subject, marksObtained)
- `ReportCard` (student, term, grades, remarks, generated PDF)
- `LibraryBook` (ISBN, title, author, copies)
- `LibraryIssue` (book, student/staff, issueDate, dueDate, returnDate, fine)
- `TransportRoute` (routeName, stops, vehicle, driver)
- `TransportAssignment` (student, route, stop)

**Services:**
- `AdmissionService` — online enquiry → admission → enrollment workflow
- `FeeService` — compute dues, collect payment, overdue reminders
- `AttendanceService` (student) — bulk mark via timetable
- `ExaminationService` — schedule, enter marks, compute grades, generate report cards
- `LibraryService` — issue/return, fine calculation, overdue alerts
- `TransportService` — route management, bus pass generation

**Estimated effort:** 5 weeks

---

### 3.6 Hotel & Hospitality Management

**Target industries:** Hotels, resorts, guesthouses, service apartments, banquet halls.

**Entities to create:**
- `RoomType` (category, baseRate, maxOccupancy, amenities)
- `Room` (roomNumber, floor, type, status: AVAILABLE/OCCUPIED/MAINTENANCE/HOUSEKEEPING)
- `Reservation` (guest, roomType, checkIn, checkOut, adults, children, source, status)
- `Folio` (reservation, chargeLines — room rent, F&B, laundry, etc.)
- `HousekeepingTask` (room, assignedStaff, taskType, status, completedAt)
- `RestaurantTable` (tableNumber, capacity, section)
- `FoodOrder` / `KOT` (table, waiter, items, sentToKitchenAt)
- `MenuCategory` / `MenuItem` (linked to inventory for consumption)
- `BanquetBooking` (hall, event, date, pax, packageDetails, advanceAmount)
- `GuestFeedback` (reservation, rating, comments, responseNote)

**Services:**
- `ReservationService` — availability check, booking, modification, cancellation
- `CheckInService` / `CheckOutService` — room assignment, folio settlement, invoice
- `HousekeepingService` — daily task assignment, status tracking
- `FolioService` — post charges, apply discounts, settle bill
- `RestaurantService` — table management, KOT printing, table bill
- `BanquetService` — event setup, BEO (Banquet Event Order) generation

**Estimated effort:** 5 weeks

---

### 3.7 Logistics & Transport Management

**Target industries:** Fleet operators, 3PL logistics providers, delivery businesses.

**Entities to create:**
- `Vehicle` (regNumber, type, make, model, fuelType, payload)
- `VehicleDocument` (RC, insurance, fitness, permit — with expiryDate alerts)
- `Driver` (linked to Employee, licenseNumber, licenseExpiry, badgeNumber)
- `Trip` (vehicle, driver, origin, destination, cargo, plannedDate, status)
- `TripExpense` (trip, fuelLitres, fuelCost, tollAmount, miscExpenses)
- `FreightBill` (trip, customer, weight, distance, ratePerKm, totalCharge)
- `DeliveryNote` (trip, consignmentItems, receiverSignature)
- `GpsLocation` (vehicleId, latitude, longitude, timestamp, speed) — for live tracking

**Services:**
- `TripService` — plan, assign, track, complete trips
- `FuelTrackingService` — mileage per vehicle, fuel efficiency alerts
- `FreightBillingService` — generate freight invoices, integrate with billing module
- `VehicleComplianceService` — alert 30/15/7 days before document expiry

**Estimated effort:** 3 weeks

---

## Phase 4 — Intelligence & Automation Layer (Weeks 53–68)

---

### 4.1 AI / ML Analytics Engine

**Components:**
- **Demand Forecasting** — use 12-month sales/inventory history, simple exponential smoothing or integration with Python microservice via REST
- **Cash Flow Forecasting** — predict next 90 days based on AR ageing, AP due dates, recurring expenses
- **Employee Attrition Risk Scoring** — based on attendance patterns, leave frequency, salary vs market
- **Project Delay Prediction** — from BOQ progress vs timeline trends
- **Expense Anomaly Detection** — flag expenses > 2σ from category mean
- **NLP Report Query** — natural language to pre-built report mapping ("show top customers this month")

**Implementation approach:**
- Phase 4a: Rule-based analytics (no ML dependency) — pure SQL aggregates surfaced as insight cards
- Phase 4b: Python microservice (FastAPI) for ML models, called via REST from Spring Boot
- Phase 4c: LLM integration (OpenAI / local Ollama) for NLP queries with structured output

**Estimated effort:** 6 weeks

---

### 4.2 Notifications & Communication Hub

**Channels:**
- Email — JavaMail / SendGrid API
- SMS — Twilio / AWS SNS / MSG91 (India)
- WhatsApp — Meta Cloud API / Twilio WhatsApp
- In-app — WebSocket push (Spring WebSocket / STOMP)
- Push notifications — Firebase FCM (for mobile app)

**Features:**
- Template editor per tenant (HTML email templates)
- Notification preferences per user (opt-in/out per channel per event type)
- Delivery status tracking (sent, delivered, read)
- Bulk notification for announcements
- Transactional events: invoice sent, payment received, leave approved, salary credited

**Estimated effort:** 2 weeks

---

### 4.3 Advanced BI & Reporting Engine

**Features:**
- **Custom Report Builder** — select entity, add fields, add filters, choose chart type (table/bar/line/pie), save as named report
- **Scheduled Reports** — send named report to email list on cron schedule
- **KPI Dashboard Builder** — drag-and-drop KPI cards per role
- **Export** — Excel (Apache POI), PDF (extend existing iText), CSV
- **Embed** — report URL embeddable in external portals

**Libraries:**
- Apache POI (Excel)
- iText (already in pom.xml — extend for report PDFs)
- Chart.js (already likely in frontend — extend for custom dashboards)
- JasperReports (optional — for pixel-perfect statutory report templates)

**Estimated effort:** 4 weeks

---

## Phase 5 — Platform & Ecosystem (Weeks 69–84)

---

### 5.1 Full Public REST API + API Gateway

**Deliverables:**
- OpenAPI 3.0 specification for all modules (Springdoc OpenAPI)
- API versioning (`/api/v1/`, `/api/v2/`)
- API key management per tenant (create, rotate, revoke)
- Rate limiting per API key (Bucket4j)
- Webhook outbound (subscribe to events: `invoice.created`, `payment.received`, etc.)
- SDK generation from OpenAPI spec (Java, Python, JavaScript)

**Estimated effort:** 3 weeks

---

### 5.2 Integration Connectors

| Connector | Purpose | Method |
|-----------|---------|--------|
| Tally Prime | Sync GL entries, vouchers | Tally XML bridge |
| Zoho Books | Bi-directional invoice sync | Zoho REST API |
| Shopify | Import orders as Sales Orders | Shopify Webhook |
| WooCommerce | Import orders | WC REST API |
| Razorpay | Payment links, auto-reconciliation | Razorpay Webhook |
| PayU / CCAvenue | Payment gateway | Redirect + Webhook |
| GSTN API | File GSTR, fetch notices | NIC GSTN API |
| NSDL / TRACES | TDS filing, Form 16A download | TRACES API |
| DigiLocker | Employee KYC document verification | DigiLocker API |
| Aadhaar (OTP eKYC) | Identity verification | UIDAI OKYC API |
| Banking (HDFC/ICICI) | Account statement auto-fetch | Account Aggregator API |
| WhatsApp Business | Transactional messages | Meta Cloud API |

**Estimated effort:** 5 weeks

---

### 5.3 Customer & Vendor Self-Service Portals

**Customer Portal:**
- View and download invoices (PDF)
- Make online payments (Razorpay integration)
- Track order / project status
- Raise support tickets
- View statement of account

**Vendor Portal:**
- View and acknowledge POs
- Submit invoices against GRNs
- Track payment status
- View vendor scorecard
- Upload KYC documents

**Implementation:** Separate Spring Security filter chain for portal URLs (`/portal/customer/**`, `/portal/vendor/**`) with magic link / OTP login (no password required).

**Estimated effort:** 3 weeks

---

### 5.4 Mobile App Layer

**Approach:** Progressive Web App (PWA) first — zero native build pipeline, works on iOS + Android.

**PWA features:**
- Offline-first with Service Worker + IndexedDB sync
- Home screen install prompt
- Push notifications via FCM

**Key mobile-optimised screens:**
- Attendance clock-in/clock-out with GPS stamp
- Daily site report submission (photos + notes)
- Expense capture with camera photo upload
- Approval actions (approve/reject with comment)
- Dashboard KPI cards

**Native app (Phase 5b):**
- React Native or Flutter wrapper over the REST API
- Biometric login
- Offline data sync for field workers in low-connectivity areas

**Estimated effort:** 4 weeks (PWA) + 6 weeks (native)

---

### 5.5 Multi-Company & Intercompany Accounting

**Entities to create:**
- `CompanyGroup` (holding company entity)
- `IntercompanyTransaction` (from company, to company, amount, nature)
- `ConsolidationRuleset` (elimination entries for intercompany)

**Features:**
- Switch between legal entities within same login session
- Intercompany sales/purchases with automatic contra entry
- Consolidated P&L and Balance Sheet
- Transfer pricing documentation

**Estimated effort:** 3 weeks

---

## Phase 6 — Security, Compliance & SaaS Hardening (Weeks 85–94)

---

### 6.1 Advanced Security

| Feature | Implementation |
|---------|---------------|
| Two-Factor Authentication (TOTP) | Google Authenticator / Authy — `java-otp` library |
| 2FA via SMS OTP | Twilio / MSG91 — 6-digit OTP, 5-minute TTL |
| IP Whitelisting per tenant | DB-stored allowed CIDR ranges, checked in JWT filter |
| Device Trust | Store device fingerprint on first login, flag new device login |
| Active Session Management | View all active sessions, remote logout |
| Field-level Encryption | Encrypt salary, bank account, Aadhaar fields using AES-256 |
| GDPR Tools | Data export per subject, right-to-erasure with audit trail |

**Estimated effort:** 3 weeks

---

### 6.2 SaaS Subscription & Tenant Lifecycle Management

**Entities to create:**
- `SubscriptionPlan` (name, maxUsers, maxStorage, features list, monthlyPrice)
- `TenantSubscription` (tenant, plan, startDate, endDate, status: TRIAL/ACTIVE/SUSPENDED/CANCELLED)
- `UsageMeter` (tenant, metricType: USERS/API_CALLS/STORAGE_MB, period, value)
- `Invoice` (subscription invoice — separate from business invoice)
- `FeatureFlag` (feature key, enabled, rolloutPercent — per tenant or global)

**Features:**
- Self-service onboarding: subdomain selection → plan selection → card payment → tenant provisioned
- Trial period (14 days) with automatic reminder at Day 7, Day 12, Day 14
- Usage alerts: "You have used 90% of your user limit"
- Tenant suspension on payment failure (read-only mode)
- In-app changelog and feature announcements
- Admin console: all tenants, MRR, churn, usage heatmap

**Estimated effort:** 3 weeks

---

### 6.3 Multi-Region & Regulatory Compliance Profiles

**Regions:**
- **India:** GST, TDS, PF, ESI, PT, FSSAI (food), RERA (real estate), Labour Law
- **UAE:** VAT (5%), WPS (Wage Protection System), mainland vs freezone rules
- **US:** Sales tax (state-wise), 1099-NEC generation, payroll tax (FICA)
- **UK:** VAT (20%), Making Tax Digital (MTD), PAYE

**Implementation:**
- `RegionProfile` entity (region code, taxRates JSON, complianceModules JSON)
- Tenant selects region on onboarding — activates applicable statutory modules
- Tax computation engine abstracted behind `TaxStrategy` interface per region

**Estimated effort:** 4 weeks

---

## Complete Feature Summary Table

| Phase | Module | Effort | Dependencies |
|-------|--------|--------|-------------|
| Pre | Performance Optimisations | 2 weeks | None — do first |
| 1 | General Ledger & Accounting | 6 weeks | Phase Pre |
| 1 | Sales CRM Pipeline | 3 weeks | Phase Pre |
| 1 | Payroll & Statutory Compliance | 4 weeks | GL, HR |
| 1 | Procurement 3-Way Match + SCM | 3 weeks | GL, Inventory |
| 2 | Warehouse Management | 3 weeks | Inventory, GL |
| 2 | Document Management | 2 weeks | FileStorage (exists) |
| 2 | Workflow & BPA Engine | 3 weeks | All Phase 1 modules |
| 2 | Budgeting & Planning | 2 weeks | GL |
| 3 | Manufacturing | 5 weeks | Inventory, WMS, GL |
| 3 | Real Estate & Property | 5 weeks | Construction (exists), GL |
| 3 | Retail & POS | 4 weeks | Inventory, GL |
| 3 | Healthcare | 6 weeks | HR, Inventory, GL |
| 3 | Education | 5 weeks | HR, Finance |
| 3 | Hotel & Hospitality | 5 weeks | POS, GL |
| 3 | Logistics & Transport | 3 weeks | Asset (exists), GL |
| 4 | AI/ML Analytics | 6 weeks | All data modules |
| 4 | Notifications & Comm Hub | 2 weeks | All modules |
| 4 | Advanced BI & Reporting | 4 weeks | All modules |
| 5 | REST API + API Gateway | 3 weeks | All modules |
| 5 | Integration Connectors | 5 weeks | API Gateway |
| 5 | Customer & Vendor Portals | 3 weeks | Sales, Procurement |
| 5 | Mobile PWA + Native App | 10 weeks | REST API |
| 5 | Multi-Company Accounting | 3 weeks | GL |
| 6 | Advanced Security (2FA, etc.) | 3 weeks | Auth (exists) |
| 6 | SaaS Subscription Management | 3 weeks | Tenant (exists) |
| 6 | Multi-Region Compliance | 4 weeks | GL, Payroll |
| **Total** | | **~94 weeks** | |

---

## Technical Architecture Decisions

### Database
- Stay on PostgreSQL — add `jsonb` columns for flexible configuration (salary structure, workflow definitions, feature flags)
- Add `pg_trgm` extension for full-text search on customers, vendors, inventory
- Partition large tables by tenant + year: `audit_logs`, `attendance`, `journal_entry_lines`
- Read replicas for reporting queries (separate datasource in Spring)

### Caching Strategy (Caffeine → Redis migration path)
- Phase 1–3: Caffeine (current) — sufficient for single-instance
- Phase 5+: Migrate to Redis for distributed caching across multiple instances
- Cache regions: `tenantMetadata` (1hr), `companySettings` (5min), `chartOfAccounts` (30min), `featureFlags` (5min), `tokenBlacklist` (24hr)

### Search
- Add `pg_trgm` trigram indexes for fuzzy search on all list pages
- Phase 5: Elasticsearch integration for cross-module full-text search (documents, transactions, customers)

### File Storage
- Phase 1–3: Local filesystem (current `./uploads`) — fine for single server
- Phase 4+: Migrate `FileStorageService` to S3-compatible object storage (AWS S3 / MinIO / Cloudflare R2) — single interface change

### Background Jobs
- Phase 1–2: Spring `@Scheduled` + `@Async` (current pattern)
- Phase 3+: Add Quartz Scheduler for distributed, DB-persisted job scheduling (payroll runs, report emails, reorder checks)

### Frontend Evolution
- Phase 1–3: Thymeleaf + Alpine.js + Chart.js (current pattern — fast to build)
- Phase 4+: Consider extracting high-interactivity modules (POS, workflow builder, BOM tree, report builder) as Vue.js or React SPAs served from the same Spring Boot app via static resources

### Module Packaging
- Current: Flat package structure in one module
- Phase 3+: Reorganise into feature packages:
  ```
  com.supererp.erp.modules.accounting
  com.supererp.erp.modules.sales
  com.supererp.erp.modules.manufacturing
  com.supererp.erp.modules.healthcare
  ...
  ```
- Phase 6+: Extract high-load modules (notifications, file storage, reports) to separate Spring Boot services behind an API gateway if needed

---

## Sprint Planning Guide (2-week sprints)

### Sprints 1–2: Performance fixes + GL foundations
- Fix all P1–P11 performance issues
- Create `ChartOfAccount`, `JournalEntry`, `JournalEntryLine`, `FiscalYear` entities
- Liquibase migrations for new tables
- Basic Journal Entry CRUD

### Sprints 3–5: General Ledger core
- Trial Balance, Balance Sheet, P&L wired to GL
- Auto-posting rules from existing billing and expense modules
- Bank account management

### Sprints 6–7: GST & Indian compliance in GL
- GSTR-1, GSTR-3B export
- TDS ledger tracking

### Sprints 8–9: Sales CRM pipeline
- Lead → Opportunity → Quotation → Sales Order
- Quotation PDF generation

### Sprints 10–12: Payroll compliance engine
- Salary structure, PF, ESI, PT
- Payslip PDF + bank disbursement file

### Sprints 13–15: Procurement 3-way match
- RFQ, GRN, Vendor Invoice matching
- Vendor scorecard

### Sprints 16–18: WMS + Workflow engine
- Warehouse locations, stock ledger
- Configurable approval workflows

### Sprints 19+: Industry verticals (one vertical per 3–4 sprints)

---

## Key Performance Indicators (Track These)

| KPI | Target |
|-----|--------|
| API response time (P95) | < 200ms |
| DB query count per page load | < 15 queries |
| Page load time (Thymeleaf render) | < 500ms |
| Concurrent users per instance | 200+ |
| Test coverage (new modules) | > 70% |
| Uptime SLA | 99.9% |

---

## Dependency Map (What Unlocks What)

```
Performance Fixes
      │
      ▼
General Ledger ──────────────────────────────────────────┐
      │                                                   │
      ├──► Sales CRM Pipeline                             │
      │         │                                         │
      │         ▼                                         │
      ├──► Payroll & Statutory ◄─── HR Module (exists)   │
      │                                                   │
      ├──► Procurement 3-Way Match ◄── Inventory (exists) │
      │         │                                         │
      │         ▼                                         │
      └──► Warehouse Management                           │
                │                                         │
                ▼                                         │
         Workflow Engine ◄──────────────────────────────-─┘
                │
                ▼
      ┌─────────────────────────┐
      │   Industry Verticals    │
      │  Manufacturing          │
      │  Real Estate            │
      │  Retail / POS           │
      │  Healthcare             │
      │  Education              │
      │  Hotel                  │
      │  Logistics              │
      └─────────────────────────┘
                │
                ▼
      ┌──────────────────────────┐
      │   Intelligence Layer     │
      │  AI Analytics            │
      │  BI & Reporting          │
      │  Notifications Hub       │
      └──────────────────────────┘
                │
                ▼
      ┌──────────────────────────┐
      │   Platform Layer         │
      │  REST API + Gateway      │
      │  Integration Connectors  │
      │  Mobile App              │
      │  Customer/Vendor Portals │
      └──────────────────────────┘
                │
                ▼
      ┌──────────────────────────┐
      │   SaaS Hardening         │
      │  2FA + Security          │
      │  Subscription Mgmt       │
      │  Multi-Region Compliance │
      └──────────────────────────┘
```

---

*Document version: 1.0 | Created: June 2026 | Stack: Java 17 + Spring Boot 3.2.5 + PostgreSQL*
