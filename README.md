# SpendWise: Your money. Your clarity.

A complete database-backed personal finance desktop application built with Java 21, Java Swing, JDBC, and MySQL 8.

## Project Structure

```text
spendwise/
├── pom.xml
├── README.md
├── .gitignore
├── database/
│   ├── schema.sql
│   └── sample_data.sql
└── src/
    ├── main/
    │   ├── java/com/spendwise/
    │   │   ├── Main.java
    │   │   ├── database/
    │   │   ├── model/
    │   │   ├── repository/
    │   │   ├── service/
    │   │   ├── ui/
    │   │   │   ├── components/
    │   │   │   ├── screens/
    │   │   │   └── theme/
    │   │   └── util/
    │   └── resources/
    │       ├── application.properties.example
    │       └── application.properties (gitignored)
    └── test/
        ├── java/com/spendwise/
        └── resources/
            └── application.properties (gitignored)
```

## Prerequisites

- Java 21 (JDK 21+)
- Apache Maven 3.9+
- MySQL 8.0+

## Setup & Configuration

1. Copy the example configuration file:
   ```bash
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   ```
2. Update `src/main/resources/application.properties` with your database credentials.

## Database Design Decisions

- **Single Local User & Ownership**: A `users` table holds the default local user (`user_id = 1`). All entities (`categories`, `transactions`, `budgets`) retain `user_id` as a foreign key so ownership checks and ER relationships are meaningful.
- **Composite Type Integrity**: `categories` enforces `UNIQUE(category_id, category_type)`. The `transactions` table uses a composite foreign key `(category_id, transaction_type) REFERENCES categories(category_id, category_type)`, guaranteeing at the database engine level that an EXPENSE transaction cannot point to an INCOME category.
- **Delete Policy**: `ON DELETE RESTRICT` is enforced on categories in transactions. Categories with existing transactions cannot be deleted.
- **Budget Uniqueness with NULLs**: In standard SQL and MySQL InnoDB, multiple `NULL` values do not violate a `UNIQUE` constraint. Since an overall monthly budget has `category_id = NULL` while category budgets have a specific `category_id`, a standard `UNIQUE(user_id, budget_year, budget_month, category_id)` would permit multiple overall budgets per month. To solve this, a generated column `category_key = IFNULL(category_id, 0)` is indexed under `UNIQUE(user_id, budget_year, budget_month, category_key)`. When `category_id` is `NULL`, `category_key` evaluates to `0`, ensuring exactly one overall budget per month.
- **Monetary Precision**: All monetary values use MySQL `DECIMAL(12,2)` and Java `BigDecimal` with scale 2 and `RoundingMode.HALF_UP`, centralized in `MoneyUtil`.

## Compilation & Execution

- Compile the project:
  ```bash
  mvn clean compile
  ```
- Run the application:
  ```bash
  mvn exec:java
  ```
- Run test suite:
  ```bash
  mvn test
  ```

