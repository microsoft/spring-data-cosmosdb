/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.performance;

import com.google.common.collect.Lists;
import com.microsoft.azure.documentdb.DocumentClient;
import com.microsoft.azure.documentdb.DocumentClientException;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentDbPageRequest;
import com.microsoft.azure.spring.data.cosmosdb.performance.domain.PerfPerson;
import com.microsoft.azure.spring.data.cosmosdb.performance.repository.PerfPersonRepository;
import com.microsoft.azure.spring.data.cosmosdb.performance.service.SdkService;
import com.microsoft.azure.spring.data.cosmosdb.performance.utils.Constants;
import com.microsoft.azure.spring.data.cosmosdb.performance.utils.DatabaseUtils;
import com.microsoft.azure.spring.data.cosmosdb.performance.utils.PerfDataProvider;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.microsoft.azure.spring.data.cosmosdb.performance.utils.FunctionUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = PerfConfiguration.class)
public class PerformanceCompare {
    @Autowired
    private DocumentClient documentClient;

    @Autowired
    private PerfPersonRepository repository;

    private static final int TIMES = 100;
    private static final int BATCH_SIZE = 10;
    private static final float ACCEPTANCE_DIFF = 0.1f;
    private static boolean hasInit = false;
    private static SdkService sdkService;
    private static PerformanceReport report = new PerformanceReport();

    @Before
    public void setup() throws DocumentClientException {
        if (!hasInit) {
            DatabaseUtils.createDatabase(documentClient, Constants.PERF_DATABASE_NAME);
            DatabaseUtils.createCollection(documentClient, Constants.PERF_DATABASE_NAME,
                    Constants.SPRING_COLLECTION_NAME);
            DatabaseUtils.createCollection(documentClient, Constants.PERF_DATABASE_NAME, Constants.SDK_COLLECTION_NAME);

            sdkService = new SdkService(documentClient, Constants.PERF_DATABASE_NAME, Constants.SDK_COLLECTION_NAME);
            hasInit = true;
        }
    }

    @After
    public void clear() {
        repository.deleteAll();
        sdkService.deleteAll();
    }

    @AfterClass
    public static void printReport() {
        report.getPerfItems().forEach(System.out::println);
    }

    @Test
    public void saveOneRecordTest() {
        final List<PerfPerson> personList = PerfDataProvider.getPerfData(TIMES);

        final long springCost = applyInputListFunc(personList, repository::save);
        final long sdkCost = applyInputListFunc(personList, sdkService::save);

        verifyResult(OperationType.SAVE_ONE, springCost, sdkCost, ACCEPTANCE_DIFF);
    }

    @Test
    public void saveMultipleRecordsTest() {
        final List<Iterable<PerfPerson>> personList = PerfDataProvider.getMultiPerfData(TIMES, BATCH_SIZE);

        final long springCost = acceptInputListFunc(personList, repository::saveAll);
        final long sdkCost = acceptInputListFunc(personList, sdkService::saveAll);

        verifyResult(OperationType.SAVE_ALL, springCost, sdkCost, ACCEPTANCE_DIFF);
    }

    @Test
    public void deleteOneRecordTest() {
        final List<PerfPerson> personList = prepareListData(TIMES);

        final long springCost = acceptInputListFunc(personList, repository::delete);
        final long sdkCost = acceptInputListFunc(personList, sdkService::delete);

        verifyResult(OperationType.DELETE_ONE, springCost, sdkCost, ACCEPTANCE_DIFF);
    }

    @Test
    public void deleteAllRecordsTest() {
        final List<Iterable<PerfPerson>> personList = prepareListBatchData(TIMES, BATCH_SIZE);

        final long springCost = acceptInputListFunc(personList, repository::deleteAll);
        final long sdkCost = acceptInputListFunc(personList, sdkService::deleteAll);

        verifyResult(OperationType.DELETE_ALL, springCost, sdkCost, ACCEPTANCE_DIFF);
    }

    @Test
    public void findByIdTest() {
        final List<String> idList = prepareListData(TIMES).stream().map(PerfPerson::getId).collect(Collectors.toList());

        final long springCost = applyInputListFunc(idList, repository::findById);
        final long sdkCost = applyInputListFunc(idList, sdkService::findById);

        verifyResult(OperationType.FIND_BY_ID, springCost, sdkCost, ACCEPTANCE_DIFF);
    }

    @Test
    public void findByMultipleIdsTest() {
        final List<Iterable<String>> idList = listBatchIds(TIMES, BATCH_SIZE);

        final long springCost = acceptInputListFunc(idList, repository::findAllById);
        final long sdkCost = acceptInputListFunc(idList, sdkService::findAllById);

        verifyResult(OperationType.FIND_BY_IDS, springCost, sdkCost, ACCEPTANCE_DIFF);
    }

