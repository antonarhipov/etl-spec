Specification Review: Temperature Data CSV Import Batch Job
                                                  
---                                                                                                                                               
BLOCKERS

[BLOCKER-1] Skip limit scope is contradictory between requirements and ACs

Requirements (§2) says the skip limit applies to rows with "missing fields, unparseable datetime, or non-numeric temp." The implementation        
checklist says the skip policy catches both DuplicateKeyException and validation exceptions in the same policy. AC-4.1 uses a separate "duplicate
counter," but AC-3.1–3.5 each increment "skip count toward the skip limit." It is never stated whether duplicates also count toward the skip      
limit.

Impact: A re-run of a large previously-imported file could breach the skip limit and fail. Implementation will make an arbitrary choice.

Resolution: Explicitly state that DuplicateKeyException rows do not count toward the skip limit. The skip limit applies only to validation/parse  
errors. Use two separate counters.
                                                                                                                                                    
---             
[BLOCKER-2] AC-6.4 exception type not covered by the defined skip policy

AC-6.4 requires that a temperature value outside DECIMAL(6,2) range causes the row to be skipped and the skip count incremented. However, this is
a DB write failure — MySQL throws a DataTruncationException (a subtype of SQLException), not a DuplicateKeyException or processor-level           
SkippableException. The requirements only define skip handling for those two exception types.

Impact: Without explicit configuration, this exception propagates as a fatal job failure, not a skip.

Resolution: Add DataTruncationException (or DataAccessException broadly) to the skip policy configuration, or document that AC-6.4 is             
intentionally a fatal error (and update the AC accordingly).
                                                                                                                                                    
---             
MAJOR

[MAJOR-1] Summary format omits invalid/skipped row count

AC-5.3 defines the summary as Inserted: K, Duplicates skipped: D. There is no field for rows skipped due to validation errors (null fields, bad   
datetime, non-numeric temp). A file with valid=5, duplicates=3, invalid=2 rows would report Inserted: 5, Duplicates skipped: 3 — the 2 invalid    
rows are invisible in the output.

Resolution: Extend the summary format to Inserted: K, Duplicates skipped: D, Invalid skipped: E, or explicitly document that invalid rows are     
intentionally not reported in the summary (and reconcile with requirement §2 which says invalid rows are "counted in the final summary").
                                                                                                                                                    
---             
[MAJOR-2] AC-3.7 chunk-boundary commit semantics are undefined

AC-3.7 states "no further records be committed after the threshold is crossed." Spring Batch's behavior when the skip limit is exceeded mid-chunk
is that the current chunk is rolled back. This means records within the in-flight chunk are not committed, but all prior chunks are committed. The
AC does not state whether the partial last chunk is committed or rolled back.

Resolution: Add explicit language: "The chunk in progress when the skip limit is exceeded SHALL be rolled back. All prior committed chunks remain
in the database."
                                                                                                                                                    
---             
[MAJOR-3] AC-1.2 validation mechanism is underspecified

AC-1.2 says "application performs startup validation" and "job fails immediately." No mechanism is specified: is this a Spring Batch
JobParametersValidator, a CommandLineRunner pre-check, or something else? There is no implementation checklist entry for this component.

Resolution: Add to the implementation checklist: "JobParametersValidator implementation that checks filePath presence and file existence,         
registered on the Job bean."
                                                                                                                                                    
---             
[MAJOR-4] Integration test coverage leaves 22 of 28 ACs untested

Category 9 defines only 4 integration tests (AC-9.1 to AC-9.4). The following AC categories have zero corresponding test specification: startup
validation (AC-1.2, 1.3, 1.4), CSV parsing edge cases (AC-2.3, 2.4), all skip-behavior ACs (AC-3.1–3.7), skip limit breach (AC-3.7), empty file   
(AC-6.1), single-row (AC-6.2), temperature boundaries (AC-6.3, 6.4), idempotency re-run instance creation (AC-7.2), datetime/decimal precision
(AC-8.2, 8.3).

Resolution: Expand Category 9 to include at minimum: a skip-limit-exceeded test, a validation-error-skip test, an empty-file test, and a          
precision-verification test. Unit tests for the ItemProcessor should be added for AC-3.1–3.5.
                                                                                                                                                    
---             
[MAJOR-5] Summary on FAILED job is underspecified

AC-5.1 says summary is logged "regardless of outcome." AC-3.7 says the job terminates with FAILED status when the skip limit is exceeded. AC-5.3
only defines the summary for a successful run (K valid rows, D duplicates). There is no specification for what the summary reports when the job   
fails mid-run (e.g., only 50 of 1000 rows were processed before failure).

Resolution: Add an AC or note: "When the job exits FAILED, the summary SHALL reflect counts accumulated up to the point of failure."
   
---                                                                                                                                               
MINOR

[MINOR-1] DECIMAL(6,2) range description is misleading

Requirements §1 states the column "supports values like -273.15 to 999.99." DECIMAL(6,2) actually supports up to ±9999.99. AC-6.3 correctly uses  
9999.99 as a boundary value, creating a discrepancy with the requirements table.

