### Requirements Analysis: Temperature Data CSV Import Batch Job

---

### 1. AMBIGUITIES — RESOLVED

| Ambiguity | Resolution |
|-----------|-----------|
| **"CSV files" (plural):** Single file or multiple per run? | **Single file per run.** The file path is passed as a Spring Batch job parameter (`filePath`). Processing multiple files requires launching separate job instances. |
| **"Duplicate entries should be reported and ignored":** What does "reported" mean? | **Logged via SLF4J** at WARN level (name + datetime of each duplicate) and counted in the final summary. No separate reject file. |
| **"Print the summary":** Where? | **Logged via a `JobExecutionListener#afterJob`** at INFO level and also printed to stdout. Format: `Inserted: N, Duplicates skipped: M`. |
| **"temp" column:** Data type and unit? | **`DECIMAL(6,2)`** — supports values like `-273.15` to `999.99`. Unit is not enforced by the application (stored as-is). |
| **"datetime" column:** Format? | **`yyyy-MM-dd HH:mm:ss`** (no timezone). Stored as MySQL `DATETIME`. |

---

### 2. MISSING INFORMATION — RESOLVED

| Gap | Decision |
|-----|----------|
| **CSV file path** | Passed as a job parameter: `--filePath=path/to/data.csv`. Validated at startup; job fails fast if the file does not exist. |
| **CSV format** | Standard comma delimiter, UTF-8 encoding, **header row present** (first line skipped via `linesToSkip=1`). Columns in order: `name,datetime,temp`. No quoting required. |
| **Database schema** | Table: `temperature_reading`. Columns: `id BIGINT AUTO_INCREMENT PRIMARY KEY`, `name VARCHAR(255) NOT NULL`, `datetime DATETIME NOT NULL`, `temp DECIMAL(6,2) NOT NULL`. Unique constraint: `UNIQUE (name, datetime)`. Schema managed by **Flyway**. |
| **Database type** | **MySQL** — confirmed by `pom.xml` (`mysql-connector-j`, `flyway-mysql`, `testcontainers-mysql`) and `compose.yaml`. |
| **"name" semantics** | Sensor or weather station name. No domain validation applied. |
| **Error handling beyond duplicates** | Rows with missing fields, unparseable datetime, or non-numeric temp are **skipped with a WARN log**. A configurable skip limit (default: 10) causes the job to fail if exceeded. |
| **Chunk size / commit interval** | **100 records per chunk** (configurable via `batch.chunk-size` property). |
| **Job scheduling** | **One-time run on startup** (`spring.batch.job.enabled=true`). No cron scheduling. |

---

### 3. IMPLICIT ASSUMPTIONS — CONFIRMED

- **Header row is present** in the CSV. `FlatFileItemReader` is configured with `linesToSkip=1`.
- **Duplicate detection is against the database** (unique constraint on `name + datetime`). The writer catches `DuplicateKeyException` and counts it; no pre-check query is performed.
- **Unique constraint is enforced at the DB level** via a DDL `UNIQUE KEY` in the Flyway migration, not only in application logic.
- **Spring Boot 4.0.5** is the framework (confirmed by `pom.xml`). Spring Batch 6.x is included transitively.
- **MySQL** is the target database (confirmed).
- **Job runs as a command-line Spring Boot application**, triggered on startup with job parameters passed as CLI args.

---

### 4. EDGE CASES — HANDLING DEFINED

| Edge Case | Handling |
|-----------|----------|
| Empty CSV file (no rows, header only) | Job completes successfully. Summary: `Inserted: 0, Duplicates skipped: 0`. |
| Duplicate rows **within the same CSV file** | Each duplicate against the DB triggers a `DuplicateKeyException` at write time; all are counted and skipped. |
| Null or empty `name`, `datetime`, or `temp` | Row is skipped (WARN logged). Counts toward skip limit. |
| Malformed rows (wrong column count, bad datetime, non-numeric temp) | Row is skipped (WARN logged). Counts toward skip limit. |
| Extra columns in CSV | Ignored — `FlatFileItemReader` maps only the first three columns by position. |
| Re-running the same file | All previously imported records are reported as duplicates and skipped. Job completes successfully (idempotent). Spring Batch job instance uniqueness is based on `filePath` + run timestamp to allow re-runs. |
| Job failure mid-run | Spring Batch restart is **not enabled** (`allowStartIfComplete=false` is default). Re-run the job with the same file; already-inserted records will be caught as duplicates. |
| Concurrent executions with overlapping data | DB unique constraint prevents double-insertion. One of the concurrent writers will receive `DuplicateKeyException` and skip. |
| Extremely large files | Chunk size of 100 keeps memory bounded. No partitioning needed for the stated scope. |
| Temperature boundary values | No application-level range validation. DB column type (`DECIMAL(6,2)`) will reject values outside `±9999.99`. |

---

### 5. CLARIFYING QUESTIONS — ANSWERED

| # | Question | Answer |
|---|----------|--------|
| 1 | What database engine? | **MySQL** (from `pom.xml` and `compose.yaml`) |
| 2 | Datetime format in CSV? | **`yyyy-MM-dd HH:mm:ss`** |
| 3 | Temperature type and unit? | **`DECIMAL(6,2)`**, unit not enforced |
| 4 | How is CSV path provided? | **CLI job parameter:** `--filePath=...` |
| 5 | Duplicate detection scope? | **DB-level only** — `DuplicateKeyException` on write; no in-memory deduplication |
| 6 | Invalid row handling? | **Skip with WARN log**, configurable skip limit (default 10) |
| 7 | Header row in CSV? | **Yes** — `linesToSkip=1` |
| 8 | Summary output target? | **SLF4J INFO log + stdout** via `JobExecutionListener#afterJob` |
| 9 | Is the job re-runnable (idempotent)? | **Yes** — re-runs produce duplicate-skip counts for already-imported rows; job parameter includes a timestamp to allow new job instances for the same file |
| 10 | Expected file scale? | Not specified; chunk size of 100 is safe for files up to millions of rows |

---

### 6. IMPLEMENTATION CHECKLIST

- [ ] Flyway migration `V1__create_temperature_reading.sql` — creates `temperature_reading` table with unique constraint
- [ ] `TemperatureRecord` — item POJO with `name`, `datetime` (`LocalDateTime`), `temp` (`BigDecimal`)
- [ ] `FlatFileItemReader<TemperatureRecord>` — comma-delimited, `linesToSkip=1`, positional field mapping
- [ ] `ItemProcessor` — validates non-null fields; throws `SkippableException` on invalid rows
- [ ] `JdbcBatchItemWriter<TemperatureRecord>` — `INSERT INTO temperature_reading (name, datetime, temp) VALUES (:name, :datetime, :temp)`
- [ ] Skip policy — catches `DuplicateKeyException` and validation exceptions; increments duplicate/invalid counters
- [ ] `JobExecutionListener` — prints summary after job completes
- [ ] `application.properties` — datasource config, `spring.batch.job.enabled=true`, `batch.chunk-size=100`
- [ ] Integration test with Testcontainers MySQL — covers normal insert, duplicate skip, and summary output
