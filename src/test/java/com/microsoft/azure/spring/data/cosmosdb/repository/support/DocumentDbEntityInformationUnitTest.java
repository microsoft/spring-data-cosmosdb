/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.support;

import java.util.List;

import com.microsoft.azure.spring.data.cosmosdb.common.TestConstants;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.Document;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.PartitionKey;
import com.microsoft.azure.spring.data.cosmosdb.domain.Address;
import com.microsoft.azure.spring.data.cosmosdb.domain.Person;
import com.microsoft.azure.spring.data.cosmosdb.domain.Student;
import lombok.Data;
import org.junit.Test;

import org.springframework.data.annotation.Version;

import static org.assertj.core.api.Assertions.*;

public class DocumentDbEntityInformationUnitTest {
    private static final String ID = "entity_info_test_id";
    private static final String FIRST_NAME = "first name";
    private static final String LAST_NAME = "last name";
    private static final List<String> HOBBIES = TestConstants.HOBBIES;
    private static final List<Address> ADDRESSES = TestConstants.ADDRESSES;

    @Test
    public void testGetId() {
        final Person testPerson = new Person(ID, FIRST_NAME, LAST_NAME, HOBBIES, ADDRESSES);
        final DocumentDbEntityInformation<Person, String> entityInformation =
                new DocumentDbEntityInformation<Person, String>(Person.class);

        final String idField = entityInformation.getId(testPerson);

        assertThat(idField).isEqualTo(testPerson.getId());
    }

    @Test
    public void testGetIdType() {
        final DocumentDbEntityInformation<Person, String> entityInformation =
                new DocumentDbEntityInformation<Person, String>(Person.class);

        final Class<?> idType = entityInformation.getIdType();
        assertThat(idType.getSimpleName()).isEqualTo(String.class.getSimpleName());
    }

    @Test
    public void testGetCollectionName() {
        final DocumentDbEntityInformation<Person, String> entityInformation =
                new DocumentDbEntityInformation<Person, String>(Person.class);

        final String collectionName = entityInformation.getCollectionName();
        assertThat(collectionName).isEqualTo(Person.class.getSimpleName());
    }

    @Test
    public void testCustomCollectionName() {
        final DocumentDbEntityInformation<VersionedVolunteer, String> entityInformation =
                new DocumentDbEntityInformation<VersionedVolunteer, String>(VersionedVolunteer.class);

        final String collectionName = entityInformation.getCollectionName();
        assertThat(collectionName).isEqualTo("testCollection");
    }

    @Test
    public void testGetPartitionKeyName() {
        final DocumentDbEntityInformation<VolunteerWithPartitionKey, String> entityInformation =
                new DocumentDbEntityInformation<>(VolunteerWithPartitionKey.class);

        final String partitionKeyName = entityInformation.getPartitionKeyFieldName();
        assertThat(partitionKeyName).isEqualTo("name");
    }

    @Test
    public void testNullPartitionKeyName() {
        final DocumentDbEntityInformation<Volunteer, String> entityInformation =
                new DocumentDbEntityInformation<>(Volunteer.class);

        final String partitionKeyName = entityInformation.getPartitionKeyFieldName();
        assertThat(partitionKeyName).isEqualTo(null);
    }

    @Test
    public void testCustomPartitionKeyName() {
        final DocumentDbEntityInformation<VolunteerWithCustomPartitionKey, String> entityInformation =
                new DocumentDbEntityInformation<>(VolunteerWithCustomPartitionKey.class);

        final String partitionKeyName = entityInformation.getPartitionKeyFieldName();
        assertThat(partitionKeyName).isEqualTo("vol_name");
    }
    
    @Test
    public void testVersionedEntity() {
        final DocumentDbEntityInformation<VersionedVolunteer, String> entityInformation =
                new DocumentDbEntityInformation<VersionedVolunteer, String>(VersionedVolunteer.class);

        final boolean isVersioned = entityInformation.isVersioned();
        assertThat(isVersioned).isTrue();
    }

    @Test
    public void testEntityShouldNotBeVersionedWithWrongType() {
        final DocumentDbEntityInformation<WrongVersionType, String> entityInformation =
                new DocumentDbEntityInformation<WrongVersionType, String>(WrongVersionType.class);

        final boolean isVersioned = entityInformation.isVersioned();
        assertThat(isVersioned).isFalse();
    }

    @Test
    public void testEntityShouldNotBeVersionedWithoutAnnotationOnEtag() {
        final DocumentDbEntityInformation<VersionOnWrongField, String> entityInformation =
                new DocumentDbEntityInformation<VersionOnWrongField, String>(VersionOnWrongField.class);

        final boolean isVersioned = entityInformation.isVersioned();
        assertThat(isVersioned).isFalse();
    }

    @Test
    public void testNonVersionedEntity() {
        final DocumentDbEntityInformation<Student, String> entityInformation =
                new DocumentDbEntityInformation<Student, String>(Student.class);

        final boolean isVersioned = entityInformation.isVersioned();
        assertThat(isVersioned).isFalse();
    }

    @Document(collection = "testCollection")
    class Volunteer {
        String id;
        String name;
    }

    @Document
    private class VolunteerWithCustomPartitionKey {
        private String id;
        @PartitionKey("vol_name")
        private String name;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Document
    private class VolunteerWithPartitionKey {
        private String id;
        @PartitionKey
        private String name;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
    
    @Data
    @Document(collection = "testCollection")
    class VersionedVolunteer {
        private String id;
        private String name;
        @Version
        private String _etag;
    }
    
    @Data
    @Document
    class WrongVersionType {
        private String id;
        private String name;
        private long _etag;
    } 
    
    @Data
    @Document
    class VersionOnWrongField {
        private String id;
        @Version
        private String name;
        private String _etag;
    }
}
