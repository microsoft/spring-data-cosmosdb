/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.integration;

import com.microsoft.azure.spring.data.cosmosdb.common.TestConstants;
import com.microsoft.azure.spring.data.cosmosdb.common.TestUtils;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentDbPageRequest;
import com.microsoft.azure.spring.data.cosmosdb.domain.Address;
import com.microsoft.azure.spring.data.cosmosdb.repository.TestRepositoryConfig;
import com.microsoft.azure.spring.data.cosmosdb.repository.repository.PageableAddressRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestRepositoryConfig.class)
public class PageableAddressRepositoryIT {
    private static final Address TEST_ADDRESS1_PARTITION1 = new Address(
            TestConstants.POSTAL_CODE, TestConstants.STREET, TestConstants.CITY);
    private static final Address TEST_ADDRESS2_PARTITION1 = new Address(
            TestConstants.POSTAL_CODE_0, TestConstants.STREET, TestConstants.CITY);
    private static final Address TEST_ADDRESS1_PARTITION2 = new Address(
            TestConstants.POSTAL_CODE_1, TestConstants.STREET_0, TestConstants.CITY_0);
    private static final Address TEST_ADDRESS4_PARTITION3 = new Address(
            TestConstants.POSTAL_CODE, TestConstants.STREET_1, TestConstants.CITY_1);

    private static final int PAGE_SIZE = 3;
    private static final int MIN_PAGE_SIZE = 1;

    @Autowired
    private PageableAddressRepository repository;

    @Before
    public void setup() {
        repository.save(TEST_ADDRESS1_PARTITION1);
        repository.save(TEST_ADDRESS1_PARTITION2);
        repository.save(TEST_ADDRESS2_PARTITION1);
        repository.save(TEST_ADDRESS4_PARTITION3);
    }

    @After
    public void cleanup() {
        repository.deleteAll();
    }

    @Test
    public void testFindAll() {
        final List<Address> result = TestUtils.toList(repository.findAll());

        assertThat(result.size()).isEqualTo(4);
    }

    @Test
    public void testFindAllByPage() {
        final DocumentDbPageRequest pageRequest = new DocumentDbPageRequest(0, PAGE_SIZE, null);
        final Page<Address> page = repository.findAll(pageRequest);

        assertThat(page.getContent().size()).isEqualTo(PAGE_SIZE);
        validateNonLastPage(page, PAGE_SIZE);

        final Page<Address> nextPage = repository.findAll(page.getPageable());
        assertThat(nextPage.getContent().size()).isEqualTo(1);
        validateLastPage(nextPage, PAGE_SIZE);
    }

    @Test
    public void testFindWithParitionKeySinglePage() {
        final DocumentDbPageRequest pageRequest = new DocumentDbPageRequest(0, PAGE_SIZE, null);
        final Page<Address> page = repository.findByCity(TestConstants.CITY, pageRequest);

        assertThat(page.getContent().size()).isEqualTo(2);
        validateResultCityMatch(page, TestConstants.CITY);
        validateLastPage(page, PAGE_SIZE);
    }

    @Test
    public void testFindWithParitionKeyMultiPages() {
        final DocumentDbPageRequest pageRequest = new DocumentDbPageRequest(0, MIN_PAGE_SIZE, null);
        final Page<Address> page = repository.findByCity(TestConstants.CITY, pageRequest);

        assertThat(page.getContent().size()).isEqualTo(MIN_PAGE_SIZE);
        validateResultCityMatch(page, TestConstants.CITY);
        validateNonLastPage(page, MIN_PAGE_SIZE);

        final Page<Address> nextPage = repository.findByCity(TestConstants.CITY, page.getPageable());

        assertThat(nextPage.getContent().size()).isEqualTo(MIN_PAGE_SIZE);
        validateResultCityMatch(page, TestConstants.CITY);
        validateLastPage(nextPage, MIN_PAGE_SIZE);
    }

    @Test
    public void testFindWithoutPartitionKeySinglePage() {
        final DocumentDbPageRequest pageRequest = new DocumentDbPageRequest(0, PAGE_SIZE, null);
        final Page<Address> page = repository.findByStreet(TestConstants.STREET, pageRequest);

        assertThat(page.getContent().size()).isEqualTo(2);
        validateResultStreetMatch(page, TestConstants.STREET);
        validateLastPage(page, PAGE_SIZE);
    }

    @Test
    public void testFindWithoutPartitionKeyMultiPages() {
        final DocumentDbPageRequest pageRequest = new DocumentDbPageRequest(0, MIN_PAGE_SIZE, null);
        final Page<Address> page = repository.findByStreet(TestConstants.STREET, pageRequest);

        assertThat(page.getContent().size()).isEqualTo(1);
        validateResultStreetMatch(page, TestConstants.STREET);
        validateNonLastPage(page, MIN_PAGE_SIZE);

        final Page<Address> nextPage = repository.findByStreet(TestConstants.STREET, page.getPageable());

        assertThat(nextPage.getContent().size()).isEqualTo(MIN_PAGE_SIZE);
        validateResultStreetMatch(page, TestConstants.STREET);
        validateLastPage(nextPage, MIN_PAGE_SIZE);
    }

    private void validateLastPage(Page page, int pageSize) {
        final Pageable pageable = page.getPageable();

        assertThat(pageable).isInstanceOf(DocumentDbPageRequest.class);
        assertThat(((DocumentDbPageRequest) pageable).getRequestContinuation()).isNullOrEmpty();
        assertThat(pageable.getPageSize()).isEqualTo(pageSize);
    }

    private void validateNonLastPage(Page page, int pageSize) {
        final Pageable pageable = page.getPageable();

        assertThat(pageable).isInstanceOf(DocumentDbPageRequest.class);
        assertThat(((DocumentDbPageRequest) pageable).getRequestContinuation()).isNotNull();
        assertThat(((DocumentDbPageRequest) pageable).getRequestContinuation()).isNotBlank();
        assertThat(pageable.getPageSize()).isEqualTo(pageSize);
    }

    private void validateResultCityMatch(Page<Address> page, String city) {
        assertThat(page.getContent().stream().filter(address -> address.getCity().equals(city))
                .collect(Collectors.toList()).size()).isEqualTo(page.getContent().size());
    }

    private void validateResultStreetMatch(Page<Address> page, String street) {
        assertThat(page.getContent().stream().filter(address -> address.getStreet().equals(street))
                .collect(Collectors.toList()).size()).isEqualTo(page.getContent().size());
    }
}
