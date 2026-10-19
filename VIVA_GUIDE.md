# SpendWise: Academic Viva & Technical Evaluation Guide

This guide is designed for the two-student project presentation of **SpendWise** (AIML Second-Year Project). It provides rigorous, explainable answers to 40 likely viva questions, a genuine OOP concepts table, an exhaustive DBMS concepts table, and a balanced two-person presentation division.

---

## Part 1: Team Division & Cross-Review Questions

### Suggested Presentation Split
- **Member 1 (Database & Repositories Lead)**:
  - Architecture: Database design, 3NF normalization, foreign key constraints, composite keys, generated virtual column `category_key`, `MoneyUtil`, JDBC `PreparedStatement`, transaction isolation, SQL aggregations.
- **Member 2 (Swing UI & Analytics Lead)**:
  - Architecture: Swing threading model (EDT and `SwingWorker`), FlatLaf dark theme, Java2D vector rendering, event handling, MVC/Service layer separation, rule-based analytics (unusual spending detection, leap-year forecasting, what-if simulator), CSV export.

### Cross-Review Questions (Testing Each Other's Knowledge)
1. **Member 2 asks Member 1**: *"Why could we not use a standard `UNIQUE(user_id, budget_year, budget_month, category_id)` constraint for overall budgets?"*
   - **Answer**: Standard SQL and MySQL InnoDB allow multiple `NULL` values in a unique index. Because overall budgets have `category_id = NULL`, a standard constraint would permit multiple overall budgets for the same month. We introduced the generated column `category_key = IFNULL(category_id, 0)` to map `NULL` to `0`, ensuring uniqueness.
2. **Member 1 asks Member 2**: *"Why is database access forbidden directly inside a `JButton` `ActionListener` in Swing?"*
   - **Answer**: `ActionListener` runs on the Event Dispatch Thread (EDT). Running blocking network or database I/O on the EDT freezes the UI, causes "Not Responding" beachballs, and halts repainting. We delegate database tasks to `SwingWorker` via `AsyncWorker.execute` and update UI components on `done()`.

---

## Part 2: Object-Oriented Programming (OOP) Mapping Table

| OOP Concept | Class / Interface | Method / Construct | Academic Justification (Why it exists) |
| :--- | :--- | :--- | :--- |
| **Encapsulation** | `com.spendwise.model.Transaction` | Private fields + getters/setters | Shields transaction state; amount and dates can only be accessed through controlled accessors. |
| **Abstraction** | `com.spendwise.repository.TransactionRepository` | `List<Transaction> findFiltered(...)` | Exposes *what* data can be queried without exposing *how* (JDBC SQL, connection handling). |
| **Inheritance** | `com.spendwise.ui.components.StyledButton` | `extends JButton` | Reuses core button behaviors (focus, accessibility, action listeners) while overriding custom rendering. |
| **Polymorphism** | `com.spendwise.ui.screens.BaseScreen` | `void onMonthChanged(int year, int month)` | `MainFrame` invokes `onMonthChanged` polymorphically across Dashboard, Transactions, and Budgets. |
| **Method Overriding**| `com.spendwise.ui.components.SummaryCard` | `@Override protected void paintComponent(Graphics g)` | Customizes Java2D background rounding and border painting while delegating child layout to super. |
| **Method Overloading**| `com.spendwise.util.MoneyUtil` | `of(BigDecimal amount)` vs `of(double amount)` | Provides convenient numeric conversions while standardizing on scale 2 and `RoundingMode.HALF_UP`. |
| **Composition** | `com.spendwise.service.TransactionService` | `private final TransactionRepository transactionRepo;` | `TransactionService` has-a repository; promotes loose coupling and testability via dependency injection. |
| **Interfaces** | `com.spendwise.repository.CategoryRepository` | `interface CategoryRepository` | Allows substituting real JDBC implementations with `FakeCategoryRepository` for isolated unit tests. |
| **Enums** | `com.spendwise.model.TransactionType` | `enum TransactionType { INCOME, EXPENSE }` | Enforces type safety at compile time, eliminating invalid string typos in business logic. |
| **Custom Exceptions**| `com.spendwise.service.ValidationException` | `extends RuntimeException` | Distinguishes client validation errors (e.g. negative amount) from catastrophic database failures. |
| **Exception Wrapping**| `com.spendwise.service.DataAccessException` | `new DataAccessException("...", sqlException)` | Prevents leaky abstractions by wrapping raw `SQLException` into an unchecked domain exception. |
| **Static Constants** | `com.spendwise.ui.theme.AppTheme` | `public static final Color BG_DARK` | Centralizes color tokens and fonts across the entire application for design consistency. |

