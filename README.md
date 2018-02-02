[![Travis CI](https://travis-ci.org/Microsoft/spring-data-documentdb.svg?branch=master)](https://travis-ci.org/Microsoft/spring-data-documentdb)
[![AppVeyor](https://ci.appveyor.com/api/projects/status/ms9i54axmxa7jg9d/branch/master?svg=true)](https://ci.appveyor.com/project/yungez/spring-data-documentdb)
[![codecov](https://codecov.io/gh/Microsoft/spring-data-documentdb/branch/master/graph/badge.svg)](https://codecov.io/gh/Microsoft/spring-data-documentdb)
[![Maven Central](https://img.shields.io/maven-central/v/com.microsoft.azure/spring-data-documentdb.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.microsoft.azure%22%20AND%20a%3A%22spring-data-documentdb%22)
[![MIT License](http://img.shields.io/badge/license-MIT-green.svg) ](https://github.com/Microsoft/spring-data-documentdb/blob/master/LICENSE)


# Spring Data for Azure DocumentDB 

[Azure DocumentDB](https://docs.microsoft.com/en-us/azure/cosmos-db/documentdb-introduction) helps manage JSON data through well-defined database resources. It is a key part of [Azure Cosmos DB](https://docs.microsoft.com/en-us/azure/cosmos-db/documentdb-introduction), which is a globally-distributed database service that allows developers to work with data using a variety of standard APIs, such as DocumentDB, MongoDB, Graph, and Table. 

**Spring Data DocumentDB** provides initial Spring Data support for [Azure DocumentDB API](https://docs.microsoft.com/en-us/azure/cosmos-db/documentdb-introduction) based on Spring Data framework, the other APIs are not supported in this package. 

## TOC

* [Sample Code](#sample-codes)
* [Feature List](#feature-list)
* [Quick Start](#quick-start)
* [Filing Issues](#filing-issues)
* [How to Contribute](#how-to-contribute)
* [Code of Conduct](#code-of-conduct)

## Sample Code
Please refer to [sample project here](./samplecode).

## Feature List
- Spring Data CRUDRepository basic CRUD functionality
    - save
    - findAll
    - findOne by Id
    - deleteAll
    - delete by Id
    - delete entity
- Spring Data [@Id](https://github.com/spring-projects/spring-data-commons/blob/db62390de90c93a78743c97cc2cc9ccd964994a5/src/main/java/org/springframework/data/annotation/Id.java) annotation.
  There're 2 ways to map a field in domain class to `id` field of Azure Cosmos DB document.
  - annotate a field in domain class with `@Id`, this field will be mapped to document `id` in Cosmos DB. 
  - set name of this field to `id`, this field will be mapped to document `id` in Cosmos DB.
- Custom collection Name.
  By default, collection name will be class name of user domain class. To customize it, add annotation `@Document(collection="myCustomCollectionName")` to domain class, that's all.
- Supports [Azure Cosmos DB partition](https://docs.microsoft.com/en-us/azure/cosmos-db/partition-data). To specify a field of domain class to be partition key field, just annotate it with `@PartitionKey`. When you do CRUD operation, pls specify your partition value. For more sample on partition CRUD, pls refer to [test here](./src/test/java/com/microsoft/azure/spring/data/cosmosdb/documentdb/repository/AddressRepositoryIT.java)
- Supports [Spring Data custom query](https://docs.spring.io/spring-data/commons/docs/current/reference/html/#repositories.query-methods.details) find operation.
- Supports [spring-boot-starter-data-rest](https://projects.spring.io/spring-data-rest/).
- Supports List and nested type in domain class.
  
## Quick Start

### Add the dependency
`spring-data-documentdb` is published on Maven Central Repository.  
If you are using Maven, add the following dependency.  

```xml
<dependency>
    <groupId>com.microsoft.azure</groupId>
    <artifactId>spring-data-documentdb</artifactId>
    <version>0.1.3</version>
</dependency>
```

### Setup Configuration
Setup configuration class.

```
@Configuration
@EnableDocumentDbRepositories
public class AppConfiguration extends AbstractDocumentDbConfiguration {

    @Value("${azure.documentdb.uri}")
    private String uri;

    @Value("${azure.documentdb.key}")
    private String key;

    @Value("${azure.documentdb.database}")
    private String dbName;

    public DocumentClient documentClient() {
        return new DocumentClient(uri, key, ConnectionPolicy.GetDefault(), ConsistencyLevel.Session);
    }

    public String getDatabase() {
        return dbName;
    }
}
```
By default, `@EnableDocumentDbRepositories` will scan the current package for any interfaces that extend one of Spring Data's repository interfaces. Using it to annotate your Configuration class to scan a different root package by type if your project layout has multiple projects and it's not finding your repositories.
```
@Configuration
@EnableDocumentDbRepositories(basePackageClass=UserRepository.class)
public class AppConfiguration extends AbstractDocumentDbConfiguration {
    // configuration code
}
```


### Define en entity
Define a simple entity as Document in DocumentDB.

```
@Document(collection = "mycollection")
public class User {
    private String id;
    private String firstName;
    @PartitionKey
    private String lastName;
 
    ... // setters and getters

    public User(String id, String firstName, String lastName) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    public String toString() {
        return String.format("User: %s %s, %s", firstName, lastName);
    }
}
```
`id` field will be used as document id in Azure Cosmos DB. If you want use another field like `emailAddress` as document `id`, just annotate that field with `@Id` annotation.

Annotation `@Document(collection="mycollection")` is used to specify collection name in Azure Cosmos DB.
Annotation `@PartitionKey` on `lastName` field is used to specify this field be partition key in Azure Cosmos DB.

```
@Document(collection = "mycollection")
public class User {
    @Id
    private String emailAddress;

    ...
}
```

### Create repositories
Extends DocumentDbRepository interface, which provides Spring Data repository support.

```
import DocumentDbRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends DocumentDbRepository<User, String> {
    List<User> findByFirstName(String firstName); 
}
```

`findByFirstName` method is custom query method, it will find documents per FirstName.

### Create an Application class
Here create an application class with all the components

```
@SpringBootApplication
public class SampleApplication implements CommandLineRunner {

    @Autowired
    private UserRepository repository;

    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }

    public void run(String... var1) throws Exception {

        final User testUser = new User("testId", "testFirstName", "testLastName");

        repository.deleteAll();
        repository.save(testUser);

        // to find by Id, please specify partition key value if collection is partitioned
        final User result = repository.findOne(testUser.getId(), testUser.getLastName);
        // if emailAddress is mapped to id, then 
        // final User result = respository.findOne(testUser.getEmailAddress(), testUser.getLastName());
    }
}
```
Autowired UserRepository interface, then can do save, delete and find operations. Azure Cosmos DB DocumentDB Spring Data uses the DocumentTemplate to execute the queries behind *find*, *save* methods. You can use the template yourself for more complex queries.

## Filing Issues

If you encounter any bug, please file an issue [here](https://github.com/Microsoft/spring-data-documentdb/issues/new).

To suggest a new feature or changes that could be made, file an issue the same way you would for a bug.

## How To Contribute

Contribution is welcome. Please follow [this instruction](./HowToContribute.md) to contribute code.

## Code of Conduct

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.
