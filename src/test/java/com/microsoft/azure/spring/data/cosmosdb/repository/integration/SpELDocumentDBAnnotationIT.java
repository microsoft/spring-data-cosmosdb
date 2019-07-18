/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.documentdb.DocumentCollection;
import com.microsoft.azure.spring.data.cosmosdb.DocumentDbFactory;
import com.microsoft.azure.spring.data.cosmosdb.common.TestConstants;
import com.microsoft.azure.spring.data.cosmosdb.config.DocumentDBConfig;
import com.microsoft.azure.spring.data.cosmosdb.core.DocumentDbTemplate;
import com.microsoft.azure.spring.data.cosmosdb.core.convert.MappingDocumentDbConverter;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.DocumentDbMappingContext;
import com.microsoft.azure.spring.data.cosmosdb.domain.SpELBeanStudent;
import com.microsoft.azure.spring.data.cosmosdb.domain.SpELPropertyStudent;
import com.microsoft.azure.spring.data.cosmosdb.repository.TestRepositoryConfig;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.DocumentDbEntityInformation;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScanner;
import org.springframework.context.ApplicationContext;
import org.springframework.data.annotation.Persistent;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * 
 * @author Domenico Sibilio
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestRepositoryConfig.class)
public class SpELDocumentDBAnnotationIT {
    private static final SpELPropertyStudent TEST_PROPERTY_STUDENT = 
            new SpELPropertyStudent(TestConstants.ID_1, TestConstants.FIRST_NAME,
            TestConstants.LAST_NAME);

    @Value("${cosmosdb.uri}")
    private String dbUri;

    @Value("${cosmosdb.key}")
    private String dbKey;

    @Autowired
    private ApplicationContext applicationContext;

    private DocumentDbTemplate dbTemplate;
    private DocumentDbEntityInformation<SpELPropertyStudent, String> documentDbEntityInfo;

    @After
    public void cleanUp() {
        if (dbTemplate != null && documentDbEntityInfo != null) {
            dbTemplate.deleteCollection(documentDbEntityInfo.getCollectionName());
        }
    }
    
    @Test
    public void testDynamicCollectionNameWithPropertySourceExpression() {
        final DocumentDbEntityInformation<SpELPropertyStudent, Object> propertyStudentInfo =
                new DocumentDbEntityInformation<>(SpELPropertyStudent.class);
        
        assertEquals(TestConstants.DYNAMIC_PROPERTY_COLLECTION_NAME, propertyStudentInfo.getCollectionName());
    }
    
    @Test
    public void testDynamicCollectionNameWithBeanExpression() {
        final DocumentDbEntityInformation<SpELBeanStudent, Object> beanStudentInfo =
                new DocumentDbEntityInformation<>(SpELBeanStudent.class);
        
        assertEquals(TestConstants.DYNAMIC_BEAN_COLLECTION_NAME, beanStudentInfo.getCollectionName());
    }
    
    @Test
    public void testDatabaseOperationsOnDynamicallyNamedCollection() throws ClassNotFoundException {
      final DocumentDBConfig dbConfig = DocumentDBConfig.builder(dbUri, dbKey, TestConstants.DB_NAME).build();
      final DocumentDbFactory dbFactory = new DocumentDbFactory(dbConfig);

      documentDbEntityInfo = new DocumentDbEntityInformation<>(SpELPropertyStudent.class);
      final DocumentDbMappingContext dbContext = new DocumentDbMappingContext();
      dbContext.setInitialEntitySet(new EntityScanner(this.applicationContext).scan(Persistent.class));

      final ObjectMapper objectMapper = new ObjectMapper();
      final MappingDocumentDbConverter mappingConverter = new MappingDocumentDbConverter(dbContext, objectMapper);
      dbTemplate = new DocumentDbTemplate(dbFactory, mappingConverter, TestConstants.DB_NAME);
      
      final DocumentCollection collection = dbTemplate.createCollectionIfNotExists(documentDbEntityInfo);

      final SpELPropertyStudent insertedRecord = 
              dbTemplate.insert(documentDbEntityInfo.getCollectionName(), TEST_PROPERTY_STUDENT, null);
      assertNotNull(insertedRecord);
      
      final SpELPropertyStudent readRecord = 
              dbTemplate.findById(TestConstants.DYNAMIC_PROPERTY_COLLECTION_NAME, 
                      insertedRecord.getId(), SpELPropertyStudent.class);
      assertNotNull(readRecord);
    }

}