    @Test
    public void findAllTest() {
        prepareListData(TIMES);

        final long springCost = getSupplier(TIMES, repository::findAll);
        final long sdkCost = getSupplier(TIMES, sdkService::findAll);

        verifyResult(OperationType.FIND_ALL, springCost, sdkCost, ACCEPTANCE_DIFF);
    }

    @Test
    public void deleteAllTest() {
        final long springCost = getSupplier(TIMES, this::springDeleteAll);
        final long sdkCost = getSupplier(TIMES, sdkService::deleteAll);

        verifyResult(OperationType.DELETE_ALL, springCost, sdkCost, ACCEPTANCE_DIFF);
    }

    @Test
    public void findBySortingTest() {
        final Sort sort = new Sort(Sort.Direction.ASC, "name");
        final List<Sort> sortList = buildSortList(sort, TIMES);

        final long springCost = applyInputListFunc(sortList, repository::findAll);
        final long sdkCost = applyInputListFunc(sortList, sdkService::searchDocuments);

        verifyResult(OperationType.FIND_BY_SORT, springCost, sdkCost, ACCEPTANCE_DIFF);
    }

    @Test
    public void findByPagingTest() {
        prepareListData(TIMES);
        int pageSize = TIMES / 2;
        pageSize = pageSize >= 1 ? pageSize : 1;

        final long springCost = runConsumerForTimes(TIMES, pageSize, this::queryTwoPages);
        final long sdkCost = runConsumerForTimes(TIMES, pageSize, sdkService::queryTwoPages);

        verifyResult(OperationType.FIND_BY_PAGING, springCost, sdkCost, ACCEPTANCE_DIFF);
    }

    @Test
    public void findByFieldTest() {
        final List<PerfPerson> data = prepareListData(TIMES);

        final String name = data.get(TIMES / 2).getName();

        final long springCost = runFunctionForTimes(TIMES, name, repository::findByName);
        final long sdkCost = runConsumerForTimes(TIMES, name, sdkService::findByName);

        verifyResult(OperationType.FIND_BY_FIELD, springCost, sdkCost, ACCEPTANCE_DIFF);
    }

    @Test
    public void countTest() {
        final long springCost = getSupplier(TIMES, repository::count);
        final long sdkCost = getSupplier(TIMES, sdkService::count);

        report.addItem(new PerfItem(OperationType.COUNT, springCost, sdkCost, TIMES));
        assertPerf(springCost, sdkCost, ACCEPTANCE_DIFF);
    }

    /**
     * Check whether two time cost fall into the acceptable range.
     * @param timeCostSpring
     * @param timeCostSdk
     * @param acceptanceDiff The acceptable diff between two time cost.
     */
    private void assertPerf(long timeCostSpring, long timeCostSdk, float acceptanceDiff) {
        final long diff = timeCostSpring - timeCostSdk;
        final float actualDiff = (float) diff / timeCostSdk;

        assertThat(actualDiff).isLessThan(acceptanceDiff);
    }

    private void verifyResult(OperationType type, long timeCostSpring, long timeCostSdk, float acceptanceDiff) {
        report.addItem(new PerfItem(type, timeCostSpring, timeCostSdk, TIMES));
        assertPerf(timeCostSpring, timeCostSdk, acceptanceDiff);
    }

    private boolean springDeleteAll() {
        repository.deleteAll();
        return true;  // To provide return value for Supplier
    }

    private List<Sort> buildSortList(Sort sort, int times) {
        final List<Sort> sorts = Lists.newArrayList();
        for (int i = 0; i < times; i++) {
            sorts.add(sort);
        }

        return sorts;
    }

    private void queryTwoPages(int pageSize) {
        final Pageable pageable = new DocumentDbPageRequest(0, pageSize, null);
        final Page<PerfPerson> page = this.repository.findAll(pageable);
        this.repository.findAll(page.getPageable());
    }

    private List<PerfPerson> prepareListData(int count) {
        final List<PerfPerson> personList = PerfDataProvider.getPerfData(count);

        applyInputListFunc(personList, repository::save);
        applyInputListFunc(personList, sdkService::save);

        return personList;
    }


    private List<Iterable<PerfPerson>> prepareListBatchData(int times, int batchSize) {
        final List<Iterable<PerfPerson>> personList = PerfDataProvider.getMultiPerfData(times, batchSize);

        applyInputListFunc(personList, repository::saveAll);
        applyInputListFunc(personList, sdkService::saveAll);

        return personList;
    }

    private List<Iterable<String>> listBatchIds(int times, int batchSize) {
        return prepareListBatchData(times, batchSize).stream()
                .map(iterable -> {
                    final List<String> batchIds = Lists.newArrayList();
                    iterable.forEach(person -> batchIds.add(person.getId()));
                    return batchIds;
                }).collect(Collectors.toList());
    }
}
