/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.Assert;

import java.util.List;

@SpringBootApplication
public class SampleApplication implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SampleApplication.class);

    @Autowired
    private UserRepository repository;

    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }

    public void run(String... var1) {

        final User testUser = new User("test@test.com", "testFirstName", "testLastName");

        repository.deleteAll();
        repository.save(testUser);

        final List<User> results = repository.findByEmailAddressAndLastName(testUser.getEmailAddress(), testUser.getLastName());
        Assert.isTrue(results.size() == 1, "Result size should be 1");

        final User result1 = results.get(0);
        Assert.state(result1.getFirstName().equals(testUser.getFirstName()), "query result firstName doesn't match!");
        Assert.state(result1.getLastName().equals(testUser.getLastName()), "query result lastName doesn't match!");

        LOGGER.info("findByEmailAddressAndLastName in User collection get result: {}", result1.toString());
        System.out.println("findByEmailAddressAndLastName in User collection get result:" + result1.toString());

        final List<User> result2 = repository.findByFirstName(testUser.getFirstName());

        Assert.state(result2.get(0).getFirstName().equals(testUser.getFirstName()),
                "query result firstName doesn't match!");
        Assert.state(result2.get(0).getLastName().equals(testUser.getLastName()),
                "query result lastName doesn't match!");

        LOGGER.info("findByFirstName in User collection get result: {}", result2.get(0).toString());
        System.out.println("findByFirstName in User collection get result:" + result2.get(0).toString());

        final List<User> result3 = repository.findByLastName(testUser.getLastName());

        Assert.state(result3.get(0).getFirstName().equals(testUser.getFirstName()),
                "query result firstName doesn't match!");
        Assert.state(result3.get(0).getLastName().equals(testUser.getLastName()),
                "query result lastName doesn't match!");

        LOGGER.info("findByFirstName in User collection get result: {}", result3.get(0).toString());
        System.out.println("findByFirstName in User collection get result:" + result3.get(0).toString());
    }
}
