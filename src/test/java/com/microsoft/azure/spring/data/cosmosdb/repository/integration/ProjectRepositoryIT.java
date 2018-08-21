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
import java.util.Comparator;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestRepositoryConfig.class)
public class ProjectRepositoryIT {

    private static final String ID_0 = "id-0";
    private static final String ID_1 = "id-1";
    private static final String ID_2 = "id-2";
    private static final String ID_3 = "id-3";
    private static final String ID_4 = "id-4";

    private static final String NAME_0 = "name-0";
    private static final String NAME_1 = "name-1";
    private static final String NAME_2 = "name-2";
    private static final String NAME_3 = "name-3";
    private static final String FAKE_NAME = "fake-name";

    private static final String CREATOR_0 = "creator-0";
    private static final String CREATOR_1 = "creator-1";
    private static final String CREATOR_2 = "creator-2";
    private static final String CREATOR_3 = "creator-3";
    private static final String FAKE_CREATOR = "fake-creator";

    private static final Long STAR_COUNT_0 = 0L;
    private static final Long STAR_COUNT_1 = 1L;
    private static final Long STAR_COUNT_2 = 2L;
    private static final Long STAR_COUNT_3 = 3L;

    private static final Long FORK_COUNT_0 = 0L;
    private static final Long FORK_COUNT_1 = 1L;
    private static final Long FORK_COUNT_2 = 2L;
    private static final Long FORK_COUNT_3 = 3L;
    private static final Long FAKE_COUNT = 123234L;

    private static final Project PROJECT_0 = new Project(ID_0, NAME_0, CREATOR_0, true, STAR_COUNT_0, FORK_COUNT_0);
    private static final Project PROJECT_1 = new Project(ID_1, NAME_1, CREATOR_1, true, STAR_COUNT_1, FORK_COUNT_1);
    private static final Project PROJECT_2 = new Project(ID_2, NAME_2, CREATOR_2, true, STAR_COUNT_2, FORK_COUNT_2);
    private static final Project PROJECT_3 = new Project(ID_3, NAME_3, CREATOR_3, true, STAR_COUNT_3, FORK_COUNT_3);
    private static final Project PROJECT_4 = new Project(ID_4, NAME_0, CREATOR_0, false, STAR_COUNT_0, FORK_COUNT_0);

    private static final List<Project> PROJECTS = Arrays.asList(PROJECT_0, PROJECT_1, PROJECT_2, PROJECT_3, PROJECT_4);

    @Autowired
    private ProjectRepository repository;

    @Before
    public void setup() {
        this.repository.saveAll(PROJECTS);
    }

    @After
    public void cleanup() {
        this.repository.deleteAll();
    }

    @Test
    public void testFindByWithAnd() {
        List<Project> projects = this.repository.findByNameAndStarCount(NAME_1, STAR_COUNT_1);

        Assert.assertEquals(projects.size(), 1);
        Assert.assertEquals(projects.get(0), PROJECT_1);

        projects = this.repository.findByNameAndStarCount(NAME_0, STAR_COUNT_1);

        Assert.assertTrue(projects.isEmpty());

        projects = this.repository.findByNameAndStarCount(NAME_0, STAR_COUNT_0);
        final List<Project> reference = Arrays.asList(PROJECT_0, PROJECT_4);

        projects.sort(Comparator.comparing(Project::getId));
        reference.sort(Comparator.comparing(Project::getId));

        Assert.assertEquals(projects.size(), reference.size());
        Assert.assertEquals(projects, reference);
    }

    @Test
    public void testFindByWithOr() {
        List<Project> projects = this.repository.findByNameOrForkCount(NAME_2, STAR_COUNT_2);

        Assert.assertEquals(projects.size(), 1);
        Assert.assertEquals(projects.get(0), PROJECT_2);

        projects = this.repository.findByNameOrForkCount(FAKE_NAME, FAKE_COUNT);

        Assert.assertTrue(projects.isEmpty());

        projects = this.repository.findByNameOrForkCount(NAME_0, FORK_COUNT_1);
        final List<Project> reference = Arrays.asList(PROJECT_0, PROJECT_1, PROJECT_4);

        projects.sort(Comparator.comparing(Project::getId));
        reference.sort(Comparator.comparing(Project::getId));

        Assert.assertEquals(projects.size(), reference.size());
        Assert.assertEquals(projects, reference);
    }

    @Test
    public void testFindByWithAndPartition() {
        List<Project> projects = this.repository.findByNameAndCreator(NAME_1, CREATOR_1);

        Assert.assertEquals(projects.size(), 1);
        Assert.assertEquals(projects.get(0), PROJECT_1);

        projects = this.repository.findByNameAndCreator(NAME_0, CREATOR_1);

        Assert.assertTrue(projects.isEmpty());

        projects = this.repository.findByNameAndCreator(NAME_0, CREATOR_0);
        final List<Project> reference = Arrays.asList(PROJECT_0, PROJECT_4);

        projects.sort(Comparator.comparing(Project::getId));
        reference.sort(Comparator.comparing(Project::getId));

        Assert.assertEquals(projects.size(), reference.size());
        Assert.assertEquals(projects, reference);
    }

    @Test
    public void testFindByWithOrPartition() {
        List<Project> projects = this.repository.findByNameOrCreator(NAME_2, CREATOR_2);

        Assert.assertEquals(projects.size(), 1);
        Assert.assertEquals(projects.get(0), PROJECT_2);

        projects = this.repository.findByNameOrCreator(FAKE_NAME, FAKE_CREATOR);

        Assert.assertTrue(projects.isEmpty());

        projects = this.repository.findByNameOrCreator(NAME_0, CREATOR_1);
        final List<Project> reference = Arrays.asList(PROJECT_0, PROJECT_1, PROJECT_4);

        projects.sort(Comparator.comparing(Project::getId));
        reference.sort(Comparator.comparing(Project::getId));

        Assert.assertEquals(projects.size(), reference.size());
        Assert.assertEquals(projects, reference);
    }

