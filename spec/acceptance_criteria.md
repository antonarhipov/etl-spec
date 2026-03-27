# Acceptance Criteria: Temperature Data CSV Import Batch Job

---

## Category 1: Job Startup & Configuration

**AC-1.1 — Valid file path launches job**
- **WHEN** the application is started with a valid `--filePath=path/to/data.csv` CLI argument pointing to an existing file
- **THEN** the Spring Batch job is triggered automatically on startup
- **SHALL** the job enter STARTED status and begin processing the file

**AC-1.2 — Missing file path fails fast**
- **WHEN** the application is started without the `--filePath` job parameter
- **THEN** the application performs startup validation
- **SHALL** the job fail immediately with a descriptive error before any processing occurs

**AC-1.3 — Non-existent file path fails fast**
- **WHEN** the application is started with `--filePath` pointing to a file that does not exist on disk
- **THEN** the application performs file existence validation at startup
- **SHALL** the job fail immediately with a descriptive error and no records are written to the database

**AC-1.4 — Chunk size is configurable**
- **WHEN** the `batch.chunk-size` property is set in `application.properties`
- **THEN** the job uses that value as the commit interval
- **SHALL** records be committed to the database in chunks of that configured size (default: 100)

---

## Category 2: CSV Parsing

**AC-2.1 — Header row is skipped**
- **WHEN** a CSV file with a header row (`name,datetime,temp`) is provided
- **THEN** the reader processes the file with `linesToSkip=1`
- **SHALL** the header row not be inserted as a data record into the database

**AC-2.2 — Standard columns are extracted**
- **WHEN** a CSV file contains rows with comma-separated values in `name,datetime,temp` order
- **THEN** the reader maps each row by positional field order
- **SHALL** the `name`, `datetime`, and `temp` values be correctly extracted and available for processing

**AC-2.3 — Extra columns are ignored**
- **WHEN** a CSV file contains rows with more than three columns (e.g., `name,datetime,temp,extra_col`)
- **THEN** the reader maps only the first three columns by position
- **SHALL** the extra columns be silently ignored and the row be processed normally

**AC-2.4 — UTF-8 encoding is supported**
- **WHEN** a CSV file encoded in UTF-8 is provided, including rows with non-ASCII characters in the `name` field
- **THEN** the reader reads the file using UTF-8 encoding
- **SHALL** all characters be correctly decoded and stored in the database without corruption

**AC-2.5 — Datetime format is parsed correctly**
- **WHEN** a CSV row contains a `datetime` value formatted as `yyyy-MM-dd HH:mm:ss` (e.g., `2024-01-15 13:45:00`)
- **THEN** the reader parses the value into a `LocalDateTime`
- **SHALL** the parsed datetime be stored accurately in the `temperature_reading` table

---

## Category 3: Data Validation & Skip Behavior

**AC-3.1 — Row with null `name` is skipped**
- **WHEN** a CSV row has an empty or missing `name` field
- **THEN** the processor detects the invalid field
- **SHALL** the row be skipped, a WARN-level log entry be emitted containing the row details, and the skip count be incremented toward the skip limit

**AC-3.2 — Row with null `datetime` is skipped**
- **WHEN** a CSV row has an empty or missing `datetime` field
- **THEN** the processor detects the invalid field
- **SHALL** the row be skipped, a WARN-level log entry be emitted, and the skip count be incremented toward the skip limit

**AC-3.3 — Row with null `temp` is skipped**
- **WHEN** a CSV row has an empty or missing `temp` field
- **THEN** the processor detects the invalid field
- **SHALL** the row be skipped, a WARN-level log entry be emitted, and the skip count be incremented toward the skip limit

**AC-3.4 — Row with malformed datetime is skipped**
- **WHEN** a CSV row contains a `datetime` value that does not match the `yyyy-MM-dd HH:mm:ss` format (e.g., `15/01/2024` or `not-a-date`)
- **THEN** the parser throws a parse exception
- **SHALL** the row be skipped, a WARN-level log entry be emitted, and the skip count be incremented

