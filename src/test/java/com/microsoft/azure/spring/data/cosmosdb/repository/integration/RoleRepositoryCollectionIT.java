/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.integration;

import com.microsoft.azure.spring.data.cosmosdb.core.CosmosTemplate;
import com.microsoft.azure.spring.data.cosmosdb.core.query.CosmosPageRequest;
import com.microsoft.azure.spring.data.cosmosdb.domain.IntegerIdDomain;
import com.microsoft.azure.spring.data.cosmosdb.exception.CosmosDBAccessException;
import com.microsoft.azure.spring.data.cosmosdb.repository.TestRepositoryConfig;
import com.microsoft.azure.spring.data.cosmosdb.repository.repository.IntegerIdDomainRepository;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.CosmosEntityInformation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestRepositoryConfig.class)
public class RoleRepositoryCollectionIT {

    private static final IntegerIdDomain DOMAIN = new IntegerIdDomain(2, "fake-name");

    @Autowired
    private IntegerIdDomainRepository repository;

    private final CosmosEntityInformation<IntegerIdDomain, Integer> entityInformation =
            new CosmosEntityInformation<>(IntegerIdDomain.class);

    @Autowired
    private CosmosTemplate template;

    @Before
    public void setUp() {
        repository.deleteAll();
    }

    @PreDestroy
    public void cleanUpCollection() {
        template.deleteContainer(entityInformation.getContainerName());
    }

    @Test
    public void testBasicQuery() {
        this.repository.save(DOMAIN);
    }

    @Test
    public void testSaveAndFindById() {
        Assert.assertNotNull(this.repository.save(DOMAIN));

        final Optional<IntegerIdDomain> savedEntity = this.repository.findById(DOMAIN.getNumber());
        Assert.assertTrue(savedEntity.isPresent());
        Assert.assertEquals(DOMAIN, savedEntity.get());
    }

    @Test
    public void testSaveAllAndFindAll() {
        Assert.assertFalse(this.repository.findAll().iterator().hasNext());

        final Set<IntegerIdDomain> entitiesToSave = Collections.singleton(DOMAIN);
        this.repository.saveAll(entitiesToSave);

        final Set<IntegerIdDomain> savedEntities = StreamSupport.stream(this.repository.findAll().spliterator(), false)
                .collect(Collectors.toSet());

        Assert.assertTrue(entitiesToSave.containsAll(savedEntities));
    }

    @Test
    public void testFindAllById() {
        this.repository.findAllById(Collections.singleton(DOMAIN.getNumber()));
    }

    @Test
    public void testCount() {
        Assert.assertEquals(0, repository.count());
        this.repository.save(DOMAIN);
        Assert.assertEquals(1, repository.count());
    }

    @Test
    public void testDeleteById() {
        this.repository.save(DOMAIN);
        this.repository.deleteById(DOMAIN.getNumber());
        Assert.assertEquals(0, this.repository.count());
    }

    @Test(expected = CosmosDBAccessException.class)
    public void testDeleteByIdShouldFailIfNothingToDelete() {
        this.repository.deleteById(DOMAIN.getNumber());
    }

    @Test
    public void testDelete() {
        this.repository.save(DOMAIN);
        this.repository.delete(DOMAIN);
        Assert.assertEquals(0, this.repository.count());
    }

    @Test(expected = CosmosDBAccessException.class)
    public void testDeleteShouldFailIfNothingToDelete() {
        this.repository.delete(DOMAIN);
    }

    @Test
    public void testDeleteAll() {
        this.repository.save(DOMAIN);
        this.repository.deleteAll(Collections.singleton(DOMAIN));
        Assert.assertEquals(0, this.repository.count());
    }

    @Test
    public void testExistsById() {
        this.repository.save(DOMAIN);
        Assert.assertTrue(this.repository.existsById(DOMAIN.getNumber()));
    }

    @Test
    public void testFindAllSort() {
        final IntegerIdDomain other = new IntegerIdDomain(DOMAIN.getNumber() + 1, "other-name");
        this.repository.save(other);
        this.repository.save(DOMAIN);

        final Sort ascSort = Sort.by(Sort.Direction.ASC, "number");
        final List<IntegerIdDomain> ascending = StreamSupport
                .stream(this.repository.findAll(ascSort).spliterator(), false)
                .collect(Collectors.toList());
        Assert.assertEquals(2, ascending.size());
        Assert.assertEquals(DOMAIN, ascending.get(0));
        Assert.assertEquals(other, ascending.get(1));

        final Sort descSort = Sort.by(Sort.Direction.DESC, "number");
        final List<IntegerIdDomain> descending = StreamSupport
                .stream(this.repository.findAll(descSort).spliterator(), false)
                .collect(Collectors.toList());
        Assert.assertEquals(2, descending.size());
        Assert.assertEquals(other, descending.get(0));
        Assert.assertEquals(DOMAIN, descending.get(1));

    }

    @Test
    public void testFindAllPageable() {
        final IntegerIdDomain other = new IntegerIdDomain(DOMAIN.getNumber() + 1, "other-name");
        this.repository.save(DOMAIN);
        this.repository.save(other);

        final Page<IntegerIdDomain> page1 = this.repository.findAll(new CosmosPageRequest(0, 1, null));
        final Iterator<IntegerIdDomain> page1Iterator = page1.iterator();
        Assert.assertTrue(page1Iterator.hasNext());
        Assert.assertEquals(DOMAIN, page1Iterator.next());

        final Page<IntegerIdDomain> page2 = this.repository.findAll(new CosmosPageRequest(1, 1, null));
        final Iterator<IntegerIdDomain> page2Iterator = page2.iterator();
        Assert.assertTrue(page2Iterator.hasNext());
        Assert.assertEquals(DOMAIN, page2Iterator.next());
    }
}
