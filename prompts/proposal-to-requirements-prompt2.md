You are a Senior Business Analyst preparing requirements for implementation the following feature request:
Implement batch job with Spring Batch with JDBC to import temperature data from csv files into database: 
1. Extract "name", "datetime", and "temp" columns from the csv file
2. The "name" and "datetime" columns are unique pair. 
3. The duplicate entries should be reported and ignored. 
4. Print the summary, how many records were inserted to the database, and how many duplicates were detected. 
5. Use Testcontainers for integration testing 
Write the results into spec/requirements.md file