---

## Part 3: Database Management Systems (DBMS) Mapping Table

| DBMS Concept | SQL Query / Feature in Project | Code Location | Academic Explanation |
| :--- | :--- | :--- | :--- |
| **DDL (Definition)** | `CREATE TABLE IF NOT EXISTS transactions (...)` | `database/schema.sql` | Defines schema structure, primary keys, foreign keys, and column types. |
| **DML (Manipulation)**| `INSERT INTO transactions (...) VALUES (?, ?, ?, ?, ?, ?)` | `JdbcTransactionRepository.java` | Inserts financial records dynamically using parameterized inputs. |
| **Primary Key** | `transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY` | `database/schema.sql` | Uniquely identifies each record; creates a clustered B-tree index in InnoDB. |
| **Composite Unique Key**| `UNIQUE (category_id, category_type)` | `database/schema.sql` | Enables composite foreign key enforcement between categories and transactions. |
| **Composite Foreign Key**| `FOREIGN KEY (category_id, transaction_type) REFERENCES categories(category_id, category_type)` | `database/schema.sql` | Rejects mismatched transaction types (e.g. Income category on Expense transaction) at DB level. |
| **Referential Action** | `ON DELETE RESTRICT` on categories | `database/schema.sql` | Prevents deleting categories with historical transactions, preserving audit history. |
| **Virtual Generated Column**| `category_key BIGINT GENERATED ALWAYS AS (IFNULL(category_id, 0)) STORED` | `database/schema.sql` | Eliminates duplicate NULL issues in unique indexes; stored for fast index lookup. |
| **Composite Index** | `CREATE INDEX idx_transactions_user_date ON transactions (user_id, transaction_date)` | `database/schema.sql` | Accelerates monthly range queries and sort operations on user financial records. |
| **SQL Aggregations** | `SUM(amount)`, `COUNT(*)`, `AVG(amount)` | `JdbcReportRepository.java` | Pushes computations to the database engine instead of loading millions of rows into Java. |
| **GROUP BY & HAVING** | `GROUP BY c.category_id HAVING total_amount > 0` | `JdbcReportRepository.java` | Groups spending by category and filters out unused categories before returning data. |
| **Subqueries** | `(SELECT COUNT(*) FROM transactions t WHERE t.category_id = c.category_id)` | `JdbcCategoryRepository.java` | Correlated subquery computing live transaction counts per category. |
| **Date Queries** | `YEAR(transaction_date) = ? AND MONTH(transaction_date) = ?` | `JdbcReportRepository.java` | Extracts calendar components for accurate monthly ledger scoping. |
| **PreparedStatement**| `conn.prepareStatement(sql)` with positional parameters | All Repository classes | Precompiles SQL, reuses query execution plans, and 100% prevents SQL Injection. |
| **Connection Safety**| `try (Connection conn = ...)` | All Repository classes | Guarantees automatic connection cleanup and prevents database socket connection leaks. |

---

## Part 4: 40 Viva Questions and Academic Answers

### Category A: Database Architecture & SQL (Questions 1–10)
1. **What database normalization level does SpendWise achieve?**
   - *Answer*: Third Normal Form (3NF). Every non-key attribute depends solely on the primary key (1NF: atomic attributes, 2NF: no partial key dependencies, 3NF: no transitive dependencies).
2. **Why use DECIMAL(12,2) instead of DOUBLE or FLOAT in MySQL?**
   - *Answer*: `FLOAT` and `DOUBLE` use binary floating-point representation (IEEE 754), which causes precision loss (e.g. 0.1 + 0.2 = 0.30000000000000004). `DECIMAL` stores exact fixed-point numerical digits, mandatory for financial auditing.
3. **How does the database prevent an income category from being used in an expense?**
   - *Answer*: By establishing a composite unique constraint `UNIQUE (category_id, category_type)` in `categories` and a matching composite foreign key `(category_id, transaction_type)` in `transactions`.
