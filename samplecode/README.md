## Overview
This sample illustrates the process to use annotation `@Document` and `@Id` to interact with Azure Cosmos DB Collection, extend `DocumentDbRepository` to customize a query operation with specific fields, and apply `spring-data-rest` to expose a discoverable REST API for clients.

### Get started
To get started, first create a new database instance by using the [Azure portal](https://portal.azure.com/). You can find Azure Cosmos DB in Databases and choose SQL (Document DB) for the API. When your database has been created, you can find the URI and keys on the overview page. The values will be used to configure your Spring Boot application. [More details](https://docs.microsoft.com/en-us/java/azure/spring-framework/configure-spring-boot-starter-java-app-with-cosmos-db). 


#### Application.properties
Open the application.properties file under `main` and `test` folder, and add the following lines to the file, and replace the sample values with the appropriate properties for your database.

```
    # Specify the DNS URI of your Azure Cosmos DB.
    azure.documentdb.uri=your-documentDb-uri

    # Specify the access key for your database.
    azure.documentdb.key=your-documentDb-key

    # Specify the name of your database.
    azure.documentdb.database=your-documentDb-dbName

```

### Give it a run

   - Use Maven 

     ```
     mvn clean install
     cd example
     mvn spring-boot:run
     ```

 If running locally, browse to `http://localhost:8080` and check the app. 
