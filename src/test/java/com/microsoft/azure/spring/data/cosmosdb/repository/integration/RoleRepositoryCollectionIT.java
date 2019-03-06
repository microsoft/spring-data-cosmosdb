/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.integration;

import com.microsoft.azure.spring.data.cosmosdb.core.DocumentDbTemplate;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentDbPageRequest;
import com.microsoft.azure.spring.data.cosmosdb.domain.IntegerIdDomain;
import com.microsoft.azure.spring.data.cosmosdb.exception.DocumentDBAccessException;
import com.microsoft.azure.spring.data.cosmosdb.repository.TestRepositoryConfig;
import com.microsoft.azure.spring.data.cosmosdb.repository.repository.IntegerIdDomainRepository;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.DocumentDbEntityInformation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.PreDestroy;
import java.util.Collections;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestRepositoryConfig.class)
public class RoleRepositoryCollectionIT {

    private static final IntegerIdDomain DOMAIN = new IntegerIdDomain(2, "fake-name");

    @Autowired
    private IntegerIdDomainRepository repository;

    private final DocumentDbEntityInformation<IntegerIdDomain, Integer> entityInformation =
            new DocumentDbEntityInformation<>(IntegerIdDomain.class);

    @Autowired
    private DocumentDbTemplate template;

    @PreDestroy
    public void cleanUpCollection() {
        template.deleteCollection(entityInformation.getCollectionName());
    }

    @Test
    public void testBasicQuery() {
        this.repository.save(DOMAIN);
    }

    @Test
    public void testSaveAll() {
        this.repository.saveAll(Collections.singleton(DOMAIN));
    }

    @Test
    public void testFindAll() {
        this.repository.findAll();
    }

    @Test
    public void testFindAllById() {
        this.repository.findAllById(Collections.singleton(DOMAIN.getNumber()));
    }

    @Test
    public void testFindById() {
        this.repository.findById(DOMAIN.getNumber());
    }

    @Test
    public void testCount() {
        this.repository.count();
    }

    @Test(expected = DocumentDBAccessException.class)
    public void testDeleteById() {
        this.repository.deleteById(DOMAIN.getNumber());
    }

    @Test(expected = DocumentDBAccessException.class)
    public void testDelete() {
        this.repository.delete(DOMAIN);
    }

    @Test
    public void testDeleteAll() {
        this.repository.deleteAll(Collections.singleton(DOMAIN));
    }

    @Test
    public void testExistsById() {
        this.repository.existsById(DOMAIN.getNumber());
    }

    @Test
    public void testFindAllSort() {
        this.repository.findAll(Sort.unsorted());
    }

    @Test
    public void testFindAllPageable() {
        final DocumentDbPageRequest pageRequest = new DocumentDbPageRequest(0, 3, null);
        this.repository.findAll(pageRequest);
    }
}
