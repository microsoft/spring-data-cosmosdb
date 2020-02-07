/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.core;

import com.azure.data.cosmos.CosmosClient;
import com.azure.data.cosmos.CosmosContainerResponse;
import com.azure.data.cosmos.CosmosItemProperties;
import com.azure.data.cosmos.CosmosItemRequestOptions;
import com.azure.data.cosmos.FeedOptions;
import com.azure.data.cosmos.FeedResponse;
import com.azure.data.cosmos.PartitionKey;
import com.azure.data.cosmos.SqlQuerySpec;
import com.microsoft.azure.spring.data.cosmosdb.CosmosDbFactory;
import com.microsoft.azure.spring.data.cosmosdb.core.convert.MappingCosmosConverter;
import com.microsoft.azure.spring.data.cosmosdb.core.generator.CountQueryGenerator;
import com.microsoft.azure.spring.data.cosmosdb.core.generator.FindQuerySpecGenerator;
import com.microsoft.azure.spring.data.cosmosdb.core.query.Criteria;
import com.microsoft.azure.spring.data.cosmosdb.core.query.CriteriaType;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.CosmosEntityInformation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.microsoft.azure.spring.data.cosmosdb.common.CosmosdbUtils.fillAndProcessResponseDiagnostics;
import static com.microsoft.azure.spring.data.cosmosdb.exception.CosmosDBExceptionUtils.exceptionHandler;
import static com.microsoft.azure.spring.data.cosmosdb.exception.CosmosDBExceptionUtils.findAPIExceptionHandler;

@Slf4j
public class ReactiveCosmosTemplate implements ReactiveCosmosOperations, ApplicationContextAware {
    private static final String COUNT_VALUE_KEY = "_aggregate";

    private final MappingCosmosConverter mappingCosmosConverter;
    private final String databaseName;

    private final CosmosClient cosmosClient;
    private final ResponseDiagnosticsProcessor responseDiagnosticsProcessor;
    private final boolean isPopulateQueryMetrics;

    private final List<String> collectionCache;

    /**
     * Constructor
     *
     * @param cosmosDbFactory            the cosmosdbfactory
     * @param mappingCosmosConverter the mappingCosmosConverter
     * @param dbName                     database name
     */
    public ReactiveCosmosTemplate(CosmosDbFactory cosmosDbFactory,
                                  MappingCosmosConverter mappingCosmosConverter,
                                  String dbName) {
        Assert.notNull(cosmosDbFactory, "CosmosDbFactory must not be null!");
        Assert.notNull(mappingCosmosConverter, "MappingCosmosConverter must not be null!");

        this.mappingCosmosConverter = mappingCosmosConverter;
        this.databaseName = dbName;
        this.collectionCache = new ArrayList<>();

        this.cosmosClient = cosmosDbFactory.getCosmosClient();
        this.responseDiagnosticsProcessor = cosmosDbFactory.getConfig().getResponseDiagnosticsProcessor();
        this.isPopulateQueryMetrics = cosmosDbFactory.getConfig().isPopulateQueryMetrics();
    }

