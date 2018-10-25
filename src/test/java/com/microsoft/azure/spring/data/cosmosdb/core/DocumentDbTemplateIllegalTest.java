/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core;

import com.microsoft.azure.cosmosdb.PartitionKey;
import com.microsoft.azure.spring.data.cosmosdb.DocumentDbFactory;
import com.microsoft.azure.spring.data.cosmosdb.common.TestConstants;
import com.microsoft.azure.spring.data.cosmosdb.config.DocumentDBConfig;
import com.microsoft.azure.spring.data.cosmosdb.core.convert.MappingDocumentDbConverter;
import com.microsoft.azure.spring.data.cosmosdb.core.query.Criteria;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import com.microsoft.azure.spring.data.cosmosdb.domain.Person;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.Assert;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.microsoft.azure.spring.data.cosmosdb.core.query.CriteriaType.IS_EQUAL;

@RunWith(MockitoJUnitRunner.class)
public class DocumentDbTemplateIllegalTest {
    private static final String NULL_STR = null;
    private static final String DUMMY_COLL = "dummy";
    private static final String DUMMY_ID = "ID_1";
    private static final PartitionKey DUMMY_KEY = new PartitionKey("dummy");
    private static final String EMPTY_STR = StringUtils.EMPTY;
    private static final String WHITESPACES_STR = "  ";
    private static final String CHECK_FAILURE_MSG = "Illegal argument is not checked";

    private DocumentDbTemplate dbTemplate;
    private Class dbTemplateClass;

    @Mock
    MappingDocumentDbConverter dbConverter;

    @Before
    public void setUp() {
        final DocumentDBConfig dbConfig = DocumentDBConfig.builder("http://uri", "key", TestConstants.DB_NAME).build();
        this.dbTemplate = new DocumentDbTemplate(new DocumentDbFactory(dbConfig), dbConverter, TestConstants.DB_NAME);
        dbTemplateClass = dbTemplate.getClass();
    }

    @Test
    public void deleteIllegalShouldFail() throws NoSuchMethodException {
        final Method method = dbTemplateClass.getMethod("delete", DocumentQuery.class, String.class, Class.class,
                List.class);
        final Criteria criteria = Criteria.getInstance(IS_EQUAL, "faker", Arrays.asList("faker-value"));
        final DocumentQuery query = new DocumentQuery(criteria);

        checkIllegalArgument(method, null, DUMMY_COLL, Person.class, new ArrayList<>());
        checkIllegalArgument(method, query, DUMMY_COLL, null, new ArrayList<>());
        checkIllegalArgument(method, query, null, Person.class, new ArrayList<>());
    }

    @Test
    public void deleteIllegalCollectionShouldFail() throws NoSuchMethodException {
        final Method method = dbTemplateClass.getDeclaredMethod("deleteAll", String.class, List.class);

        checkIllegalArgument(method, NULL_STR, new ArrayList<>());
        checkIllegalArgument(method, EMPTY_STR, new ArrayList<>());
        checkIllegalArgument(method, WHITESPACES_STR, new ArrayList<>());
    }

    @Test
    public void deleteByIdIllegalArgsShouldFail() throws NoSuchMethodException {
        final Method method = dbTemplateClass.getDeclaredMethod("deleteById", String.class, Object.class,
                PartitionKey.class);

        // Test argument collectionName
        checkIllegalArgument(method, null, DUMMY_ID, DUMMY_KEY);
        checkIllegalArgument(method, EMPTY_STR, DUMMY_ID, DUMMY_KEY);
        checkIllegalArgument(method, WHITESPACES_STR, DUMMY_ID, DUMMY_KEY);

        // Test argument id
        checkIllegalArgument(method, DUMMY_COLL, null, DUMMY_KEY);
        checkIllegalArgument(method, DUMMY_COLL, EMPTY_STR, DUMMY_KEY);
        checkIllegalArgument(method, DUMMY_COLL, WHITESPACES_STR, DUMMY_KEY);
    }

    @Test
    public void findByCollIdIllegalArgsShouldFail() throws NoSuchMethodException {
        final Method method = dbTemplateClass.getDeclaredMethod("findById", String.class,
                Object.class, Class.class, PartitionKey.class);

        checkIllegalArgument(method, DUMMY_COLL, null, Person.class, null);
        checkIllegalArgument(method, DUMMY_COLL, EMPTY_STR, Person.class, null);
        checkIllegalArgument(method, DUMMY_COLL, WHITESPACES_STR, Person.class, null);
    }

    /**
     * Check IllegalArgumentException is thrown for illegal parameters
     * @param method
     * @param args Method invocation parameters
     */
    private void checkIllegalArgument(Method method, Object... args) {
        try {
            method.invoke(dbTemplate, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            Assert.isTrue(e.getCause() instanceof IllegalArgumentException, CHECK_FAILURE_MSG);
            return; // Test passed
        }

        throw new IllegalStateException(CHECK_FAILURE_MSG, null);
    }
}
