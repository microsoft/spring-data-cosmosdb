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

        this.repository.saveAsync(question).subscribe(
                a -> {
                    Assert.assertEquals(a, question);
                    Assert.assertTrue(this.repository.findById(question.getId()).isPresent());
                    Assert.assertEquals(this.repository.findById(question.getId()).get(), question);
                }
        );

        question.setUrl("new-link");

        this.repository.saveAsync(question).subscribe(
                a -> {
                    Assert.assertEquals(a, question);
                    Assert.assertTrue(this.repository.findById(question.getId()).isPresent());
                    Assert.assertEquals(this.repository.findById(question.getId()).get(), question);
                }
        );
    }

    @Test
    public void testFindByIdAsync() {
        this.repository.findByIdAsync(QUESTION.getId()).subscribe(a -> Assert.assertEquals(a, QUESTION));
        this.repository.findByIdAsync(NOT_EXIST_ID).subscribe(
                a -> {
                    Assert.assertTrue(false); // should not reach here.
                },
                e -> {
                    Assert.assertEquals(e.getClass(), DocumentClientException.class);
                    Assert.assertEquals(((DocumentClientException) e).getStatusCode(), HttpStatus.SC_NOT_FOUND);
                }
        );
    }

    @Test
    public void testDeleteById() {
        this.repository.deleteByIdAsync(QUESTION.getId()).subscribe(
                a -> {
                    Assert.assertTrue(a instanceof String);
                    Assert.assertEquals(a.toString(), QUESTION.getId());
                }
        );

        this.repository.deleteByIdAsync(QUESTION.getId()).subscribe(
                a -> {
                    Assert.assertTrue(false); // should not reach here.
                },
                e -> {
                    Assert.assertTrue(e instanceof DocumentClientException);
                    Assert.assertEquals(((DocumentClientException) e).getStatusCode(), HttpStatus.SC_NOT_FOUND);
                }
        );
    }

    @Test
    public void testDeleteAll() {
        final Question question = new Question("new-id", "new-url");

        this.repository.save(question);
        this.repository.findByIdAsync(question.getId()).subscribe(q -> Assert.assertEquals(question, q));

        this.repository.deleteAllAsync().toCompletable().await();

        Assert.assertFalse(this.repository.findById(QUESTION.getId()).isPresent());
        Assert.assertFalse(this.repository.findById(question.getId()).isPresent());
    }

    @Test
    public void testDelete() {
        this.repository.findByIdAsync(QUESTION.getId()).subscribe(q -> Assert.assertEquals(QUESTION, q));
        this.repository.deleteAsync(QUESTION).subscribe(q -> Assert.assertEquals(QUESTION, q));

        this.repository.findByIdAsync(QUESTION.getId()).subscribe(
                a -> {
                    Assert.assertTrue(false); // should not reach here.
                },
                e -> {
                    Assert.assertTrue(e instanceof DocumentClientException);
                    Assert.assertEquals(((DocumentClientException) e).getStatusCode(), HttpStatus.SC_NOT_FOUND);
                }
        );

        final Question question = new Question("new-id", "new-url");
        this.repository.save(question);

        this.repository.deleteAsync(QUESTION).subscribe(
                a -> {
                    Assert.assertTrue(false); // should not reach here.
                },
                e -> {
                    Assert.assertTrue(e instanceof DocumentClientException);
                    Assert.assertEquals(((DocumentClientException) e).getStatusCode(), HttpStatus.SC_NOT_FOUND);
                }
        );
    }

}
