/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.integration;

import com.microsoft.azure.spring.data.cosmosdb.domain.Project;
import com.microsoft.azure.spring.data.cosmosdb.repository.TestRepositoryConfig;
import com.microsoft.azure.spring.data.cosmosdb.repository.repository.ProjectRepository;
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

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestRepositoryConfig.class)
public class ProjectRepositoryIT {

    private static final String ID_0 = "id-0";
    private static final String ID_1 = "id-1";
    private static final String ID_2 = "id-2";
    private static final String ID_3 = "id-3";

    private static final String NAME_0 = "name-0";
    private static final String NAME_1 = "name-1";
    private static final String NAME_2 = "name-2";
    private static final String NAME_3 = "name-3";

    private static final String CREATOR_0 = "creator-0";
    private static final String CREATOR_1 = "creator-1";
    private static final String CREATOR_2 = "creator-2";
    private static final String CREATOR_3 = "creator-3";

    private static final Long STAR_COUNT_0 = 0L;
    private static final Long STAR_COUNT_1 = 1L;
    private static final Long STAR_COUNT_2 = 2L;
    private static final Long STAR_COUNT_3 = 3L;

    private static final Long FORK_COUNT_0 = 0L;
    private static final Long FORK_COUNT_1 = 1L;
    private static final Long FORK_COUNT_2 = 2L;
    private static final Long FORK_COUNT_3 = 3L;

    private static final Project PROJECT_0 = new Project(ID_0, NAME_0, CREATOR_0, true, STAR_COUNT_0, FORK_COUNT_0);
    private static final Project PROJECT_1 = new Project(ID_1, NAME_1, CREATOR_1, true, STAR_COUNT_1, FORK_COUNT_1);
    private static final Project PROJECT_2 = new Project(ID_2, NAME_2, CREATOR_2, true, STAR_COUNT_2, FORK_COUNT_2);
    private static final Project PROJECT_3 = new Project(ID_3, NAME_3, CREATOR_3, true, STAR_COUNT_3, FORK_COUNT_3);

    @Autowired
    private ProjectRepository repository;

    @Before
    public void setup() {
        final List<Project> projects = Arrays.asList(PROJECT_0, PROJECT_1, PROJECT_2, PROJECT_3);

        this.repository.saveAll(projects);
    }

    @After
    public void cleanup() {
        this.repository.deleteAll();
    }

    @Test
    public void testFindByWithAnd() {
        List<Project> projects = this.repository.findByNameAndCreator(NAME_0, CREATOR_0);

        Assert.assertEquals(projects.size(), 1);
        Assert.assertEquals(projects.get(0).getName(), NAME_0);
        Assert.assertEquals(projects.get(0).getCreator(), CREATOR_0);

        projects = this.repository.findByNameAndCreator(NAME_0, CREATOR_1);

        Assert.assertTrue(projects.isEmpty());
    }

    @Test
    public void testFindByWithOr() {
        List<Project> projects = this.repository.findByNameOrCreator(NAME_0, CREATOR_0);

        Assert.assertEquals(projects.size(), 1);
        Assert.assertEquals(projects.get(0).getName(), NAME_0);
        Assert.assertEquals(projects.get(0).getCreator(), CREATOR_0);

        projects = this.repository.findByNameAndCreator(NAME_0, CREATOR_1);

        Assert.assertEquals(projects.size(), 2);
    }
}
