/*
 * Copyright 2015-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package example.springdata.cosmosdb;

import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentDbPageRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {UserRepositoryConfiguration.class})
public class UserRepositoryIntegrationTest {

    private static final String ID = "0123456789";
    private static final String EMAIL = "xxx-xx@xxx.com";
    private static final String NAME = "myName";
    private static final String POSTAL_CODE = "0123456789";
    private static final String STREET = "zixing road";
    private static final String CITY = "shanghai";
    private static final String ROLE_CREATOR = "creator";
    private static final String ROLE_CONTRIBUTOR = "contributor";
    private static final int COST_CREATOR = 234;
    private static final int COST_CONTRIBUTOR = 666;
    private static final Long COUNT = 123L;

    @Autowired
    private UserRepository repository;

    @Before
    public void setup() {
        this.repository.deleteAll();
    }

    @After
    public void cleanup() {
        this.repository.deleteAll();
    }

    @Test
    public void testUserRepository() {
        final Address address = new Address(POSTAL_CODE, STREET, CITY);
        final Role creator = new Role(ROLE_CREATOR, COST_CREATOR);
        final Role contributor = new Role(ROLE_CONTRIBUTOR, COST_CONTRIBUTOR);
        final User user = new User(ID, EMAIL, NAME, COUNT, address, Arrays.asList(creator, contributor));

        this.repository.save(user);

        // Test for findById
        User result = this.repository.findById(ID).get();
        Assert.notNull(result, "should be exist in database");
        Assert.isTrue(result.getId().equals(ID), "should be the same id");

        // Test for findByName
		List<User> resultList = this.repository.findByName(user.getName());
		Assert.isTrue(resultList.size() == 1, "should be only one user here");
		Assert.isTrue(resultList.get(0).getName().equals(user.getName()), "should be same Name");
		Assert.notNull(result.getRoleList(), "roleList should not be null");
        Assert.isTrue(result.getRoleList().size() == user.getRoleList().size(), "must be the same list size");

        for (int i = 0; i < user.getRoleList().size(); i++) {
            final Role role = result.getRoleList().get(i);
            final Role roleReference = user.getRoleList().get(i);

            Assert.isTrue(role.getName().equals(roleReference.getName()), "should be the same role name");
            Assert.isTrue(role.getCost() == roleReference.getCost(), "should be the same role cost");
        }

        // Test for findByEmailAndAddress
        resultList = this.repository.findByEmailAndAddress(user.getEmail(), user.getAddress());
        Assert.isTrue(resultList.size() == 1, "should be only one user here");

        result = resultList.get(0);
        Assert.isTrue(result.getEmail().equals(user.getEmail()), "should be same Email");
        Assert.isTrue(result.getAddress().getPostalCode().equals(user.getAddress().getPostalCode()),
                "should be same postalCode");
        Assert.isTrue(result.getAddress().getCity().equals(user.getAddress().getCity()), "should be same City");
        Assert.isTrue(result.getAddress().getStreet().equals(user.getAddress().getStreet()), "should be same street");

        resultList = this.repository.findByEmailOrName(user.getEmail(), user.getName());
        result = resultList.get(0);
        Assert.isTrue(result.getId().equals(user.getId()), "should be the same Id");

        resultList = this.repository.findByCount(COUNT, Sort.by(new Sort.Order(Sort.Direction.ASC, "count")));
        result = resultList.get(0);
        Assert.isTrue(result.getId().equals(user.getId()), "should be the same Id");

        resultList = this.repository.findByNameIn(Arrays.asList(user.getName(), "fake-name"));
        result = resultList.get(0);
        Assert.isTrue(result.getId().equals(user.getId()), "should be the same Id");

        // Test for findByAddress
        final Pageable pageable = new DocumentDbPageRequest(0, 2, null);
        Page<User> page = this.repository.findByAddress(address, pageable);
        resultList = page.getContent();
        result = resultList.get(0);
        Assert.isTrue(result.getId().equals(user.getId()), "should be the same Id");
    }
}

