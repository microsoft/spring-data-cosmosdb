/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core.convert;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ZonedDateTimeDeserializerTest {

    private static final String EXAMPLE_DATE_STRING = "2018-10-08T15:06:07:992Z";
    private static final String ILLEGAL_DATE_STRING = "illegal-date-string";
    private static final ZonedDateTime EXPECTED_SERIALIZED_ZONED_DATE_TIME
            = ZonedDateTime.of(2018, 10, 8, 15, 6, 7, 992000000, ZoneId.of("Z"));

    @Test
    public void parse() throws IOException {
        final JsonParser jsonParser = Mockito.mock(JsonParser.class);
        when(jsonParser.getValueAsString()).thenReturn(EXAMPLE_DATE_STRING);

        final ZonedDateTime zonedDateTime = new ZonedDateTimeDeserializer().parse(jsonParser);

        assertThat(zonedDateTime.equals(EXPECTED_SERIALIZED_ZONED_DATE_TIME)).isTrue();
    }

    @Test(expected = JsonParseException.class)
    public void testParseException() throws IOException {
        final JsonParser jsonParser = Mockito.mock(JsonParser.class);
        when(jsonParser.getValueAsString()).thenReturn(ILLEGAL_DATE_STRING);

        new ZonedDateTimeDeserializer().parse(jsonParser);
    }
}