4. **Explain how `ON DELETE RESTRICT` works in SpendWise.**
   - *Answer*: If a category has any existing transactions, attempting a `DELETE` triggers SQL Error 1451. SpendWise catches this and explains to the user that categories in use cannot be deleted.
5. **Why can standard MySQL UNIQUE indexes have duplicate NULL entries?**
   - *Answer*: In SQL-92 and MySQL InnoDB, `NULL` represents an unknown value. Since `NULL != NULL`, multiple rows can possess `NULL` without violating uniqueness.
6. **How did SpendWise solve the duplicate NULL budget problem?**
   - *Answer*: We added a stored generated column `category_key = IFNULL(category_id, 0)` and placed a `UNIQUE (user_id, budget_year, budget_month, category_key)` constraint on it.
7. **What is an index and why did you index `(user_id, transaction_date)`?**
   - *Answer*: An index is a B-tree data structure that accelerates lookups. We indexed `(user_id, transaction_date)` because monthly summaries, dashboard metrics, and date-range filters query on these two columns constantly.
8. **What is SQL Injection and how is it prevented?**
   - *Answer*: SQL Injection occurs when untrusted user input alters SQL query structure. SpendWise strictly uses `PreparedStatement` with parameterized placeholders (`?`), treating inputs as literals rather than executable SQL.
9. **Why is computation done in SQL rather than loading all rows into Java?**
   - *Answer*: Database servers optimize aggregations using B-tree indexes, execution plans, and memory caches. Loading all rows into Java wastes network bandwidth and JVM memory.
10. **What is the difference between `WHERE` and `HAVING` in SQL?**
    - *Answer*: `WHERE` filters rows before aggregation; `HAVING` filters aggregated group records (e.g. `HAVING total_amount > 0`).

---

### Category B: Java 21 & Backend Services (Questions 11–20)
11. **Why is `BigDecimal` paired with `RoundingMode.HALF_UP` in Java?**
    - *Answer*: `BigDecimal` provides arbitrary-precision arithmetic. `HALF_UP` implements standard banking/commercial rounding where values >= 0.5 round to the nearest higher neighbor.
12. **What are Java 21 Records and where did you use them?**
    - *Answer*: Records (`record`) are immutable data carriers that automatically generate constructors, getters, `equals()`, `hashCode()`, and `toString()`. We used them for DTOs like `MonthlySummary`, `CategorySpending`, and `SpendingForecast`.
13. **Why are there no code comments in your Java files?**
    - *Answer*: We adhere to clean code principles: self-documenting method names, explicit variable names, single-responsibility classes, and zero clutter.
14. **How does SpendWise compute Net Savings and Savings Rate?**
    - *Answer*: Net Savings = `Total Income - Total Expenses`. Savings Rate = `(Net Savings / Total Income) * 100`. When income is zero, Savings Rate safely returns 0.0% to prevent division by zero.
15. **Explain the rule-based unusual spending algorithm.**
    - *Answer*: It calculates a 3-month baseline average for the exact same elapsed day-of-month window (e.g. days 1 to 19). If current spending exceeds the baseline by >= 30% AND >= Rs 500, it flags a potential unusual spending pattern.
16. **Why require at least 2 months of history before flagging unusual spending?**
    - *Answer*: With 0 or 1 month of prior history, there is insufficient baseline stability. Requiring 2 full months prevents noisy false alarms.
17. **How does the spending forecast handle leap years?**
    - *Answer*: It utilizes `YearMonth.of(year, month).lengthOfMonth()`, which evaluates the Gregorian calendar rules in Java time, returning 29 for February 2024 and 28 for February 2025.
18. **What does the What-If Simulator do, and does it write to MySQL?**
    - *Answer*: It calculates potential savings and revised balances based on percentage cuts to chosen categories. It is a pure mathematical calculation and never writes to the database.
19. **What is the difference between `ValidationException` and `DataAccessException`?**
    - *Answer*: `ValidationException` indicates invalid user input (e.g. negative amount), while `DataAccessException` wraps operational infrastructure failures (e.g. DB connection dropped).
20. **Why use `try-with-resources` for JDBC objects?**
    - *Answer*: Any class implementing `AutoCloseable` is automatically closed when exiting the block, preventing resource leaks even if exceptions occur.

---

