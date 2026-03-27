## Triggered by: "import temperature data from csv files"

📎 Clarify scope: Should the job process one file per run, or multiple files in a single execution? If one file — how is the path provided: CLI argument, environment variable, or a config property?


## Triggered by: "datetime" column mention

📎 Clarify format: What datetime format does the CSV use? For example: 2024-01-15 13:45:00 (yyyy-MM-dd HH:mm:ss), 2024-01-15T13:45:00, or something else?


## Triggered by: "temp" column mention

📎 Clarify data type: Should temperature be stored as an integer, or decimal? If decimal — how many decimal places do you need? (e.g., 23.75 → DECIMAL(6,2))


## Triggered by: "duplicate entries should be reported and ignored"

📎 Clarify "reported": Where should duplicates be reported?

* Logged at WARN level (name + datetime in the log line)
* Written to a separate reject file
* Both

Also — should duplicate detection happen only against the database, or also within the CSV itself (e.g., two identical rows in the same file)?


## Triggered by: "print the summary"

📎 Clarify summary destination: Should the summary go to the application log, standard output (stdout), or both? What format do you prefer? e.g., Inserted: 42, Duplicates skipped: 3


## Triggered by: the overall CSV description (no format details given)

📎 Clarify CSV format: A few quick assumptions to confirm:

* Delimiter: comma?
* Encoding: UTF-8?
* Is there a header row (name,datetime,temp) that should be skipped?
* Are fields ever quoted?


## Triggered by: absence of any error handling mention

📎 Clarify invalid row behavior: What should happen if a row has a missing field, an unparseable datetime, or a non-numeric temperature?

Skip the row and log a warning (continue processing)
Fail the entire job immediately

If skipping — is there a maximum number of bad rows before the job should abort?


## Triggered by: "Testcontainers for integration testing" with no mention of re-runnability

📎 Clarify re-run behavior: If the same file is run twice, should the second run be treated as an error, or should it complete successfully with all rows reported as duplicates (idempotent)?