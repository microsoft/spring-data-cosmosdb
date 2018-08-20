/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.integration;

import com.google.common.collect.Lists;
import com.microsoft.azure.spring.data.cosmosdb.domain.Project;
import com.microsoft.azure.spring.data.cosmosdb.exception.IllegalQueryException;
import com.microsoft.azure.spring.data.cosmosdb.repository.TestRepositoryConfig;
import com.microsoft.azure.spring.data.cosmosdb.repository.repository.ProjectRepository;
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
import java.util.Comparator;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestRepositoryConfig.class)
public class ProjectRepositorySortIT {

    private static final String ID_0 = "id-0";
    private static final String ID_1 = "id-1";
    private static final String ID_2 = "id-2";
    private static final String ID_3 = "id-3";
    private static final String ID_4 = "id-4";

    private static final String NAME_0 = "name-0";
    private static final String NAME_1 = "name-1";
    private static final String NAME_2 = "name-2";
    private static final String NAME_3 = "NAME-3";
    private static final String NAME_4 = "name-4";

    private static final String CREATOR_0 = "creator-0";
    private static final String CREATOR_1 = "creator-1";
    private static final String CREATOR_2 = "creator-2";
    private static final String CREATOR_3 = "creator-3";
    private static final String CREATOR_4 = "creator-4";

    private static final Long STAR_COUNT_0 = 0L;
    private static final Long STAR_COUNT_1 = 1L;
    private static final Long STAR_COUNT_2 = 2L;
    private static final Long STAR_COUNT_3 = 3L;
    private static final Long STAR_COUNT_4 = 4L;

    private static final Long FORK_COUNT_0 = 0L;
    private static final Long FORK_COUNT_1 = 1L;
    private static final Long FORK_COUNT_2 = 2L;
    private static final Long FORK_COUNT_3 = 3L;
    private static final Long FORK_COUNT_4 = FORK_COUNT_3;

    private static final Project PROJECT_0 = new Project(ID_0, NAME_0, CREATOR_0, true, STAR_COUNT_0, FORK_COUNT_0);
    private static final Project PROJECT_1 = new Project(ID_1, NAME_1, CREATOR_1, true, STAR_COUNT_1, FORK_COUNT_1);
    private static final Project PROJECT_2 = new Project(ID_2, NAME_2, CREATOR_2, true, STAR_COUNT_2, FORK_COUNT_2);
    private static final Project PROJECT_3 = new Project(ID_3, NAME_3, CREATOR_3, true, STAR_COUNT_3, FORK_COUNT_3);
    private static final Project PROJECT_4 = new Project(ID_4, NAME_4, CREATOR_4, true, STAR_COUNT_4, FORK_COUNT_4);

    private static final List<Project> PROJECTS = Arrays.asList(PROJECT_4, PROJECT_3, PROJECT_2, PROJECT_1, PROJECT_0);

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
    public void testFindAllSortASC() {
        final Sort sort = new Sort(Sort.Direction.ASC, "starCount");
        final List<Project> projects = Lists.newArrayList(this.repository.findAll(sort));

        PROJECTS.sort(Comparator.comparing(Project::getStarCount));

        Assert.assertEquals(projects.size(), PROJECTS.size());
        Assert.assertEquals(projects, PROJECTS);
    }

    @Test
    public void testFindAllSortDESC() {
        final Sort sort = new Sort(Sort.Direction.DESC, "creator");
        final List<Project> projects = Lists.newArrayList(this.repository.findAll(sort));

        PROJECTS.sort(Comparator.comparing(Project::getCreator).reversed());

        Assert.assertEquals(projects.size(), PROJECTS.size());
        Assert.assertEquals(projects, PROJECTS);
    }

    @Test
    public void testFindAllUnSorted() {
        final Sort sort = Sort.unsorted();
        final List<Project> projects = Lists.newArrayList(this.repository.findAll(sort));

        PROJECTS.sort(Comparator.comparing(Project::getId));
        projects.sort(Comparator.comparing(Project::getId));

        Assert.assertEquals(projects.size(), PROJECTS.size());
        Assert.assertEquals(projects, PROJECTS);
    }

    @Test(expected = IllegalQueryException.class)
    public void testFindAllSortMoreThanOneOrderException() {
        final Sort sort = new Sort(Sort.Direction.ASC, "name", "creator");

        this.repository.findAll(sort);
    }

    @Test(expected = IllegalQueryException.class)
    public void testFindAllSortIgnoreCaseException() {
        final Sort.Order order = Sort.Order.by("name").ignoreCase();
        final Sort sort = Sort.by(order);

        this.repository.findAll(sort);
    }

    @Test(expected = IllegalQueryException.class)
    public void testFindAllSortMissMatchException() {
        final Sort sort = new Sort(Sort.Direction.ASC, "fake-name");

        this.repository.findAll(sort);
    }

    @Test(expected = IllegalQueryException.class)
    public void testFindAllSortWithIdName() {
        final Sort sort = new Sort(Sort.Direction.ASC, "id");

        this.repository.findAll(sort);
    }

    @Test
    public void testFindSortWithOr() {
        final Sort sort = new Sort(Sort.Direction.ASC, "starCount");
        final List<Project> projects = Lists.newArrayList(this.repository.findByNameOrCreator(NAME_0, CREATOR_3, sort));
        final List<Project> references = Arrays.asList(PROJECT_0, PROJECT_3);

        references.sort(Comparator.comparing(Project::getStarCount));

        Assert.assertEquals(projects.size(), references.size());
        Assert.assertEquals(projects, references);
    }

    @Test
    public void testFindSortWithAnd() {
        final Sort sort = new Sort(Sort.Direction.ASC, "forkCount");
        final List<Project> projects = Lists.newArrayList(repository.findByNameAndCreator(NAME_0, CREATOR_0, sort));
        final List<Project> references = Arrays.asList(PROJECT_0);

        references.sort(Comparator.comparing(Project::getStarCount));

        Assert.assertEquals(projects.size(), references.size());
        Assert.assertEquals(projects, references);
    }

    @Test
    public void testFindSortWithEqual() {
        final Sort sort = new Sort(Sort.Direction.DESC, "name");
        final List<Project> projects = Lists.newArrayList(this.repository.findByForkCount(FORK_COUNT_3, sort));
        final List<Project> references = Arrays.asList(PROJECT_3, PROJECT_4);

        references.sort(Comparator.comparing(Project::getName).reversed());

        Assert.assertEquals(projects.size(), references.size());
        Assert.assertEquals(projects, references);
    }
}