    /**
     * @param applicationContext the application context
     * @throws BeansException the bean exception
     */
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        //  NOTE: When application context instance variable gets introduced, assign it here.
    }

    /**
     * Creates a collection if it doesn't already exist
     *
     * @param information the CosmosEntityInformation
     * @return Mono containing CosmosContainerResponse
     */
    @Override
    public Mono<CosmosContainerResponse> createCollectionIfNotExists(CosmosEntityInformation information) {

        return cosmosClient
            .createDatabaseIfNotExists(this.databaseName)
            .onErrorResume(throwable ->
                exceptionHandler("Failed to create database", throwable))
            .flatMap(cosmosDatabaseResponse -> {
                fillAndProcessResponseDiagnostics(responseDiagnosticsProcessor,
                    cosmosDatabaseResponse, null);
                return cosmosDatabaseResponse
                    .database()
                    .createContainerIfNotExists(information.getCollectionName(),
                        "/" + information.getPartitionKeyFieldName(), information.getRequestUnit())
                    .map(cosmosContainerResponse -> {
                        fillAndProcessResponseDiagnostics(responseDiagnosticsProcessor,
                            cosmosContainerResponse, null);
                        this.collectionCache.add(information.getCollectionName());
                        return cosmosContainerResponse;
                    })
                    .onErrorResume(throwable ->
                        exceptionHandler("Failed to create collection", throwable));
            });

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
        return findAll(entityClass.getSimpleName(), entityClass);
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
        return findById(getContainerName(entityClass), id, entityClass);
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

        final String query = String.format("select * from root where root.id = '%s'", id.toString());
        final FeedOptions options = new FeedOptions();
        options.enableCrossPartitionQuery(true);
        options.populateQueryMetrics(isPopulateQueryMetrics);

        return cosmosClient.getDatabase(databaseName)
                           .getContainer(containerName)
                           .queryItems(query, options)
                           .flatMap(cosmosItemFeedResponse -> {
                                fillAndProcessResponseDiagnostics(responseDiagnosticsProcessor,
                                    null, cosmosItemFeedResponse);
                                return Mono.justOrEmpty(cosmosItemFeedResponse
                                    .results()
                                    .stream()
                                    .map(cosmosItem -> toDomainObject(entityClass, cosmosItem))
                                    .findFirst());
                           })
                           .onErrorResume(throwable ->
                               findAPIExceptionHandler("Failed to find item", throwable))
                           .next();
    }

    /**
     * Find by id
     *
     * @param id            the id
     * @param entityClass   the entity class
     * @param partitionKey  partition Key
     * @return Mono with the item or error
     */
    @Override
    public <T> Mono<T> findById(Object id, Class<T> entityClass, PartitionKey partitionKey) {
        Assert.notNull(entityClass, "entityClass should not be null");
        assertValidId(id);

        final String containerName = getContainerName(entityClass);
        return cosmosClient.getDatabase(databaseName)
                           .getContainer(containerName)
                           .getItem(id.toString(), partitionKey)
                           .read()
                           .flatMap(cosmosItemResponse -> {
                               fillAndProcessResponseDiagnostics(responseDiagnosticsProcessor,
                                   cosmosItemResponse, null);
                               return Mono.justOrEmpty(toDomainObject(entityClass,
                                   cosmosItemResponse.properties()));
                           })
                           .onErrorResume(throwable ->
                               findAPIExceptionHandler("Failed to find item", throwable));
    }

    /**
     * Insert
     *
     * @param objectToSave the object to save
     * @param partitionKey the partition key
     * @return Mono with the item or error
     */
    public <T> Mono<T> insert(T objectToSave, PartitionKey partitionKey) {
        Assert.notNull(objectToSave, "entityClass should not be null");

        return insert(getContainerName(objectToSave.getClass()), objectToSave, partitionKey);
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
        final CosmosItemProperties originalItem = mappingCosmosConverter.writeCosmosItemProperties(objectToSave);
        return cosmosClient.getDatabase(this.databaseName)
                           .getContainer(getContainerName(objectToSave.getClass()))
                           .createItem(originalItem, new CosmosItemRequestOptions())
                           .onErrorResume(throwable ->
                               exceptionHandler("Failed to insert item", throwable))
                           .flatMap(cosmosItemResponse -> {
                               fillAndProcessResponseDiagnostics(responseDiagnosticsProcessor,
                                   cosmosItemResponse, null);
                               return Mono.just(toDomainObject(domainClass, cosmosItemResponse.properties()));
                           });
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

        final Class<T> domainClass = (Class<T>) objectToSave.getClass();
        final CosmosItemProperties originalItem = mappingCosmosConverter.writeCosmosItemProperties(objectToSave);
        final CosmosItemRequestOptions options = new CosmosItemRequestOptions();
        if (partitionKey != null) {
            options.partitionKey(partitionKey);
        }
        return cosmosClient.getDatabase(this.databaseName)
                           .getContainer(containerName)
                           .createItem(originalItem, options)
                           .onErrorResume(throwable ->
                               exceptionHandler("Failed to insert item", throwable))
                           .flatMap(cosmosItemResponse -> {
                               fillAndProcessResponseDiagnostics(responseDiagnosticsProcessor,
                                   cosmosItemResponse, null);
                               return Mono.just(toDomainObject(domainClass, cosmosItemResponse.properties()));
                           });
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
        return upsert(getContainerName(object.getClass()), object, partitionKey);
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
        final CosmosItemProperties originalItem = mappingCosmosConverter.writeCosmosItemProperties(object);
        final CosmosItemRequestOptions options = new CosmosItemRequestOptions();
        if (partitionKey != null) {
            options.partitionKey(partitionKey);
        }

        return cosmosClient.getDatabase(this.databaseName)
                           .getContainer(containerName)
                           .upsertItem(originalItem, options)
                           .flatMap(cosmosItemResponse -> {
                               fillAndProcessResponseDiagnostics(responseDiagnosticsProcessor,
                                   cosmosItemResponse, null);
                               return Mono.just(toDomainObject(domainClass, cosmosItemResponse.properties()));
                           })
                           .onErrorResume(throwable ->
                               exceptionHandler("Failed to upsert item", throwable));
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

        if (partitionKey == null) {
            partitionKey = PartitionKey.None;
        }

        final CosmosItemRequestOptions options = new CosmosItemRequestOptions();
        options.partitionKey(partitionKey);
        return cosmosClient.getDatabase(this.databaseName)
                           .getContainer(containerName)
                           .getItem(id.toString(), partitionKey)
                           .delete(options)
                           .doOnNext(cosmosItemResponse ->
                               fillAndProcessResponseDiagnostics(responseDiagnosticsProcessor,
                               cosmosItemResponse, null))
                           .onErrorResume(throwable ->
                               exceptionHandler("Failed to delete item", throwable))
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
        final boolean isCrossPartitionQuery = query.isCrossPartitionQuery(Collections.singletonList(partitionKeyName));
        options.enableCrossPartitionQuery(isCrossPartitionQuery);
        options.populateQueryMetrics(isPopulateQueryMetrics);
        return cosmosClient.getDatabase(this.databaseName)
                           .getContainer(containerName)
                           .queryItems(sqlQuerySpec, options)
                           .onErrorResume(throwable ->
                               exceptionHandler("Failed to query items", throwable))
                           .flatMap(cosmosItemFeedResponse -> {
                               fillAndProcessResponseDiagnostics(responseDiagnosticsProcessor,
                                    null, cosmosItemFeedResponse);
                               return Flux.fromIterable(cosmosItemFeedResponse.results());
                           })
                           .flatMap(cosmosItemProperties -> cosmosClient
                               .getDatabase(this.databaseName)
                               .getContainer(containerName)
                               .getItem(cosmosItemProperties.id(), cosmosItemProperties.get(partitionKeyName))
                               .delete()
                               .doOnNext(cosmosItemResponse ->
                                   fillAndProcessResponseDiagnostics(responseDiagnosticsProcessor,
                                       cosmosItemResponse, null))
                               .onErrorResume(throwable ->
                                   exceptionHandler("Failed to delete items", throwable)))
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
    public <T> Flux<T> delete(DocumentQuery query, Class<T> entityClass, String containerName) {
        Assert.notNull(query, "DocumentQuery should not be null.");
        Assert.notNull(entityClass, "domainClass should not be null.");
        Assert.hasText(containerName, "container name should not be null, empty or only whitespaces");

        final Flux<CosmosItemProperties> results = findItems(query, entityClass, containerName);
        final List<String> partitionKeyName = getPartitionKeyNames(entityClass);

        return results.flatMap(d -> deleteItem(d, partitionKeyName, containerName))
                      .flatMap(cosmosItemProperties -> Mono.just(toDomainObject(entityClass, cosmosItemProperties)));
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
        return findItems(query, entityClass, containerName)
                .map(cosmosItemProperties -> toDomainObject(entityClass, cosmosItemProperties));
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
        return count(query, true, containerName).flatMap(count -> Mono.just(count > 0));
    }

    /**
     * Exists
     * @param id the id
     * @param entityClass the entity class
     * @param containerName the containercontainer nam,e
     * @return Mono with a boolean or error
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
        return count(query, true, containerName);
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
        return count(query, true, containerName);
    }

    @Override
    public MappingCosmosConverter getConverter() {
        return mappingCosmosConverter;
    }

    public Mono<Long> count(DocumentQuery query, boolean isCrossPartitionQuery, String containerName) {
        return getCountValue(query, isCrossPartitionQuery, containerName);
    }

    private Mono<Long> getCountValue(DocumentQuery query, boolean isCrossPartitionQuery, String containerName) {
        final SqlQuerySpec querySpec = new CountQueryGenerator().generateCosmos(query);
        final FeedOptions options = new FeedOptions();

        options.enableCrossPartitionQuery(isCrossPartitionQuery);
        options.populateQueryMetrics(isPopulateQueryMetrics);

        return executeQuery(querySpec, containerName, options)
                .doOnNext(feedResponse -> fillAndProcessResponseDiagnostics(responseDiagnosticsProcessor,
                    null, feedResponse))
                .onErrorResume(throwable ->
                    exceptionHandler("Failed to get count value", throwable))
                .next()
                .map(r -> r.results().get(0).getLong(COUNT_VALUE_KEY));
    }

    private Flux<FeedResponse<CosmosItemProperties>> executeQuery(SqlQuerySpec sqlQuerySpec, String collectionName,
                                                        FeedOptions options) {

        return cosmosClient.getDatabase(this.databaseName)
                           .getContainer(collectionName)
                           .queryItems(sqlQuerySpec, options)
                           .onErrorResume(throwable ->
                               exceptionHandler("Failed to execute query", throwable));
    }

    /**
     * Delete container with container name
     *
     * @param containerName the container name
     */
    @Override
    public void deleteContainer(@NonNull String containerName) {
        Assert.hasText(containerName, "containerName should have text.");
        cosmosClient.getDatabase(this.databaseName)
                    .getContainer(containerName)
                    .delete()
                    .doOnNext(cosmosContainerResponse ->
                        fillAndProcessResponseDiagnostics(responseDiagnosticsProcessor,
                            cosmosContainerResponse, null))
                    .onErrorResume(throwable ->
                        exceptionHandler("Failed to delete container", throwable))
                    .block();
        this.collectionCache.remove(containerName);
    }

    /**
     * @param domainClass the domain class
     * @return the container name
     */
    public String getContainerName(Class<?> domainClass) {
        Assert.notNull(domainClass, "domainClass should not be null");

        return new CosmosEntityInformation<>(domainClass).getCollectionName();
    }

    private Flux<CosmosItemProperties> findItems(@NonNull DocumentQuery query, @NonNull Class<?> domainClass,
                                                 @NonNull String containerName) {
        final SqlQuerySpec sqlQuerySpec = new FindQuerySpecGenerator().generateCosmos(query);
        final boolean isCrossPartitionQuery = query.isCrossPartitionQuery(getPartitionKeyNames(domainClass));
        final FeedOptions feedOptions = new FeedOptions();
        feedOptions.enableCrossPartitionQuery(isCrossPartitionQuery);
        feedOptions.populateQueryMetrics(isPopulateQueryMetrics);

        return cosmosClient
                .getDatabase(this.databaseName)
                .getContainer(containerName)
                .queryItems(sqlQuerySpec, feedOptions)
                .flatMap(cosmosItemFeedResponse -> {
                    fillAndProcessResponseDiagnostics(responseDiagnosticsProcessor,
                        null, cosmosItemFeedResponse);
                    return Flux.fromIterable(cosmosItemFeedResponse.results());
                })
                .onErrorResume(throwable ->
                    exceptionHandler("Failed to query items", throwable));
    }

    private void assertValidId(Object id) {
        Assert.notNull(id, "id should not be null");
        if (id instanceof String) {
            Assert.hasText(id.toString(), "id should not be empty or only whitespaces.");
        }
    }

    private List<String> getPartitionKeyNames(Class<?> domainClass) {
        final CosmosEntityInformation<?, ?> entityInfo = new CosmosEntityInformation<>(domainClass);

        if (entityInfo.getPartitionKeyFieldName() == null) {
            return new ArrayList<>();
        }

        return Collections.singletonList(entityInfo.getPartitionKeyFieldName());
    }

    private Mono<CosmosItemProperties> deleteItem(@NonNull CosmosItemProperties cosmosItemProperties,
                                                  @NonNull List<String> partitionKeyNames,
                                                  String containerName) {
        Assert.isTrue(partitionKeyNames.size() <= 1, "Only one Partition is supported.");

        PartitionKey partitionKey = null;

        if (!partitionKeyNames.isEmpty() && StringUtils.hasText(partitionKeyNames.get(0))) {
            partitionKey = new PartitionKey(cosmosItemProperties.get(partitionKeyNames.get(0)));
        }

        final CosmosItemRequestOptions options = new CosmosItemRequestOptions(partitionKey);

        return cosmosClient.getDatabase(this.databaseName)
                           .getContainer(containerName)
                           .getItem(cosmosItemProperties.id(), partitionKey)
                           .delete(options)
                           .map(cosmosItemResponse -> {
                               fillAndProcessResponseDiagnostics(responseDiagnosticsProcessor,
                                   cosmosItemResponse, null);
                               return cosmosItemProperties;
                           })
                           .onErrorResume(throwable ->
                               exceptionHandler("Failed to delete item", throwable));
    }

    private <T> T toDomainObject(@NonNull Class<T> domainClass, CosmosItemProperties cosmosItemProperties) {
        return mappingCosmosConverter.read(domainClass, cosmosItemProperties);
    }

}