### Category C: Java Swing UI & Event Handling (Questions 21–30)
21. **What is the Event Dispatch Thread (EDT)?**
    - *Answer*: Swing is single-threaded; all GUI rendering, component layouts, and user event handling take place on the EDT.
22. **What happens if a database query is executed directly on the EDT?**
    - *Answer*: The entire user interface freezes, buttons cannot be clicked, animations stop, and the operating system marks the window as unresponsive.
23. **How does `AsyncWorker` solve the EDT blocking problem?**
    - *Answer*: It wraps `SwingWorker`: `doInBackground()` runs the database query on a background thread pool, and `done()` posts results back to the EDT.
24. **Why did you use FlatLaf instead of the default Swing look and feel?**
    - *Answer*: FlatLaf provides modern dark styling, HiDPI scaling, rounded corners, clean fonts, and consistent cross-platform appearance on macOS, Windows, and Linux.
25. **Why use Java2D vector icons instead of raster images or emojis?**
    - *Answer*: Java2D vector paths render smoothly at any DPI resolution without blurriness, have zero external asset dependencies, and never suffer platform emoji font differences.
26. **How does SpendWise stop animations when screens are hidden?**
    - *Answer*: `WelcomeScreen` implements `HierarchyListener`. When `isShowing()` returns false, the 30fps animation timer is immediately stopped.
27. **Why is absolute positioning (`null` layout) discouraged in Swing?**
    - *Answer*: Absolute positioning breaks across different screen resolutions, OS window scales, and font sizes. SpendWise strictly uses layout managers (`BorderLayout`, `BoxLayout`, `GridLayout`, `FlowLayout`).
28. **How do transaction indicators support accessibility?**
    - *Answer*: In addition to color (emerald/crimson), transaction rows display text indicators `[+] Income` and `[-] Expense`, ensuring accessibility for color-blind users.
29. **What is `CardLayout` and how is it used in `MainFrame`?**
    - *Answer*: `CardLayout` treats a container as a deck of cards where only one is visible at a time. We use it to switch screens without destroying and recreating panels.
30. **What is the purpose of `Toast` notifications?**
    - *Answer*: They provide non-intrusive, floating feedback that disappears automatically after 3 seconds, confirming actions without forcing the user to dismiss a dialog.

---

### Category D: Testing, Build & Architecture (Questions 31–40)
31. **Why did you use hand-written fake repositories instead of Mockito for unit tests?**
    - *Answer*: Hand-written fakes provide explicit, transparent, and explainable in-memory behavior without bytecode manipulation, making tests easy to explain to examiners.
32. **What is the purpose of `spendwise_test`?**
    - *Answer*: It isolates integration tests from live project data. Before each test, test tables are reset to ensure deterministic test runs.
33. **Why is `application.properties` ignored in `.gitignore`?**
    - *Answer*: To prevent database credentials from ever being committed to source control or public repositories.
34. **What does `mvn clean compile` do?**
    - *Answer*: It deletes the `target/` directory, copies project resources, and invokes `javac` with Java 21 release flags to produce `.class` files.
35. **What is the purpose of Maven Surefire plugin?**
    - *Answer*: It discovers and executes JUnit 5 test classes during the `test` phase and generates test summary reports.
36. **How does JFreeChart integrate into Swing?**
    - *Answer*: JFreeChart creates a chart object that is rendered inside an `org.jfree.chart.ChartPanel`, which is a standard Swing `JPanel`.
37. **What is the difference between unit tests and integration tests in SpendWise?**
    - *Answer*: Unit tests verify service business logic in memory using fake repositories. Integration tests execute against the real MySQL server (`spendwise_test`) to verify SQL queries and constraints.
38. **How does SpendWise format Indian currency numbers?**
    - *Answer*: `MoneyUtil` formats numbers according to the Indian numbering system: the last 3 digits are grouped, followed by groups of 2 digits (e.g. `Rs 1,23,456.00`).
39. **Why is user ownership (`user_id`) kept if the app has a default local user?**
    - *Answer*: It keeps the schema future-proof, maintains relational integrity in the ER diagram, and allows multi-user expansion without restructuring the database.
40. **How does SpendWise guarantee data persistence across restarts?**
    - *Answer*: All creates, updates, and deletes are committed immediately to MySQL InnoDB storage on disk via JDBC connections.
