### Requirements: Temperature Data CSV Import Batch Job

---

#### Overview

Implement a Spring Batch job using JDBC that imports temperature data from CSV files into a MySQL database. The job must handle duplicate detection, report ignored entries, and print a summary upon completion.

---

#### Functional Requirements

##### FR-1: CSV File Reading

- The batch job **must** read input data from one or more CSV files.
- Each CSV file contains at least the following columns: `name`, `datetime`, and `temp`.
- The job **must** extract only the `name`, `datetime`, and `temp` columns; any other columns in the CSV file **must** be ignored.

##### FR-2: Data Model

- A database table **must** store the imported temperature records with (at minimum) the following columns:
  - `name` — location or sensor name (string).
  - `datetime` — timestamp of the temperature reading.
  - `temp` — temperature value (numeric).
- The combination of `name` and `datetime` **must** be enforced as a **unique constraint** in the database schema (managed via Flyway migration).

##### FR-3: Duplicate Detection and Handling

- Before or during insertion, the job **must** detect duplicate entries — records whose `name` and `datetime` pair already exists in the database.
- Duplicate entries **must not** be inserted into the database; they **must** be silently skipped (ignored) without causing the job to fail.
- Each skipped duplicate **must** be reported (e.g., logged) so that it is traceable.

##### FR-4: Job Completion Summary

- Upon job completion the application **must** print a summary containing:
  - The total number of records successfully inserted into the database.
  - The total number of duplicate records detected and skipped.

---

#### Non-Functional Requirements

##### NFR-1: Technology Stack

| Concern            | Technology                          |
|--------------------|-------------------------------------|
| Batch framework    | Spring Batch (spring-boot-starter-batch) |
| Persistence        | JDBC (spring-boot-starter-batch-jdbc) |
| Database           | MySQL                               |
| Schema management  | Flyway (flyway-mysql)               |
| Testing            | Testcontainers (MySQL), JUnit 5     |

##### NFR-2: Spring Batch Job Structure

- The job **should** follow the standard Spring Batch reader → processor → writer pattern:
  - **ItemReader** — reads rows from the CSV file (e.g., `FlatFileItemReader`).
  - **ItemProcessor** — validates and/or checks for duplicates.
  - **ItemWriter** — writes non-duplicate records to the database via JDBC.

##### NFR-3: Database Schema Migration

- The temperature table schema **must** be created and maintained through Flyway migrations.
- The unique constraint on (`name`, `datetime`) **must** be part of the migration script.

---

#### Integration Testing Requirements

##### TR-1: Testcontainers

- Integration tests **must** use Testcontainers to spin up a real MySQL container — no in-memory database substitutes.
- The existing `TestcontainersConfiguration` class **should** be leveraged for container lifecycle management.

##### TR-2: Test Scenarios

| # | Scenario                              | Expected Outcome                                                                                  |
|---|---------------------------------------|---------------------------------------------------------------------------------------------------|
| 1 | Import a CSV with all unique records  | All records are inserted; duplicate count is 0.                                                   |
| 2 | Import a CSV containing duplicate rows (same `name` + `datetime`) | Unique records are inserted; duplicates are skipped; counts in the summary are correct. |
| 3 | Re-run the job with the same CSV      | Previously inserted records are detected as duplicates; no new inserts; summary reflects this.    |
| 4 | CSV with extra columns                | Extra columns are ignored; only `name`, `datetime`, and `temp` are imported.                      |

---

#### Acceptance Criteria

1. Running the batch job with a valid CSV file inserts all unique `(name, datetime, temp)` records into the database.
2. Duplicate `(name, datetime)` pairs — whether within the same file or across multiple runs — are detected, skipped, and reported.
3. A summary line is printed at the end of the job showing inserted count and duplicate count.
4. All integration tests pass using Testcontainers with a MySQL container.
5. Database schema is managed by Flyway and includes the unique constraint on `(name, datetime)`.
