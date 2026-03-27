### Requirements: Temperature Data CSV Import Batch Job

#### Overview
Implement a Spring Batch job using JDBC to import temperature data from CSV files into a database.

#### Functional Requirements

1. **CSV Parsing**
   - Read CSV files containing temperature data.
   - Extract the following columns from each row: `name`, `datetime`, and `temp`.

2. **Uniqueness Constraint**
   - The combination of `name` and `datetime` must be unique in the database.

3. **Duplicate Handling**
   - Duplicate entries (same `name` + `datetime` pair) must be detected during import.
   - Duplicates must be skipped (not inserted into the database).
   - Duplicates must be reported (logged or otherwise surfaced).

4. **Summary Report**
   - After the job completes, print a summary containing:
     - Total number of records successfully inserted into the database.
     - Total number of duplicate records detected and skipped.

#### Technical Requirements

5. **Spring Batch with JDBC**
   - Use Spring Batch as the batch processing framework.
   - Use JDBC (not JPA/Hibernate) for database operations.

6. **Integration Testing with Testcontainers**
   - Use Testcontainers to provide a real database instance for integration tests.
   - Integration tests must verify:
     - Successful import of valid records.
     - Correct detection and skipping of duplicate entries.
     - Accuracy of the summary report (inserted count and duplicate count).
