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
package example.springdata.documentdb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Arrays;

@SpringBootApplication
public class Application {

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

    private final Address address = new Address(POSTAL_CODE, STREET, CITY);
    private final Role creator = new Role(ROLE_CREATOR, COST_CREATOR);
    private final Role contributor = new Role(ROLE_CONTRIBUTOR, COST_CONTRIBUTOR);
    private final User user = new User(ID, EMAIL, NAME, address, Arrays.asList(creator, contributor));

    @Autowired
    private UserRepository repository;

    public static void main(String... args) {
        SpringApplication.run(Application.class, args);
    }

    @PostConstruct
    public void setup() {
        this.repository.save(user);
    }

    @PreDestroy
    public void cleanup() {
        this.repository.deleteAll();
    }
}