**AC-3.5 — Row with non-numeric temperature is skipped**
- **WHEN** a CSV row contains a `temp` value that cannot be parsed as a decimal number (e.g., `"hot"`, `"N/A"`)
- **THEN** the parser throws a parse exception
- **SHALL** the row be skipped, a WARN-level log entry be emitted, and the skip count be incremented

**AC-3.6 — Skip limit not exceeded — job completes**
- **WHEN** the total number of invalid/skipped rows in the file is less than the configured skip limit (default: 10)
- **THEN** the job continues processing without interruption
- **SHALL** the job complete with status COMPLETED and all valid rows be processed

**AC-3.7 — Skip limit exceeded — job fails**
- **WHEN** the total number of invalid/skipped rows exceeds the configured skip limit (default: 10)
- **THEN** the job's skip limit threshold is breached
- **SHALL** the job terminate with status FAILED and no further records be committed after the threshold is crossed

---

## Category 4: Duplicate Detection

**AC-4.1 — Duplicate against existing DB record is skipped**
- **WHEN** a CSV row contains a `name` + `datetime` combination that already exists in the `temperature_reading` table
- **THEN** the writer attempts the INSERT and receives a `DuplicateKeyException` from the database
- **SHALL** the row be skipped (not inserted), a WARN-level log entry be emitted containing the duplicate `name` and `datetime`, and the duplicate counter be incremented

**AC-4.2 — Duplicate within the same CSV file is skipped**
- **WHEN** a CSV file contains two or more rows with the same `name` + `datetime` values
- **THEN** the first occurrence is inserted successfully, and subsequent occurrences trigger `DuplicateKeyException`
- **SHALL** each subsequent duplicate be skipped and counted, and only one record for that `name` + `datetime` pair exist in the database after the job

**AC-4.3 — Unique constraint is enforced at DB level**
- **WHEN** a row is inserted into `temperature_reading`
- **THEN** the database enforces the `UNIQUE (name, datetime)` constraint via DDL
- **SHALL** it be impossible to insert a duplicate `name` + `datetime` pair regardless of application-layer logic

**AC-4.4 — Non-duplicate rows are inserted normally**
- **WHEN** a CSV row contains a `name` + `datetime` combination that does not exist in the database
- **THEN** the writer executes `INSERT INTO temperature_reading (name, datetime, temp) VALUES (:name, :datetime, :temp)`
- **SHALL** the record be persisted in the database with the correct `name`, `datetime`, and `temp` values

---

## Category 5: Summary Reporting

**AC-5.1 — Summary is logged after job completes**
- **WHEN** the batch job finishes (regardless of outcome)
- **THEN** the `JobExecutionListener#afterJob` method is invoked
- **SHALL** a summary line be emitted at INFO log level in the format: `Inserted: N, Duplicates skipped: M`

**AC-5.2 — Summary is printed to stdout**
- **WHEN** the batch job finishes
- **THEN** the `JobExecutionListener#afterJob` method is invoked
- **SHALL** the summary line `Inserted: N, Duplicates skipped: M` also appear on standard output (stdout)

**AC-5.3 — Summary counts are accurate**
- **WHEN** a file with K valid unique rows and D duplicate rows is processed
- **THEN** the job inserts K records and skips D duplicates
- **SHALL** the summary report exactly `Inserted: K, Duplicates skipped: D`

**AC-5.4 — Duplicate WARN log contains identifying fields**
- **WHEN** a duplicate row is detected and skipped
- **THEN** a WARN-level log entry is produced
- **SHALL** the log entry include the `name` and `datetime` values of the duplicate row to enable traceability

---

## Category 6: Empty & Boundary Cases

**AC-6.1 — Header-only file completes successfully**
- **WHEN** the CSV file contains only the header row and no data rows
- **THEN** the reader finds no items to process after skipping the header
- **SHALL** the job complete with status COMPLETED, no records be inserted, and the summary report `Inserted: 0, Duplicates skipped: 0`

**AC-6.2 — Single valid row is inserted**
- **WHEN** the CSV file contains exactly one valid data row after the header
- **THEN** the job processes and writes that single row
- **SHALL** exactly one record appear in `temperature_reading` and the summary report `Inserted: 1, Duplicates skipped: 0`