Resolution: Correct the requirements description to ±9999.99.
   
---                                                                                                                                               
[MINOR-2] Spring Boot version 4.0.5 does not exist

Requirements §3 states "Spring Boot 4.0.5." The latest Spring Boot major version is 3.x. This is likely a typo for 3.4.5 or 3.5.x.

Resolution: Verify the actual version from pom.xml and correct the requirements document.
                                                                                                                                                    
---                                                                                                                                               
[MINOR-3] Whitespace-only field values are undefined

A CSV row like  ,2024-01-01 00:00:00,25.0 (name = spaces only) is not addressed. The AC for null name (AC-3.1) says "empty or missing" but
whitespace-only may pass a null check while being semantically invalid.

Resolution: Clarify whether whitespace-only strings are trimmed and treated as empty (and thus skipped), or stored as-is.
                  
---                                                                                                                                               
[MINOR-4] Behavior when CSV contains quoted fields is undefined

Requirements state "No quoting required" but do not specify what happens if the file does contain RFC-4180 quoted fields (e.g., "Station
A",2024-01-01 00:00:00,23.5). A positional FlatFileItemReader without a quote character configured may parse the quotes as part of the field      
value.

Resolution: Add: "Files containing RFC-4180 quoted fields are not supported. If a field contains a quote character, behavior is undefined (the    
name value may include the quote literal)."
                                                                                                                                                    
---             
[MINOR-5] No test data artifacts specified

No sample CSV files are defined in the spec. AC-9.2 and AC-9.3 refer to "a CSV file with N valid rows" without specifying fixture content, field
ranges, or how test data is seeded into the database for duplicate tests.

Resolution: Define at minimum one canonical test fixture CSV with known row counts, or specify that tests generate data programmatically with     
documented schemas.
                                                                                                                                                    
---             
Summary Table

┌───────────┬──────────┬──────────────────┬───────────────────────────────────────────────────────────────┐
│    ID     │ Severity │     Category     │                          Description                          │                                       
├───────────┼──────────┼──────────────────┼───────────────────────────────────────────────────────────────┤
│ BLOCKER-1 │ BLOCKER  │ Consistency      │ Skip limit scope — duplicates vs. validation errors ambiguous │                                       
├───────────┼──────────┼──────────────────┼───────────────────────────────────────────────────────────────┤
│ BLOCKER-2 │ BLOCKER  │ Implementability │ DataTruncationException not in skip policy for AC-6.4         │                                       
├───────────┼──────────┼──────────────────┼───────────────────────────────────────────────────────────────┤                                       
│ MAJOR-1   │ MAJOR    │ Completeness     │ Summary format omits invalid/skipped row count                │                                       
├───────────┼──────────┼──────────────────┼───────────────────────────────────────────────────────────────┤                                       
│ MAJOR-2   │ MAJOR    │ Completeness     │ AC-3.7 chunk-boundary rollback behavior unspecified           │
├───────────┼──────────┼──────────────────┼───────────────────────────────────────────────────────────────┤                                       
│ MAJOR-3   │ MAJOR    │ Implementability │ AC-1.2 validation mechanism has no implementation entry       │
├───────────┼──────────┼──────────────────┼───────────────────────────────────────────────────────────────┤                                       
│ MAJOR-4   │ MAJOR    │ Testability      │ 22 of 28 ACs have no corresponding test case                  │
├───────────┼──────────┼──────────────────┼───────────────────────────────────────────────────────────────┤                                       
│ MAJOR-5   │ MAJOR    │ Completeness     │ Summary output on FAILED job is undefined                     │
├───────────┼──────────┼──────────────────┼───────────────────────────────────────────────────────────────┤                                       
│ MINOR-1   │ MINOR    │ Consistency      │ DECIMAL(6,2) example range conflicts with AC-6.3 boundary     │
├───────────┼──────────┼──────────────────┼───────────────────────────────────────────────────────────────┤                                       
│ MINOR-2   │ MINOR    │ Consistency      │ Spring Boot version 4.0.5 does not exist                      │
├───────────┼──────────┼──────────────────┼───────────────────────────────────────────────────────────────┤                                       
│ MINOR-3   │ MINOR    │ Completeness     │ Whitespace-only field handling undefined                      │
├───────────┼──────────┼──────────────────┼───────────────────────────────────────────────────────────────┤                                       
│ MINOR-4   │ MINOR    │ Completeness     │ Quoted CSV field behavior undefined                           │
├───────────┼──────────┼──────────────────┼───────────────────────────────────────────────────────────────┤                                       
│ MINOR-5   │ MINOR    │ Testability      │ No test fixture data or seeding strategy specified            │
└───────────┴──────────┴──────────────────┴───────────────────────────────────────────────────────────────┘

2 blockers must be resolved before implementation begins. MAJOR-1 and MAJOR-3 require spec amendments. MAJOR-4 (test coverage gaps) can be        
addressed in a test plan alongside implementation.