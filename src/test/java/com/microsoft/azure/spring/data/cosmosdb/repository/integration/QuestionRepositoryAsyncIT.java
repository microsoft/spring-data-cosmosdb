/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.integration;

import com.microsoft.azure.spring.data.cosmosdb.domain.Question;
import com.microsoft.azure.spring.data.cosmosdb.repository.TestRepositoryConfig;
import com.microsoft.azure.spring.data.cosmosdb.repository.repository.QuestionRepository;
import lombok.NonNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

    private void assertQuestionListEquals(@NonNull List<Question> found, @NonNull List<Question> expected) {
        Assert.assertEquals(found.size(), expected.size());

        found.sort(Comparator.comparing(Question::getId));
        expected.sort(Comparator.comparing(Question::getId));

        Assert.assertEquals(found, expected);
    }

    @Test
    public void testSave() {
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
        List<Question> found = this.repository.deleteAllAsync(entities).toList().toBlocking().single();

        assertQuestionListEquals(entities, found);
        Assert.assertFalse(this.repository.findById(QUESTION_0.getId()).isPresent());
        Assert.assertTrue(this.repository.findById(QUESTION_2.getId()).isPresent());

        this.repository.saveAll(QUESTIONS);
        final Question question = new Question(NOT_EXIST_ID, "fake-url");
        found = this.repository.deleteAllAsync(Arrays.asList(QUESTION_0, question)).toList().toBlocking().single();

        assertQuestionListEquals(Collections.singletonList(QUESTION_0), found);

        this.repository.deleteAll();
        found = this.repository.deleteAllAsync(Arrays.asList(QUESTION_0, question)).toList().toBlocking().single();

        Assert.assertTrue(found.isEmpty());
    }

    @Test
    public void testFindAllById() {
        this.repository.saveAll(QUESTIONS);
        List<String> ids = Arrays.asList(QUESTION_ID_0, QUESTION_ID_2);

        List<Question> found = this.repository.findAllByIdAsync(ids).toList().toBlocking().single();
        assertQuestionListEquals(found, Arrays.asList(QUESTION_0, QUESTION_2));

        ids = Arrays.asList(QUESTION_ID_0, NOT_EXIST_ID);
        found = this.repository.findAllByIdAsync(ids).toList().toBlocking().single();
        assertQuestionListEquals(found, Collections.singletonList(QUESTION_0));

        ids = Collections.singletonList(NOT_EXIST_ID);
        found = this.repository.findAllByIdAsync(ids).toList().toBlocking().single();
        Assert.assertTrue(found.isEmpty());
    }

    @Test
    public void testFindById() {
        this.repository.save(QUESTION_0);
        final Question question = this.repository.findByIdAsync(QUESTION_0.getId()).toBlocking().single();

        Assert.assertEquals(question, QUESTION_0);
        Assert.assertTrue(this.repository.findByIdAsync(NOT_EXIST_ID).isEmpty().toBlocking().single());
        Assert.assertTrue(this.repository.findByIdAsync("").isEmpty().toBlocking().single());
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
        Assert.assertTrue(this.repository.deleteByIdAsync(NOT_EXIST_ID).isEmpty().toBlocking().single());
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
    public void testFindAllSort() {
        this.repository.saveAll(QUESTIONS);

        final Sort sort = new Sort(Sort.Direction.ASC, "url");
        final List<Question> questions = this.repository.findAllAsync(sort).toList().toBlocking().single();

        Assert.assertTrue(questions.isEmpty()); // Question Collection need indexing config for sorting.
    }

    @Test
    public void testDelete() {
        this.repository.saveAll(QUESTIONS);
        Assert.assertTrue(this.repository.findById(QUESTION_0.getId()).isPresent());

        this.repository.deleteAsync(QUESTION_0).toCompletable().await();
        Assert.assertFalse(this.repository.findById(QUESTION_0.getId()).isPresent());

        Assert.assertTrue(this.repository.deleteAsync(QUESTION_0).isEmpty().toBlocking().single());
    }

    @Test
    public void testFindAll() {
        this.repository.saveAll(QUESTIONS);

        final List<Question> found = this.repository.findAllAsync().toList().toBlocking().single();
        assertQuestionListEquals(found, QUESTIONS);

        this.repository.deleteAll();
        Assert.assertTrue(this.repository.findAllAsync().toList().toBlocking().single().isEmpty());
    }

    @Test
    public void testCount() {
        this.repository.saveAll(QUESTIONS);

        Assert.assertEquals(Long.valueOf(QUESTIONS.size()), this.repository.countAllAsync().toBlocking().single());

        this.repository.deleteAll();

        Assert.assertEquals(Long.valueOf(0), this.repository.countAllAsync().toBlocking().single());
    }
}
