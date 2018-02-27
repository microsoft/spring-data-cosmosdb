### How to Query Partitioned CosmosDB Collection

With CosmosDB, you can configure [partition key](https://docs.microsoft.com/en-us/azure/cosmos-db/partition-data) for your collection.

Below is an example about how to query partitioned collection with this spring data module.

#### Example 

Given a document entity structure:
```
    @Document
    @Data
    @AllArgsConstructor
    public class Address {
        @Id
        String postalCode;
        String street;
        @PartitionKey
        String city;
    }
```

How to write the repository interface:
```
    @Repository
    public interface AddressRepository extends DocumentDbRepository<Address, String> {
        void deleteByPostalCodeAndCity(String postalCode, String city);
        void deleteByCity(String city);   
        List<Address> findByPostalCodeAndCity(String postalCode, String city);   
        List<Address> findByCity(String city);
        
        /*
         * Partition key value must be provided when querying with ID.
         * UnsupportedOperationException will throw if partition key value is not provided, 
         * which is different from querying collection without partition key.
         */
        // void deleteById(String postalCode); // Incorrect
        // Address findById(String postalCode); // Incorrect
        // Address findOne(String postalCode); // Incorrect
    }
```

