/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.core.converter;

import com.microsoft.azure.documentdb.Document;
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
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
    private static final String DATE_STR = "1/1/2000";

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
        final Address testAddress = new Address("98052", "testCity", "testStreet");

        final Document document = new Document();

        dbConverter.write(testAddress, document);

        assertThat(document.getId()).isEqualTo(testAddress.getPostalCode());
        assertThat(document.getString("city")).isEqualTo(testAddress.getCity());
        assertThat(document.getString("street")).isEqualTo(testAddress.getStreet());

    }

    @Test
    public void convertDocumentToAddressCorrectly() {
        final Document document = new Document();

        document.setId("testId");
        document.set("city", "testCity");
        document.set("street", "testStreet");

        final Address address = dbConverter.read(Address.class, document);

        assertThat(address.getPostalCode()).isEqualTo("testId");
        assertThat(address.getCity()).isEqualTo("testCity");
        assertThat(address.getStreet()).isEqualTo("testStreet");
    }

    @Test
    public void canWritePojoWithDateToDocument() throws ParseException {
        final Document document = new Document();
        final Memo memo = new Memo("testId", "test pojo with date", DATE_FORMAT.parse(DATE_STR));
        dbConverter.write(memo, document);

        assertThat(document.getId()).isEqualTo(memo.getId());
        assertThat(document.getString("message")).isEqualTo(memo.getMessage());
        assertThat(document.getLong("date")).isEqualTo(memo.getDate().getTime());
    }

    @Test
    public void canReadPojoWithDateFromDocument() throws ParseException {
        final Document document = new Document();
        document.setId("testId");
        document.set("message", "test pojo with date");

        final long date = DATE_FORMAT.parse(DATE_STR).getTime();
        document.set("date", date);

        final Memo memo = dbConverter.read(Memo.class, document);
        assertThat(document.getId()).isEqualTo(memo.getId());
        assertThat(document.getString("message")).isEqualTo("test pojo with date");
        assertThat(document.getLong("date")).isEqualTo(date);
    }
}