**AC-6.3 — Temperature boundary values are stored as-is**
- **WHEN** a CSV row contains a `temp` value at the `DECIMAL(6,2)` boundary (e.g., `-273.15`, `9999.99`)
- **THEN** the value is passed through without application-level range validation
- **SHALL** the value be stored in the database exactly as provided, subject only to the DB column type constraint

**AC-6.4 — Temperature value exceeding DB column precision is rejected by DB**
- **WHEN** a CSV row contains a `temp` value outside the `DECIMAL(6,2)` range (e.g., `99999.99`)
- **THEN** the INSERT is attempted and the database rejects it
- **SHALL** the row be treated as a failed write, the skip count be incremented, and no partial data be stored

---

## Category 7: Idempotency & Re-runnability

**AC-7.1 — Re-running the same file is safe**
- **WHEN** the job is executed a second time with the same `--filePath` value pointing to a previously fully imported file
- **THEN** all rows from the file already exist in the database and trigger `DuplicateKeyException`
- **SHALL** the job complete with status COMPLETED, zero new records be inserted, and the summary report `Inserted: 0, Duplicates skipped: N` where N equals the total data rows in the file

**AC-7.2 — Each run creates a new job instance**
- **WHEN** the job is re-run with the same `filePath`
- **THEN** the job parameters include a timestamp component in addition to `filePath`
- **SHALL** a new Spring Batch job instance be created, allowing the re-run without being blocked by a prior COMPLETED instance for the same file

**AC-7.3 — Job restart is not supported**
- **WHEN** a job run fails mid-processing
- **THEN** the user re-launches the application with the same `filePath`
- **SHALL** a new job instance start from the beginning of the file; already-inserted records will be caught as duplicates, and the job SHALL complete successfully

---

## Category 8: Database Schema & Persistence

**AC-8.1 — Schema is managed by Flyway**
- **WHEN** the application starts for the first time against an empty database
- **THEN** Flyway executes migration `V1__create_temperature_reading.sql`
- **SHALL** the `temperature_reading` table exist with columns `id`, `name`, `datetime`, `temp` and a `UNIQUE (name, datetime)` constraint

**AC-8.2 — Stored `datetime` matches source value**
- **WHEN** a row with `datetime` value `2024-06-01 08:30:00` is inserted
- **THEN** the value is written to the MySQL `DATETIME` column without timezone conversion
- **SHALL** a query of the row return `datetime = 2024-06-01 08:30:00` exactly

**AC-8.3 — Stored `temp` preserves two decimal places**
- **WHEN** a row with `temp` value `23.50` is inserted
- **THEN** the value is stored in the `DECIMAL(6,2)` column
- **SHALL** a query of the row return `temp = 23.50` exactly

---

## Category 9: Integration Testing

**AC-9.1 — Integration test uses Testcontainers MySQL**
- **WHEN** the integration test suite is executed
- **THEN** Testcontainers spins up a real MySQL container
- **SHALL** all database interactions in the test run against the containerized MySQL instance, not an in-memory substitute

**AC-9.2 — Happy-path test: valid records are inserted**
- **WHEN** the integration test provides a CSV file with N valid, unique rows
- **THEN** the job runs to completion against the Testcontainers MySQL instance
- **SHALL** exactly N rows appear in `temperature_reading` and the job status be COMPLETED

**AC-9.3 — Duplicate-skip test: duplicates are counted and not inserted**
- **WHEN** the integration test provides a CSV file containing rows that already exist in the database
- **THEN** the job runs and encounters `DuplicateKeyException` for those rows
- **SHALL** no duplicate rows be inserted, the duplicate counter match the number of pre-existing rows, and the job status be COMPLETED

**AC-9.4 — Summary output is verifiable in tests**
- **WHEN** the integration test captures log output or stdout after the job completes
- **THEN** the `JobExecutionListener#afterJob` emits the summary
- **SHALL** the captured output contain the string `Inserted: N, Duplicates skipped: M` with the correct values for that test scenario
