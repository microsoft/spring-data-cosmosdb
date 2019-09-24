/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core.convert;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import static com.microsoft.azure.spring.data.cosmosdb.Constants.ISO_8601_COMPATIBLE_DATE_PATTERN;

public class ZonedDateTimeDeserializer extends JsonDeserializer<ZonedDateTime> {

    @Override
    public ZonedDateTime deserialize(final JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        return parse(jsonParser);
    }

    public ZonedDateTime parse(final JsonParser jsonParser) throws IOException {
        if (jsonParser.getValueAsString() == null) {
            return null;
        }

        try {
            return ZonedDateTime.parse(jsonParser.getValueAsString(),
                    DateTimeFormatter.ofPattern(ISO_8601_COMPATIBLE_DATE_PATTERN));
        } catch (DateTimeParseException e) {
            throw new JsonParseException(jsonParser, jsonParser.getValueAsString(), e);
        }
    }

}
