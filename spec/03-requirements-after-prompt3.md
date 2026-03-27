### Requirements Analysis: Temperature Data CSV Import Batch Job

---

### 1. AMBIGUITIES

- **"CSV files" (plural):** Does the job process a single CSV file per run, or multiple CSV files from a directory? If multiple, should they be processed sequentially or in parallel?
- **"Duplicate entries should be reported and ignored":** What does "reported" mean — logged to console, written to a separate reject file, or stored in a database table? At what level should duplicates be reported (per-file, per-run)?
- **"Print the summary":** Where should the summary be printed — to stdout/log, returned as a job execution result, or both?
- **"temp" column:** The data type is unspecified. Is it a decimal (e.g., 23.5), integer, or string? What unit (Celsius, Fahrenheit, Kelvin)?
- **"datetime" column:** What format is the datetime in the CSV? ISO 8601? A custom pattern? Does it include timezone information?

---

### 2. MISSING INFORMATION

- **CSV file location/path:** How is the input file path provided — command-line argument, application property, or watched directory?
- **CSV format details:** Delimiter (comma, semicolon, tab), quoting, header row presence, encoding (UTF-8?).
- **Database schema:** Target table name, column types, and any additional columns (e.g., auto-generated ID, import timestamp).
- **Database type:** PostgreSQL, MySQL, H2, or other? (Testcontainers choice depends on this.)
- **"name" column semantics:** What does "name" represent — a sensor name, location name, station ID?
- **Error handling beyond duplicates:** What happens if a row has missing fields, malformed datetime, or invalid temperature values?
- **Chunk size / commit interval:** No batch processing tuning parameters specified.
- **Job scheduling:** Is this a one-time run, triggered on demand, or scheduled (cron)?

---

### 3. IMPLICIT ASSUMPTIONS

- **The CSV has a header row** with columns named "name", "datetime", and "temp" (or mappable to these).
- **Duplicates are determined within the database**, not just within the current file — i.e., re-running the job with the same file should skip already-imported records.
- **The unique constraint (name + datetime) is enforced at the database level**, not just in application logic.
- **Spring Boot is the application framework** (given Spring Batch + JDBC context and existing project structure).
- **PostgreSQL is the target database** (common with Testcontainers + Spring Batch JDBC; needs confirmation).
- **The job runs as a command-line application** (Spring Boot batch job triggered on startup).

---

### 4. EDGE CASES

- **Empty CSV file:** Should the job succeed with a summary of 0 inserts and 0 duplicates, or fail?
- **CSV with only a header row and no data rows.**
- **Duplicate rows within the same CSV file** (not just duplicates against existing DB records).
- **Null or empty values** in name, datetime, or temp columns.
- **Extremely large CSV files:** Memory and performance considerations for batch processing.
- **Malformed rows:** Wrong number of columns, unparseable datetime, non-numeric temperature.
- **Concurrent job executions:** What if two job instances run simultaneously with overlapping data?
- **Re-runnability:** If the job fails mid-way, can it be restarted without re-inserting already-processed records?
- **Temperature boundary values:** Negative temperatures, extremely high/low values — any validation needed?
- **Extra columns in CSV:** Should additional columns beyond name/datetime/temp be silently ignored?

---

### 5. CLARIFYING QUESTIONS

| # | Question | Why It Matters |
|---|----------|---------------|
| 1 | What database engine should be used (PostgreSQL, MySQL, H2)? | Determines Testcontainers image, SQL dialect, DDL scripts, and JDBC driver dependency. |
| 2 | What is the expected datetime format in the CSV (e.g., `yyyy-MM-dd HH:mm:ss`, ISO 8601)? | Required to correctly parse the datetime column; incorrect parsing silently corrupts data. |
| 3 | Is the temperature value a decimal or integer, and what is its unit? | Affects database column type (`DECIMAL` vs `INTEGER`) and any validation logic. |
| 4 | How is the CSV file path provided to the job (CLI argument, config property, directory scan)? | Determines job parameter design and whether the job supports multiple files. |
| 5 | Should duplicate detection consider only existing database records, or also duplicates within the same CSV file? | Impacts whether deduplication happens in the writer (DB constraint) or also in the processor. |
| 6 | What should happen when a row has invalid/missing data (skip with warning, fail the job)? | Defines skip policy and error-handling strategy in the Spring Batch step configuration. |
| 7 | Does the CSV file have a header row? | Determines `FlatFileItemReader` configuration (`linesToSkip` setting). |
| 8 | Should the summary be logged or printed to stdout? | Affects implementation — `JobExecutionListener` logging vs. console output. |
| 9 | Is the job expected to be re-runnable (idempotent) for the same file? | Influences Spring Batch job parameter design and `allowStartIfComplete` settings. |
| 10 | What is the expected scale — typical file size and number of records? | Determines chunk size, commit interval, and whether partitioning/parallel processing is needed. |
