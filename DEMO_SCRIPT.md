# SpendWise: 8-Minute College Project Presentation & Demo Script

**Project Title**: SpendWise — Your money. Your clarity.  
**Tech Stack**: Java 21, Java Swing, JDBC, MySQL 8, Maven, FlatLaf, JFreeChart  
**Target Audience**: Academic Evaluators, External Examiners, Engineering Viva Panel  
**Presenters**: Student 1 (Database Architect & Backend) & Student 2 (Swing UI & Analytics Lead)

---

## Presentation Timeline Overview

| Timestamp | Phase / Topic | Presenter | Key Focus & Technical Talking Points |
| :--- | :--- | :--- | :--- |
| **0:00 – 1:00** | Landing Page & Architecture | Student 1 & 2 | Tagline, Java 21 LTS, OOP principles, clean layered architecture |
| **1:00 – 2:30** | Dashboard & Real-time Metrics | Student 2 | KPI cards, monthly trend chart, budget progress, multi-threaded SwingWorker |
| **2:30 – 4:00** | Transactions Ledger & DB Integrity | Student 1 | Composite foreign key, prepared statements, ACID compliance, ledger table |
| **4:00 – 5:15** | Categories & Delete Policy | Student 1 | Relational integrity, `ON DELETE RESTRICT`, safe user error handling |
| **5:15 – 6:30** | Budgets & Unique NULLs | Student 1 | Generated virtual column `category_key = IFNULL(category_id, 0)` |
| **6:30 – 7:30** | Analytics, What-If Simulator & CSV | Student 2 | SQL aggregations, rule-based spike detection, forecasting, non-persistent simulation |
| **7:30 – 8:00** | Settings, Diagnostics & Conclusion | Student 1 & 2 | Reduced animations, live DB health check, project takeaways |

---

## Step-by-Step Script Walkthrough

### 1. Welcome Screen & Project Introduction (0:00 – 1:00)
- **Student 2 (UI Lead)**:
  > *"Good morning, esteemed panel. We present **SpendWise**, built under the tagline **'Your money. Your clarity.'** It is a high-performance desktop workstation designed in pure Java 21 and MySQL 8. As you can see on our landing screen, we have engineered a dark fintech aesthetic powered by FlatLaf and custom Java2D vector rendering with smooth visual cues. When we click 'Open My Dashboard', the app seamlessly transitions into the core workspace without thread blocking."*
- **Action**: Click **"Open My Dashboard"** or press <kbd>Enter</kbd>.

---

### 2. Dashboard & Financial Intelligence (1:00 – 2:30)
- **Student 2 (UI Lead)**:
  > *"On the dashboard, SpendWise immediately aggregates our financial standing for the selected month (October 2026). Notice the four primary KPI cards: Total Income (Rs 95,000.00), Total Expenses (Rs 73,850.00), Net Savings (Rs 21,150.00), and Remaining Budget.*
  > *All database operations run strictly off the Event Dispatch Thread (EDT) using `SwingWorker` via our `AsyncWorker` utility, keeping the interface completely responsive.*
  > *Below the summary cards, the JFreeChart integration visualizes our category breakdown alongside a 6-month historical trend. Notice the Spending Intelligence cards: our forecast calculates end-of-month projected expenses with exact day-of-month awareness, and our unusual spending algorithm flags festive shopping that exceeded historical baselines by over 30%."*

---

### 3. Transactions Ledger & Composite Constraint Integrity (2:30 – 4:00)
- **Student 1 (Backend Lead)**:
  > *"Let us navigate to the Transactions screen. Here is our double-entry ledger. Notice how transaction indicators do not rely on color alone: we clearly format positive movements as `[+] Income` and expenses as `[-] Expense`, rendered with Indian rupee formatting (`Rs 1,23,456.00`).*
  > *Let us demonstrate our database constraint enforcement: If I click '+ Record Transaction', select 'EXPENSE', but attempt to choose 'Salary' (an Income category), the system rejects it at both the service and database levels. We enforced this in MySQL using a composite foreign key `(category_id, transaction_type) REFERENCES categories(category_id, category_type)`."*
- **Action**: Demonstrate filtering by description (e.g. type `"Rent"`), then click **"+ Record Transaction"** to log a sample transaction (e.g., Grocery: Rs 1,500.00). Point out the instant UI toast confirmation.

---

### 4. Category Management & Deletion Policy (4:00 – 5:15)
- **Student 1 (Backend Lead)**:
  > *"Under Category Management, categories are cleanly divided into Expense and Income tabs. Each row displays real-time transaction counts calculated via SQL.*
  > *Notice our deletion policy: In personal finance, deleting a category that has recorded transactions would corrupt audit history. Therefore, our schema enforces `ON DELETE RESTRICT`. If I attempt to delete 'Rent & Maintenance', which has active transactions, SpendWise blocks the deletion and presents an informative academic message explaining that the foreign key constraint protects historical data."*
- **Action**: Attempt to delete "Rent & Maintenance". Show the dialog: `"Category is in use by X transactions and cannot be deleted. Delete policy: ON DELETE RESTRICT."`

---

### 5. Budgets & Unique Generated Column (5:15 – 6:30)
- **Student 1 (Backend Lead)**:
  > *"On the Budgets screen, users can set both an overall monthly cap and individual category limits. A key technical challenge in standard SQL and MySQL InnoDB is that `UNIQUE` constraints allow multiple `NULL` values. Because an overall monthly budget has `category_id = NULL`, a standard unique key on `(user_id, year, month, category_id)` would permit duplicate overall budgets.*
  > *To solve this elegantly in MySQL 8, we implemented a virtual stored generated column: `category_key = IFNULL(category_id, 0)` under a `UNIQUE(user_id, budget_year, budget_month, category_key)` constraint. This mathematically guarantees exactly one overall budget per month while supporting individual category budgets."*
- **Action**: Highlight the budget progress bars changing color: Green (<80%), Amber (80-100%), and Red (>100% with exceeded amount).

---

### 6. Reports, What-If Simulator & CSV Export (6:30 – 7:30)
- **Student 2 (UI Lead)**:
  > *"Our Reports screen provides a bird's-eye view of cash flow. All aggregations (`SUM`, `AVG`, `GROUP BY`, `HAVING`) are executed in MySQL rather than loading raw entities into memory.*
  > *We also designed a pure What-If Spending Simulator. If an AIML student wants to see how a 25% cut in 'Dining Out' impacts monthly savings, they select the category and click 'Simulate'. The calculation is 100% client-side and mathematical — it never mutates the database.*
  > *Finally, users can click 'Export CSV' to produce a clean, spreadsheet-compatible CSV export matching exact database values."*
- **Action**: Run the What-If simulation for a category, then click **"Export CSV"** and save to disk.

---

### 7. Settings, Diagnostics & Conclusion (7:30 – 8:00)
- **Student 1 & 2**:
  > *"Finally, in Settings, users can test live database connectivity, configure threshold percentages, or toggle 'Reduce animations' for low-spec hardware.*
  > *In conclusion, SpendWise demonstrates real-world software engineering: rigorous schema normalization, composite referential integrity, multi-threaded UI responsiveness, and explainable financial intelligence. Thank you, and we welcome your questions!"*
