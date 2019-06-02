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

    private final DocumentDbEntityInformation<IntegerIdDomain, Integer> entityInformation =
            new DocumentDbEntityInformation<>(IntegerIdDomain.class);

    @Autowired
    private DocumentDbTemplate template;

    @Before
    public void setup() {
        repository.deleteAll();
    }

    @PreDestroy
    public void cleanUpCollection() {
        template.deleteCollection(entityInformation.getCollectionName());
    }

    @Test
    public void testBasicQuery() {
        this.repository.save(DOMAIN);
    }

    @Test
    public void testSaveAndFindById() {
        assert this.repository.save(DOMAIN) != null;

        final Optional<IntegerIdDomain> savedEntity = this.repository.findById(DOMAIN.getNumber());
        assert savedEntity.isPresent();
        assert DOMAIN.equals(savedEntity.get());
    }

    @Test
    public void testSaveAllAndFindAll() {
        assert !this.repository.findAll().iterator().hasNext();

        final Set<IntegerIdDomain> entitiesToSave = Collections.singleton(DOMAIN);
        this.repository.saveAll(entitiesToSave);

        final Set<IntegerIdDomain> savedEntities = StreamSupport.stream(this.repository.findAll().spliterator(), false)
                .collect(Collectors.toSet());

        assert entitiesToSave.containsAll(savedEntities);
    }

    @Test
    public void testFindAllById() {
        this.repository.findAllById(Collections.singleton(DOMAIN.getNumber()));
    }

    @Test
    public void testCount() {
        assert repository.count() == 0;
        this.repository.save(DOMAIN);
        assert repository.count() == 1;
    }

    @Test
    public void testDeleteById() {
        this.repository.save(DOMAIN);
        this.repository.deleteById(DOMAIN.getNumber());
        assert this.repository.count() == 0;
    }

    @Test(expected = DocumentDBAccessException.class)
    public void testDeleteByIdShouldFailIfNothingToDelete() {
        this.repository.deleteById(DOMAIN.getNumber());
    }

    @Test
    public void testDelete() {
        this.repository.save(DOMAIN);
        this.repository.delete(DOMAIN);
        assert this.repository.count() == 0;
    }

    @Test(expected = DocumentDBAccessException.class)
    public void testDeleteShouldFailIfNothingToDelete() {
        this.repository.delete(DOMAIN);
    }

    @Test
    public void testDeleteAll() {
        this.repository.save(DOMAIN);
        this.repository.deleteAll(Collections.singleton(DOMAIN));
        assert this.repository.count() == 0;
    }

    @Test
    public void testExistsById() {
        this.repository.save(DOMAIN);
        assert this.repository.existsById(DOMAIN.getNumber());
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
        assert ascending.size() == 2;
        assert DOMAIN.equals(ascending.get(0));
        assert other.equals(ascending.get(1));

        final Sort descSort = Sort.by(Sort.Direction.DESC, "number");
        final List<IntegerIdDomain> descending = StreamSupport
                .stream(this.repository.findAll(descSort).spliterator(), false)
                .collect(Collectors.toList());
        assert descending.size() == 2;
        assert other.equals(descending.get(0));
        assert DOMAIN.equals(descending.get(1));

    }

    @Test
    public void testFindAllPageable() {
        final IntegerIdDomain other = new IntegerIdDomain(DOMAIN.getNumber() + 1, "other-name");
        this.repository.save(DOMAIN);
        this.repository.save(other);

        final Page<IntegerIdDomain> page1 = this.repository.findAll(new DocumentDbPageRequest(0, 1, null));
        final Iterator<IntegerIdDomain> page1Iterator = page1.iterator();
        assert page1Iterator.hasNext();
        assert DOMAIN.equals(page1Iterator.next());

        final Page<IntegerIdDomain> page2 = this.repository.findAll(new DocumentDbPageRequest(1, 1, null));
        final Iterator<IntegerIdDomain> page2Iterator = page2.iterator();
        assert page2Iterator.hasNext();
        assert DOMAIN.equals(page2Iterator.next());
    }
}
