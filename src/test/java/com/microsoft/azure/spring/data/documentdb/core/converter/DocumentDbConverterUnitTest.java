/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.documentdb.core.converter;


import com.microsoft.azure.documentdb.Document;
import com.microsoft.azure.spring.data.documentdb.Constants;
import com.microsoft.azure.spring.data.documentdb.core.convert.DocumentDbConverter;
import com.microsoft.azure.spring.data.documentdb.domain.Address;
import com.microsoft.azure.spring.data.documentdb.domain.Person;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class DocumentDbConverterUnitTest {
    private static final String id = "db_converter_test_id";
    private static final String firstName = "testFirstName";
    private static final String lastName = "testLastName";
    private static final List<String> hobbies = Constants.HOBBIES;
    private static final List<Address> addresses = Constants.ADDRESSES;

    private static final String idPropertyName = "id";
    private static final String firstNamePropertyName = "firstName";
    private static final String lastNamePropertyName = "lastName";
    private static final String hobbiesPropertyName = "hobbies";
    private static final String shippingAddressesPropertyName = "shippingAddresses";

    private DocumentDbConverter dbConverter;

    @Before
    public void setup() {
        dbConverter = new DocumentDbConverter();
    }

    @Test
    public void testConvertFromEntityToDocument() {
        final Person person = new Person(id, firstName, lastName, hobbies, addresses);
        final Document document = dbConverter.convertToDocument(person);

        assertTrue(document.has(idPropertyName));
        assertTrue(document.has(firstNamePropertyName));
        assertTrue(document.has(lastNamePropertyName));
        assertTrue(document.has(hobbiesPropertyName));
        assertTrue(document.has(shippingAddressesPropertyName));
        assertThat(document.getId()).isEqualTo(id);
        assertThat(document.getString(firstNamePropertyName)).isEqualTo(firstName);
        assertThat(document.getString(lastNamePropertyName)).isEqualTo(lastName);

        final Collection<String> convertedHobbies = document.getCollection(hobbiesPropertyName, String.class);
        assertTrue(hobbies.equals(convertedHobbies));

        final Collection<Address> convertedAddresses =
                document.getCollection(shippingAddressesPropertyName, Address.class);
        assertTrue(addresses.equals(convertedAddresses));
    }

    @Test
    public void testConvertFromDocumentToEntity() {
        final JSONObject json = new JSONObject();
        json.put(idPropertyName, id);
        json.put(firstNamePropertyName, firstName);
        json.put(lastNamePropertyName, lastName);
        json.put(hobbiesPropertyName, hobbies);
        json.put(shippingAddressesPropertyName, addresses);

        final Document document = new Document(JSONObject.valueToString(json));
        final Person person = dbConverter.convertFromDocument(document, Person.class);
        assertThat(person.getId()).isEqualTo(id);
        assertThat(person.getFirstName()).isEqualTo(firstName);
        assertThat(person.getLastName()).isEqualTo(lastName);
        assertThat(person.getHobbies()).isEqualTo(hobbies);
        assertThat(person.getShippingAddresses()).isEqualTo(addresses);
    }
}
