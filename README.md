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
