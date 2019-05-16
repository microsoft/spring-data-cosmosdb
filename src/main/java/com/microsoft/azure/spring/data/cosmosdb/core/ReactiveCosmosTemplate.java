/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.core;

import com.microsoft.azure.cosmos.CosmosClient;
import com.microsoft.azure.cosmos.CosmosContainerResponse;
import com.microsoft.azure.cosmos.CosmosItem;
import com.microsoft.azure.cosmos.CosmosItemRequestOptions;
import com.microsoft.azure.cosmosdb.FeedOptions;
import com.microsoft.azure.cosmosdb.FeedResponse;
import com.microsoft.azure.cosmosdb.PartitionKey;
import com.microsoft.azure.cosmosdb.SqlQuerySpec;
import com.microsoft.azure.spring.data.cosmosdb.CosmosDbFactory;
import com.microsoft.azure.spring.data.cosmosdb.core.convert.MappingDocumentDbConverter;
import com.microsoft.azure.spring.data.cosmosdb.core.generator.CountQueryGenerator;
import com.microsoft.azure.spring.data.cosmosdb.core.generator.FindQuerySpecGenerator;
import com.microsoft.azure.spring.data.cosmosdb.core.query.Criteria;
import com.microsoft.azure.spring.data.cosmosdb.core.query.CriteriaType;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import com.microsoft.azure.spring.data.cosmosdb.exception.DocumentDBAccessException;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.DocumentDbEntityInformation;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ReactiveCosmosTemplate implements ReactiveCosmosOperations, ApplicationContextAware {
    private static final String COUNT_VALUE_KEY = "_aggregate";

    private final MappingDocumentDbConverter mappingDocumentDbConverter;
    private final String databaseName;

    @Getter(AccessLevel.PRIVATE)
    private final CosmosClient cosmosClient;
    private final CosmosDbFactory cosmosDbFactory;

    private List<String> collectionCache;

    /**
     * Constructor
     *
     * @param cosmosDbFactory            the cosmosdbfactory
     * @param mappingDocumentDbConverter the mappingDocumentDbConverter
     * @param dbName                     database name
     */
    public ReactiveCosmosTemplate(CosmosDbFactory cosmosDbFactory,
                                  MappingDocumentDbConverter mappingDocumentDbConverter,
                                  String dbName) {
        Assert.notNull(cosmosDbFactory, "CosmosDbFactory must not be null!");
        Assert.notNull(mappingDocumentDbConverter, "MappingDocumentDbConverter must not be null!");

        this.databaseName = dbName;
        this.mappingDocumentDbConverter = mappingDocumentDbConverter;
        this.collectionCache = new ArrayList<>();

        this.cosmosDbFactory = cosmosDbFactory;
        this.cosmosClient = this.cosmosDbFactory.getCosmosClient();
    }

    /**
     * @param applicationContext the application context
     * @throws BeansException the bean exception
     */
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    }

    /**
     * Creates a collection if it doesnt already exist
     *
     * @param information the DocumentDbEntityInformation
     * @return Mono containing CosmosContainerResponse
     */
    @Override
    public Mono<CosmosContainerResponse> createCollectionIfNotExists(DocumentDbEntityInformation information) {

        return cosmosClient.createDatabaseIfNotExists(this.databaseName)
                .flatMap(cosmosDatabaseResponse -> cosmosDatabaseResponse.getDatabase()
                        .createContainerIfNotExists(information.getCollectionName(),
                                "/" +
                                        information.getPartitionKeyFieldName()));

    }

    /**
     * Find all items in a given container
     *
     * @param containerName the containerName
     * @param entityClass   the entityClass
     * @return Flux with all the found items or error
     */
    @Override
    public <T> Flux<T> findAll(String containerName, Class<T> entityClass) {
        final DocumentQuery query = new DocumentQuery(Criteria.getInstance(CriteriaType.ALL));

        return find(query, entityClass, containerName);
    }

    /**
     * Find all items in a given container
     *
     * @param entityClass the entityClass
     * @return Flux with all the found items or error
     */
    @Override
    public <T> Flux<T> findAll(Class<T> entityClass) {
        final DocumentQuery query = new DocumentQuery(Criteria.getInstance(CriteriaType.ALL));

        return find(query, entityClass, entityClass.getSimpleName());
    }

    /**
     * Find by id
     *
     * @param id          the id
     * @param entityClass the entityclass
     * @return Mono with the item or error
     */
    @Override
    public <T> Mono<T> findById(Object id, Class<T> entityClass) {
        Assert.notNull(entityClass, "entityClass should not be null");
        return findById(getcontainerName(entityClass), id, entityClass);
    }

    /**
     * Find by id
     *
     * @param containerName the containername
     * @param id            the id
     * @param entityClass   the entity class
     * @return Mono with the item or error
     */
    @Override
    public <T> Mono<T> findById(String containerName, Object id, Class<T> entityClass) {
        Assert.hasText(containerName, "collectionName should not be null, empty or only whitespaces");
        Assert.notNull(entityClass, "entityClass should not be null");
        assertValidId(id);

        //TODO: Refactor this after np->p is completed upstream
        final String query = String.format("select * from root where root.id = '%s'", id.toString());
        final FeedOptions options = new FeedOptions();
        options.setEnableCrossPartitionQuery(true);
        return getCosmosClient().getDatabase(databaseName)
                .getContainer(containerName)
                .queryItems(query, options)
                .flatMap(cosmosItemFeedResponse -> Mono.just(cosmosItemFeedResponse
                        .getResults()
                        .stream()
                        .map(cosmosItem -> cosmosItem.toObject(entityClass))
                        .collect(Collectors.toList()).get(0)))
                .next();
    }

    /**
     * Insert
     *
     * @param objectToSave the object to save
     * @param partitionKey the partitoin key
     * @return Mono with the item or error
     */
    public <T> Mono<T> insert(T objectToSave, PartitionKey partitionKey) {
        Assert.notNull(objectToSave, "entityClass should not be null");

        return insert(getcontainerName(objectToSave.getClass()), objectToSave, partitionKey);
    }

    /**
     * Insert
     *
     * @param objectToSave the object to save
     * @return Mono with the item or error
     */
    public <T> Mono<T> insert(T objectToSave) {
        Assert.notNull(objectToSave, "objectToSave should not be null");

        final Class<T> domainClass = (Class<T>) objectToSave.getClass();
        return getCosmosClient().getDatabase(this.databaseName)
                .getContainer(getcontainerName(objectToSave.getClass()))
                .createItem(objectToSave, new CosmosItemRequestOptions())
                .doOnError(throwable -> Mono.error(throwable))
                .flatMap(cosmosItemResponse -> Mono.just(cosmosItemResponse.getItem().toObject(domainClass)));
    }

    /**
     * Insert
     *
     * @param containerName the container name
     * @param objectToSave  the object to save
     * @param partitionKey  the partition key
     * @return Mono with the item or error
     */
    public <T> Mono<T> insert(String containerName, Object objectToSave, PartitionKey partitionKey) {
        Assert.hasText(containerName, "containerName should not be null, empty or only whitespaces");
        Assert.notNull(objectToSave, "objectToSave should not be null");

        final CosmosItemRequestOptions options = new CosmosItemRequestOptions();
        if (partitionKey != null) {
            options.setPartitionKey(partitionKey);
        }

        final Class<T> domainClass = (Class<T>) objectToSave.getClass();
        return getCosmosClient().getDatabase(this.databaseName)
                .getContainer(containerName)
                .createItem(objectToSave, options)
                .onErrorResume(Mono::error)
                .flatMap(cosmosItemResponse -> Mono.just(cosmosItemResponse.getItem().toObject(domainClass)));
    }

    /**
     * Upsert
     *
     * @param object       the object to upsert
     * @param partitionKey the partition key
     * @return Mono with the item or error
     */
    @Override
    public <T> Mono<T> upsert(T object, PartitionKey partitionKey) {
        return upsert(getcontainerName(object.getClass()), object, partitionKey);
    }

    /**
     * Upsert
     *
     * @param containerName the container name
     * @param object        the object to save
     * @param partitionKey  the partition key
     * @return Mono with the item or error
     */
    @Override
    public <T> Mono<T> upsert(String containerName, T object, PartitionKey partitionKey) {
        final Class<T> domainClass = (Class<T>) object.getClass();
        final CosmosItemRequestOptions options = new CosmosItemRequestOptions();
        if (partitionKey != null) {
            options.setPartitionKey(partitionKey);
        }

        return getCosmosClient().getDatabase(this.databaseName)
                .getContainer(containerName)
                .upsertItem(object, options)
                .flatMap(cosmosItemResponse -> Mono.just(cosmosItemResponse.getItem().toObject(domainClass)));
    }

    /**
     * Delete an item by id
     *
     * @param containerName the container name
     * @param id            the id
     * @param partitionKey  the partition key
     * @return void Mono
     */
    @Override
    public Mono<Void> deleteById(String containerName, Object id, PartitionKey partitionKey) {
        Assert.hasText(containerName, "container name should not be null, empty or only whitespaces");
        assertValidId(id);
        Assert.notNull(partitionKey, "partitionKey should not be null");

        final JSONObject jo = new JSONObject();
        jo.put("id", id.toString());
        final CosmosItemRequestOptions options = new CosmosItemRequestOptions();
        if (partitionKey != null) {
            options.setPartitionKey(partitionKey);
        }
        return getCosmosClient().getDatabase(this.databaseName)
                .getContainer(containerName)
                .getItem(jo.toString())
                .delete(options)
                .onErrorResume(throwable -> Mono.empty())
                .then();
    }

    /**
     * Delete all items in a container
     *
     * @param containerName    the container name
     * @param partitionKeyName the partition key path
     * @return void Mono
     */
    @Override
    public Mono<Void> deleteAll(String containerName, String partitionKeyName) {
        Assert.hasText(containerName, "container name should not be null, empty or only whitespaces");
        Assert.notNull(partitionKeyName, "partitionKeyName should not be null");

        final Criteria criteria = Criteria.getInstance(CriteriaType.ALL);
        final DocumentQuery query = new DocumentQuery(criteria);
        final SqlQuerySpec sqlQuerySpec = new FindQuerySpecGenerator().generateCosmos(query);
        final FeedOptions options = new FeedOptions();
        final boolean isCrossPartitionQuery = query.isCrossPartitionQuery(Arrays.asList(partitionKeyName));
        options.setEnableCrossPartitionQuery(isCrossPartitionQuery);
        return getCosmosClient().getDatabase(this.databaseName)
                .getContainer(containerName)
                .queryItems(sqlQuerySpec, options)
                .onErrorResume(this::databaseAccessExceptionHandler)
                .map(cosmosItemFeedResponse -> cosmosItemFeedResponse.getResults()
                        .stream()
                        .map(cosmosItem -> cosmosItem.delete(new CosmosItemRequestOptions())))
                .then();
    }

    /**
     * Delete items matching query
     *
     * @param query         the document query
     * @param entityClass   the entity class
     * @param containerName the container name
     * @return Mono
     */
    @Override
    public <T> Mono<T> delete(DocumentQuery query, Class<T> entityClass, String containerName) {
        Assert.notNull(query, "DocumentQuery should not be null.");
        Assert.notNull(entityClass, "domainClass should not be null.");
        Assert.hasText(containerName, "container name should not be null, empty or only whitespaces");

        final Flux<CosmosItem> results = findDocuments(query, entityClass, containerName);
        final List<String> partitionKeyName = getPartitionKeyNames(entityClass);

        results.flatMap(d -> deleteDocument(d, partitionKeyName));

        return null;
    }

    /**
     * Find items
     *
     * @param query         the document query
     * @param entityClass   the entity class
     * @param containerName the container name
     * @return Flux with found items or error
     */
    @Override
    public <T> Flux<T> find(DocumentQuery query, Class<T> entityClass, String containerName) {
        return findDocuments(query, entityClass, containerName)
                .map(cosmosItem -> cosmosItem.toObject(entityClass));
    }

    /**
     * Exists
     *
     * @param query         the document query
     * @param entityClass   the entity class
     * @param containerName the container name
     * @return Mono with a boolean or error
     */
    @Override
    public Mono<Boolean> exists(DocumentQuery query, Class<?> entityClass, String containerName) {
        return getCountValue(query, true, containerName).flatMap(count -> Mono.just(count > 0));
    }

    /**
     * Exists
     * @param id the id
     * @param entityClass the entity class
     * @param containerName the containercontainer nam,e
     * @return
     */
    public Mono<Boolean> existsById(Object id, Class<?> entityClass, String containerName) {
        return findById(containerName, id, entityClass)
                .flatMap(o -> Mono.just(o !=  null));
    }

    /**
     * Count
     *
     * @param containerName the container name
     * @return Mono with the count or error
     */
    @Override
    public Mono<Long> count(String containerName) {
        final DocumentQuery query = new DocumentQuery(Criteria.getInstance(CriteriaType.ALL));
        return getCountValue(query, true, containerName);
    }

    /**
     * Count
     *
     * @param query         the document query
     * @param containerName the container name
     * @return Mono with count or error
     */
    @Override
    public Mono<Long> count(DocumentQuery query, String containerName) {
        return getCountValue(query, true, containerName);
    }

    private Mono<Long> getCountValue(DocumentQuery query, boolean isCrossPartitionQuery, String containerName) {
        final SqlQuerySpec querySpec = new CountQueryGenerator().generateCosmos(query);
        final FeedOptions options = new FeedOptions();

        options.setEnableCrossPartitionQuery(isCrossPartitionQuery);

        return executeQuery(querySpec, containerName, options)
                .onErrorResume(this::databaseAccessExceptionHandler)
                .next()
                .map(r -> r.getResults().get(0).getLong(COUNT_VALUE_KEY));
    }

    private Flux<FeedResponse<CosmosItem>> executeQuery(SqlQuerySpec sqlQuerySpec, String collectionName,
                                                        FeedOptions options) {

        return getCosmosClient().getDatabase(this.databaseName)
                .getContainer(collectionName)
                .queryItems(sqlQuerySpec, options);
    }

    private <T> Mono<T> databaseAccessExceptionHandler(Throwable e) {
        throw new DocumentDBAccessException("failed to access cosmosdb database", e);
    }

    /**
     * Delete container with container name
     *
     * @param containerName the container name
     */
    @Override
    public void deleteContainer(@NonNull String containerName) {
        Assert.hasText(containerName, "containerName should have text.");
        try {
            getCosmosClient().getDatabase(this.databaseName).getContainer(containerName).delete().block();
            this.collectionCache.remove(containerName);
        } catch (Exception e) {
            throw new DocumentDBAccessException("failed to delete collection: " + containerName, e);
        }
    }

    /**
     * @param domainClass the domain class
     * @return the container name
     */
    public String getcontainerName(Class<?> domainClass) {
        Assert.notNull(domainClass, "domainClass should not be null");

        return new DocumentDbEntityInformation<>(domainClass).getCollectionName();
    }

    private Flux<CosmosItem> findDocuments(@NonNull DocumentQuery query, @NonNull Class<?> domainClass,
                                           @NonNull String containerName) {
        final SqlQuerySpec sqlQuerySpec = new FindQuerySpecGenerator().generateCosmos(query);
        final boolean isCrossPartitionQuery = query.isCrossPartitionQuery(getPartitionKeyNames(domainClass));
        final FeedOptions feedOptions = new FeedOptions();
        feedOptions.setEnableCrossPartitionQuery(isCrossPartitionQuery);
        return getCosmosClient()
                .getDatabase(this.databaseName)
                .getContainer(containerName)
                .queryItems(sqlQuerySpec, feedOptions)
                .flatMap(cosmosItemFeedResponse -> Flux.fromIterable(cosmosItemFeedResponse.getResults()));
    }

    private void assertValidId(Object id) {
        Assert.notNull(id, "id should not be null");
        if (id instanceof String) {
            Assert.hasText(id.toString(), "id should not be empty or only whitespaces.");
        }
    }

    private List<String> getPartitionKeyNames(Class<?> domainClass) {
        final DocumentDbEntityInformation entityInfo = new DocumentDbEntityInformation(domainClass);

        if (entityInfo.getPartitionKeyFieldName() == null) {
            return new ArrayList<>();
        }

        return Collections.singletonList(entityInfo.getPartitionKeyFieldName());
    }

    private Mono<Void> deleteDocument(@NonNull CosmosItem cosmosItem, @NonNull List<String> partitionKeyNames) {
        Assert.isTrue(partitionKeyNames.size() <= 1, "Only one Partition is supported.");

        PartitionKey partitionKey = null;

        if (!partitionKeyNames.isEmpty() && StringUtils.hasText(partitionKeyNames.get(0))) {
            partitionKey = new PartitionKey(cosmosItem.get(partitionKeyNames.get(0)));
        }

        final CosmosItemRequestOptions options = new CosmosItemRequestOptions(partitionKey);

        return cosmosItem.delete(options).then();
    }

}
