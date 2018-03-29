/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.core.converter;

import com.microsoft.azure.documentdb.Document;
import com.microsoft.azure.spring.data.documentdb.TestConstants;
import com.microsoft.azure.spring.data.documentdb.core.convert.MappingDocumentDbConverter;
import com.microsoft.azure.spring.data.documentdb.core.mapping.DocumentDbMappingContext;
import com.microsoft.azure.spring.data.documentdb.domain.Address;
import com.microsoft.azure.spring.data.documentdb.domain.Memo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class MappingDocumentDbConverterUnitTest {
    private static final SimpleDateFormat DATE = new SimpleDateFormat(TestConstants.DATE_FORMAT);
    private static final SimpleDateFormat TIMEZONE_DATE = new SimpleDateFormat(TestConstants.DATE_TIMEZONE_FORMAT);

    MappingDocumentDbConverter dbConverter;
    DocumentDbMappingContext mappingContext;

    @Mock
    ApplicationContext applicationContext;

    @Before
    public void setup() {
        mappingContext = new DocumentDbMappingContext();

        mappingContext.setApplicationContext(applicationContext);
        mappingContext.afterPropertiesSet();
        mappingContext.getPersistentEntity(Address.class);

        dbConverter = new MappingDocumentDbConverter(mappingContext);
    }

    @Test
    public void covertAddressToDocumentCorrectly() {
        final Document document = new Document();
        final Address testAddress = new Address(TestConstants.POSTAL_CODE, TestConstants.CITY, TestConstants.STREET);

        dbConverter.write(testAddress, document);

        assertThat(document.getId()).isEqualTo(testAddress.getPostalCode());
        assertThat(document.getString(TestConstants.PROPERTY_CITY)).isEqualTo(testAddress.getCity());
        assertThat(document.getString(TestConstants.PROPERTY_STREET)).isEqualTo(testAddress.getStreet());
    }

    @Test
    public void convertDocumentToAddressCorrectly() {
        final Document document = new Document();

        document.setId(TestConstants.POSTAL_CODE);
        document.set(TestConstants.PROPERTY_CITY, TestConstants.CITY);
        document.set(TestConstants.PROPERTY_STREET, TestConstants.STREET);

        final Address address = dbConverter.read(Address.class, document);

        assertThat(address.getPostalCode()).isEqualTo(TestConstants.POSTAL_CODE);
        assertThat(address.getCity()).isEqualTo(TestConstants.CITY);
        assertThat(address.getStreet()).isEqualTo(TestConstants.STREET);
    }

    @Test
    public void canWritePojoWithDateToDocument() throws ParseException {
        final Document document = new Document();
        final Memo memo = new Memo(TestConstants.ID, TestConstants.MESSAGE, DATE.parse(TestConstants.DATE_STRING));
        dbConverter.write(memo, document);

        assertThat(document.getId()).isEqualTo(memo.getId());
        assertThat(document.getString(TestConstants.PROPERTY_MESSAGE)).isEqualTo(memo.getMessage());
        assertThat(document.getLong(TestConstants.PROPERTY_DATE)).isEqualTo(memo.getDate().getTime());
    }

    @Test
    public void canReadPojoWithDateFromDocument() throws ParseException {
        final Document document = new Document();
        document.setId(TestConstants.ID);
        document.set(TestConstants.PROPERTY_MESSAGE, TestConstants.MESSAGE);

        final long date = DATE.parse(TestConstants.DATE_STRING).getTime();
        document.set(TestConstants.PROPERTY_DATE, date);

        final Memo memo = dbConverter.read(Memo.class, document);
        assertThat(document.getId()).isEqualTo(memo.getId());
        assertThat(document.getString(TestConstants.PROPERTY_MESSAGE)).isEqualTo(TestConstants.MESSAGE);
        assertThat(document.getLong(TestConstants.PROPERTY_DATE)).isEqualTo(date);
    }

    @Test
    public void convertDateValueToMilliSeconds() throws ParseException {
        final Date date = TIMEZONE_DATE.parse(TestConstants.DATE_TIMEZONE_STRING);
        final long time = (Long) dbConverter.mapToDocumentDBValue(date);

        assertThat(time).isEqualTo(TestConstants.MILLI_SECONDS);
    }
}