    @Test
    public void testFindByWithAndOr() {
        List<Project> projects = repository.findByNameAndCreatorOrForkCount(NAME_0, CREATOR_1, FORK_COUNT_2);
        List<Project> reference = Arrays.asList(PROJECT_2);

        Assert.assertEquals(projects, reference);

        projects = repository.findByNameAndCreatorOrForkCount(NAME_1, CREATOR_1, FORK_COUNT_2);
        reference = Arrays.asList(PROJECT_1, PROJECT_2);

        projects.sort(Comparator.comparing(Project::getId));
        reference.sort(Comparator.comparing(Project::getId));

        Assert.assertEquals(projects, reference);

        projects = repository.findByNameAndCreatorOrForkCount(NAME_1, CREATOR_2, FAKE_COUNT);

        Assert.assertEquals(projects.size(), 0);
    }

    @Test
    public void testFindByWithOrAnd() {
        List<Project> projects = repository.findByNameOrCreatorAndForkCount(NAME_0, CREATOR_1, FORK_COUNT_2);
        List<Project> reference = Arrays.asList(PROJECT_0, PROJECT_4);

        projects.sort(Comparator.comparing(Project::getId));
        reference.sort(Comparator.comparing(Project::getId));

        Assert.assertEquals(projects, reference);

        projects = repository.findByNameOrCreatorAndForkCount(NAME_1, CREATOR_2, FORK_COUNT_2);
        reference = Arrays.asList(PROJECT_1, PROJECT_2);

        projects.sort(Comparator.comparing(Project::getId));
        reference.sort(Comparator.comparing(Project::getId));

        Assert.assertEquals(projects, reference);

        projects = repository.findByNameOrCreatorAndForkCount(FAKE_NAME, CREATOR_1, FORK_COUNT_2);

        Assert.assertEquals(projects.size(), 0);
    }

    @Test
    public void testFindByWithOrOr() {
        List<Project> projects = repository.findByNameOrCreatorOrForkCount(NAME_0, CREATOR_1, FORK_COUNT_2);
        final List<Project> reference = Arrays.asList(PROJECT_0, PROJECT_1, PROJECT_2, PROJECT_4);

        projects.sort(Comparator.comparing(Project::getId));
        reference.sort(Comparator.comparing(Project::getId));

        Assert.assertEquals(projects, reference);

        projects = repository.findByNameOrCreatorOrForkCount(FAKE_NAME, FAKE_CREATOR, FAKE_COUNT);

        Assert.assertEquals(projects.size(), 0);
    }


    @Test
    public void testFindByWithOrAndOr() {
        List<Project> projects = repository.findByNameOrCreatorAndForkCountOrStarCount(NAME_1, CREATOR_0,
                FORK_COUNT_2, STAR_COUNT_3);
        List<Project> reference = Arrays.asList(PROJECT_1, PROJECT_3);

        projects.sort(Comparator.comparing(Project::getId));
        reference.sort(Comparator.comparing(Project::getId));

        Assert.assertEquals(projects, reference);

        projects = repository.findByNameOrCreatorAndForkCountOrStarCount(NAME_1, CREATOR_0,
                FORK_COUNT_0, STAR_COUNT_3);
        reference = Arrays.asList(PROJECT_0, PROJECT_1, PROJECT_3, PROJECT_4);

        projects.sort(Comparator.comparing(Project::getId));
        reference.sort(Comparator.comparing(Project::getId));

        Assert.assertEquals(projects, reference);

        projects = repository.findByNameOrCreatorAndForkCountOrStarCount(FAKE_NAME, CREATOR_1,
                FORK_COUNT_0, FAKE_COUNT);

        Assert.assertEquals(projects.size(), 0);
    }

    @Test
    public void testFindByGreaterThan() {
        List<Project> projects = repository.findByForkCountGreaterThan(FORK_COUNT_1);
        final List<Project> reference = Arrays.asList(PROJECT_2, PROJECT_3);

        projects.sort(Comparator.comparing(Project::getId));
        reference.sort(Comparator.comparing(Project::getId));

        Assert.assertEquals(projects.size(), reference.size());
        Assert.assertEquals(projects, reference);

        projects = repository.findByForkCountGreaterThan(FAKE_COUNT);

        Assert.assertEquals(projects.size(), 0);
    }

    @Test
    public void testFindByGreaterThanWithAndOr() {
        List<Project> projects = repository.findByCreatorAndForkCountGreaterThan(CREATOR_2, FORK_COUNT_1);

        Assert.assertEquals(projects.get(0), PROJECT_2);

        projects = repository.findByCreatorAndForkCountGreaterThan(CREATOR_0, FORK_COUNT_1);

        Assert.assertEquals(projects.size(), 0);

        projects = repository.findByCreatorOrForkCountGreaterThan(CREATOR_0, FORK_COUNT_2);
        final List<Project> reference = Arrays.asList(PROJECT_0, PROJECT_3, PROJECT_4);

        projects.sort(Comparator.comparing(Project::getId));
        reference.sort(Comparator.comparing(Project::getId));

        Assert.assertEquals(projects.size(), reference.size());
        Assert.assertEquals(projects, reference);
    }
}
