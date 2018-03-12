/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.repository;

import com.microsoft.azure.documentdb.*;
import com.microsoft.azure.spring.data.documentdb.Constants;
import com.microsoft.azure.spring.data.documentdb.core.DocumentDbTemplate;
import com.microsoft.azure.spring.data.documentdb.core.convert.MappingDocumentDbConverter;
import com.microsoft.azure.spring.data.documentdb.core.mapping.DocumentDbMappingContext;
import com.microsoft.azure.spring.data.documentdb.domain.Person;
import com.microsoft.azure.spring.data.documentdb.domain.Role;
import com.microsoft.azure.spring.data.documentdb.repository.support.DocumentDbEntityInformation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScanner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.annotation.Persistent;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

@RunWith(SpringJUnit4ClassRunner.class)
@PropertySource(value = {"classpath:application.properties"})
public class DocumentDBAnnotationIT {
    private static final Person TEST_PERSON = new Person(Constants.TEST_ID, Constants.TEST_FIRST_NAME,
            Constants.TEST_LAST_NAME, Constants.HOBBIES, Constants.ADDRESSES);
    private static final Role TEST_ROLE = new Role(Constants.TEST_ID, Constants.TEST_LEVEL, Constants.TEST_ROLE_NAME);

    @Value("${documentdb.uri}")
    private String dbUri;

    @Value("${documentdb.key}")
    private String dbKey;

    @Autowired
    private ApplicationContext applicationContext;

    private DocumentClient dbClient;
    private DocumentDbTemplate dbTemplate;
    private DocumentCollection collectionRole;
    private DocumentCollection collectionPerson;
    private DocumentDbMappingContext dbContext;
    private MappingDocumentDbConverter mappingConverter;
    private DocumentDbEntityInformation<Role, String> roleInfo;
    private DocumentDbEntityInformation<Person, String> personInfo;

    @Before
    public void setUp() throws ClassNotFoundException {
        roleInfo = new DocumentDbEntityInformation<>(Role.class);
        personInfo = new DocumentDbEntityInformation<>(Person.class);
        dbContext = new DocumentDbMappingContext();

        dbContext.setInitialEntitySet(new EntityScanner(this.applicationContext).scan(Persistent.class));

        mappingConverter = new MappingDocumentDbConverter(dbContext);
        dbClient = new DocumentClient(dbUri, dbKey, ConnectionPolicy.GetDefault(), ConsistencyLevel.Session);
        dbTemplate = new DocumentDbTemplate(dbClient, mappingConverter, Constants.TEST_DB_NAME);

        IndexingPolicy policy = roleInfo.getIndexingPolicy();

        collectionRole = dbTemplate.createCollectionIfNotExists(roleInfo.getCollectionName(), null, null, policy);
        dbTemplate.insert(roleInfo.getCollectionName(), TEST_ROLE, null);

        policy = personInfo.getIndexingPolicy();

        collectionPerson = dbTemplate.createCollectionIfNotExists(personInfo.getCollectionName(), null, null, policy);
        dbTemplate.insert(personInfo.getCollectionName(), TEST_PERSON, null);
    }

    @After
    public void cleanUp() {
        dbTemplate.deleteAll(roleInfo.getCollectionName());
        dbTemplate.deleteAll(personInfo.getCollectionName());
    }

    @Test
    public void testNoDocumentDBAnnotationIT() {
        IndexingPolicy policy;

        Assert.notNull(collectionRole, "class Role Collection should not be null");
        Assert.notNull(collectionPerson, "class Person Collection should not be null");
        Assert.isTrue(!collectionPerson.equals(collectionRole), "class Role and Person is different Collection");

        policy = collectionPerson.getIndexingPolicy();

        Assert.isTrue(policy.getAutomatic() == Constants.DEFAULT_INDEXINGPOLICY_AUTOMATIC,
                "class Person collection policy should be default automatic");
        Assert.isTrue(policy.getIndexingMode() == Constants.DEFAULT_INDEXINGPOLICY_MODE,
                "class Person collection policy should be default indexing mode");

        policy = collectionRole.getIndexingPolicy();

        Assert.isTrue(policy.getAutomatic() == Constants.INDEXINGPOLICY_AUTOMATIC,
                "unmatched collection policy automatic of class Role");
        Assert.isTrue(policy.getIndexingMode() == Constants.INDEXINGPOLICY_MODE,
                "unmatched collection policy indexing mode of class Role");
    }
}

