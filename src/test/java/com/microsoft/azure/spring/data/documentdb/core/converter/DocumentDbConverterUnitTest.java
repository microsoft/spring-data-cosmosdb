/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.documentdb.core.converter;


import com.microsoft.azure.documentdb.Document;
import com.microsoft.azure.spring.data.documentdb.TestConstants;
import com.microsoft.azure.spring.data.documentdb.core.convert.DocumentDbConverter;
import com.microsoft.azure.spring.data.documentdb.domain.Address;
import com.microsoft.azure.spring.data.documentdb.domain.Person;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class DocumentDbConverterUnitTest {
    private DocumentDbConverter dbConverter;

    @Before
    public void setup() {
        dbConverter = new DocumentDbConverter();
    }

    @Test
    public void testConvertFromEntityToDocument() {
        final Person person = new Person(TestConstants.ID, TestConstants.FIRST_NAME,
                TestConstants.LAST_NAME, TestConstants.HOBBIES, TestConstants.ADDRESSES);
        final Document document = dbConverter.convertToDocument(person);

        assertTrue(document.has(TestConstants.PROPERTY_ID));
        assertTrue(document.has(TestConstants.PROPERTY_FIRST_NAME));
        assertTrue(document.has(TestConstants.PROPERTY_LAST_NAME));
        assertTrue(document.has(TestConstants.PROPERTY_HOBBIES));
        assertTrue(document.has(TestConstants.PROPERTY_SHIPPING_ADDRESSES));
        assertThat(document.getId()).isEqualTo(TestConstants.ID);
        assertThat(document.getString(TestConstants.PROPERTY_FIRST_NAME)).isEqualTo(TestConstants.FIRST_NAME);
        assertThat(document.getString(TestConstants.PROPERTY_LAST_NAME)).isEqualTo(TestConstants.LAST_NAME);

        final Collection<String> gotHobbies = document.getCollection(TestConstants.PROPERTY_HOBBIES, String.class);
        assertTrue(TestConstants.HOBBIES.equals(gotHobbies));

        final Collection<Address> gotAddresses =
                document.getCollection(TestConstants.PROPERTY_SHIPPING_ADDRESSES, Address.class);
        assertTrue(TestConstants.ADDRESSES.equals(gotAddresses));
    }

    @Test
    public void testConvertFromDocumentToEntity() {
        final JSONObject json = new JSONObject();
        json.put(TestConstants.PROPERTY_ID, TestConstants.ID);
        json.put(TestConstants.PROPERTY_FIRST_NAME, TestConstants.FIRST_NAME);
        json.put(TestConstants.PROPERTY_LAST_NAME, TestConstants.LAST_NAME);
        json.put(TestConstants.PROPERTY_HOBBIES, TestConstants.HOBBIES);
        json.put(TestConstants.PROPERTY_SHIPPING_ADDRESSES, TestConstants.ADDRESSES);

        final Document document = new Document(JSONObject.valueToString(json));
        final Person person = dbConverter.convertFromDocument(document, Person.class);
        assertThat(person.getId()).isEqualTo(TestConstants.ID);
        assertThat(person.getFirstName()).isEqualTo(TestConstants.FIRST_NAME);
        assertThat(person.getLastName()).isEqualTo(TestConstants.LAST_NAME);
        assertThat(person.getHobbies()).isEqualTo(TestConstants.HOBBIES);
        assertThat(person.getShippingAddresses()).isEqualTo(TestConstants.ADDRESSES);
    }
}
