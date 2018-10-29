/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.integration;

import com.microsoft.azure.spring.data.cosmosdb.domain.Question;
import com.microsoft.azure.spring.data.cosmosdb.exception.DocumentDBAccessException;
import com.microsoft.azure.spring.data.cosmosdb.repository.TestRepositoryConfig;
import com.microsoft.azure.spring.data.cosmosdb.repository.repository.QuestionRepository;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestRepositoryConfig.class)
public class QuestionRepositoryAsyncIT {

    private static final String QUESTION_ID_0 = "question-id-0";
    private static final String QUESTION_ID_1 = "question-id-1";
    private static final String QUESTION_ID_2 = "question-id-2";
    private static final String NOT_EXIST_ID = "no-exist-id";

    private static final String QUESTION_URL_0 = "http://xxx-0.html";
    private static final String QUESTION_URL_1 = "http://xxx-1.html";
    private static final String QUESTION_URL_2 = "http://xxx-2.html";

    private static final Question QUESTION_0 = new Question(QUESTION_ID_0, QUESTION_URL_0);
    private static final Question QUESTION_1 = new Question(QUESTION_ID_1, QUESTION_URL_1);
    private static final Question QUESTION_2 = new Question(QUESTION_ID_2, QUESTION_URL_2);

    private static final List<Question> QUESTIONS = Arrays.asList(QUESTION_0, QUESTION_1, QUESTION_2);

    @Autowired
    private QuestionRepository repository;

    @Before
    public void setup() {
        this.repository.deleteAll();
    }

    @After
    public void cleanup() {
        this.repository.deleteAll();
    }

    @Test
    public void testSaveAsync() {
        final Question question = new Question("id", "link");

        Question found = this.repository.saveAsync(question).toBlocking().single();
        Assert.assertEquals(found, question);

        question.setUrl("new-link");
        found = this.repository.saveAsync(question).toBlocking().single();
        Assert.assertEquals(found, question);
    }

    @Test
    public void testSaveAll() {
        this.repository.saveAllAsync(QUESTIONS).toCompletable().await();

        Assert.assertTrue(this.repository.findById(QUESTION_0.getId()).isPresent());
        Assert.assertEquals(this.repository.findById(QUESTION_0.getId()).get(), QUESTION_0);

        Assert.assertTrue(this.repository.findById(QUESTION_1.getId()).isPresent());
        Assert.assertEquals(this.repository.findById(QUESTION_1.getId()).get(), QUESTION_1);

        Assert.assertTrue(this.repository.findById(QUESTION_2.getId()).isPresent());
        Assert.assertEquals(this.repository.findById(QUESTION_2.getId()).get(), QUESTION_2);

        this.repository.saveAllAsync(QUESTIONS).toCompletable().await();
    }

    @Test
    public void testDeleteAllIterable() {
        this.repository.saveAll(QUESTIONS);

        final List<Question> entities = Arrays.asList(QUESTION_0, QUESTION_1);

        this.repository.deleteAllAsync(entities).toCompletable().await();

        Assert.assertFalse(this.repository.findById(QUESTION_0.getId()).isPresent());
        Assert.assertTrue(this.repository.findById(QUESTION_2.getId()).isPresent());

        try {
            final Question question = new Question(NOT_EXIST_ID, "fake-url");
            this.repository.deleteAllAsync(Arrays.asList(QUESTION_0, question)).toCompletable().await();
            Assert.fail("Should not reach here.");
        } catch (RuntimeException e) {
            Assert.assertTrue(e instanceof DocumentDBAccessException);
        }
    }

    @Test
    public void testFindById() {
        this.repository.save(QUESTION_0);
        final Question question = this.repository.findByIdAsync(QUESTION_0.getId()).toBlocking().single();

        Assert.assertEquals(question, QUESTION_0);
    }

    @Test(expected = NoSuchElementException.class)
    public void testFindByIdException() {
        this.repository.findByIdAsync(NOT_EXIST_ID).toCompletable().await();
    }

    @Test
    public void testExistsById() {
        this.repository.save(QUESTION_0);

        Assert.assertTrue(this.repository.existsByIdAsync(QUESTION_0.getId()).toBlocking().single());
        Assert.assertFalse(this.repository.existsByIdAsync(NOT_EXIST_ID).toBlocking().single());
    }

    @Test
    public void testDeleteById() {
        this.repository.save(QUESTION_0);
        final Object id = this.repository.deleteByIdAsync(QUESTION_0.getId()).toBlocking().single();

        Assert.assertTrue(id instanceof String);
        Assert.assertEquals(id.toString(), QUESTION_0.getId());
        Assert.assertFalse(this.repository.existsByIdAsync(QUESTION_0.getId()).toBlocking().single());
    }

    @Test(expected = DocumentDBAccessException.class)
    public void testDeleteByIdException() {
        this.repository.deleteByIdAsync(NOT_EXIST_ID).toBlocking().single();
    }

    @Test
    public void testDeleteAll() {
        this.repository.saveAll(QUESTIONS);

        final Question found = this.repository.findByIdAsync(QUESTION_1.getId()).toBlocking().single();
        Assert.assertEquals(found, QUESTION_1);

        this.repository.deleteAllAsync().toCompletable().await();

        Assert.assertFalse(this.repository.findById(QUESTION_0.getId()).isPresent());
        Assert.assertFalse(this.repository.findById(QUESTION_2.getId()).isPresent());
    }

    @Test
    public void testDelete() {
        this.repository.saveAll(QUESTIONS);

        Assert.assertTrue(this.repository.findById(QUESTION_0.getId()).isPresent());

        this.repository.deleteAsync(QUESTION_0).toCompletable().await();

        Assert.assertFalse(this.repository.findById(QUESTION_0.getId()).isPresent());
    }

    @Test(expected = DocumentDBAccessException.class)
    public void testDeleteException() {
        this.repository.deleteAsync(QUESTION_0).toCompletable().await();
    }
}
