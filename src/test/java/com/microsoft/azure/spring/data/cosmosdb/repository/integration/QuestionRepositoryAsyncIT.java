/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.integration;

import com.microsoft.azure.cosmosdb.DocumentClientException;
import com.microsoft.azure.spring.data.cosmosdb.domain.Question;
import com.microsoft.azure.spring.data.cosmosdb.repository.TestRepositoryConfig;
import com.microsoft.azure.spring.data.cosmosdb.repository.repository.QuestionRepository;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestRepositoryConfig.class)
public class QuestionRepositoryAsyncIT {

    private static final String QUESTION_ID = "question-id";

    private static final String NOT_EXIST_ID = "no-exist-id";

    private static final String QUESTION_URL = "http://xxx.html";

    private static final Question QUESTION = new Question(QUESTION_ID, QUESTION_URL);

    @Autowired
    private QuestionRepository repository;

    @Before
    public void setup() {
        this.repository.save(QUESTION);
    }

    @After
    public void cleanup() {
        this.repository.deleteAll();
    }

    @Test
    public void testSaveAsync() {
        this.repository.deleteAll();
        final Question question = new Question("id", "link");

        Question found = this.repository.saveAsync(question).toBlocking().single();
        Assert.assertEquals(found, question);

        question.setUrl("new-link");
        found = this.repository.saveAsync(question).toBlocking().single();
        Assert.assertEquals(found, question);
    }

    @Test
    public void testFindByIdAsync() {
        final Question question = this.repository.findByIdAsync(QUESTION.getId()).toBlocking().single();
        Assert.assertEquals(question, QUESTION);

        try {
            this.repository.findByIdAsync(NOT_EXIST_ID).toCompletable().await();
            Assert.fail("Should trigger RuntimeException.");
        } catch (RuntimeException e) {
            final Throwable cause = e.getCause();
            Assert.assertTrue(cause instanceof DocumentClientException);
            Assert.assertEquals(((DocumentClientException) cause).getStatusCode(), HttpStatus.SC_NOT_FOUND);
        }
    }

    @Test
    public void testDeleteById() {
        final Object id = this.repository.deleteByIdAsync(QUESTION.getId()).toBlocking().single();
        Assert.assertTrue(id instanceof String);
        Assert.assertEquals(id.toString(), QUESTION.getId());

        try {
            this.repository.deleteByIdAsync(NOT_EXIST_ID).toBlocking().single();
            Assert.fail("Should trigger RuntimeException.");
        } catch (RuntimeException e) {
            final Throwable cause = e.getCause();
            Assert.assertTrue(cause instanceof DocumentClientException);
            Assert.assertEquals(((DocumentClientException) cause).getStatusCode(), HttpStatus.SC_NOT_FOUND);
        }
    }

    @Test
    public void testDeleteAll() {
        final Question question = new Question("new-id", "new-url");
        this.repository.save(question);

        final Question found = this.repository.findByIdAsync(question.getId()).toBlocking().single();
        Assert.assertEquals(found, question);

        this.repository.deleteAllAsync().toCompletable().await();

        Assert.assertFalse(this.repository.findById(QUESTION.getId()).isPresent());
        Assert.assertFalse(this.repository.findById(question.getId()).isPresent());
    }

    @Test
    public void testDelete() {
        this.repository.findByIdAsync(QUESTION.getId()).toCompletable().await();
        this.repository.deleteAsync(QUESTION).toCompletable().await();

        Assert.assertFalse(this.repository.findById(QUESTION.getId()).isPresent());

        try {
            this.repository.deleteAsync(QUESTION).toCompletable().await();
            Assert.fail("Should trigger RuntimeException.");
        } catch (RuntimeException e) {
            final Throwable cause = e.getCause();
            Assert.assertTrue(cause instanceof DocumentClientException);
            Assert.assertEquals(((DocumentClientException) cause).getStatusCode(), HttpStatus.SC_NOT_FOUND);
        }
    }
}
