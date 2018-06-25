/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
        
package com.microsoft.azure.spring.data.cosmosdb.core.convert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

public class JsonSerializableFactory {
    private static ObjectMapper OBJECTMAPPER = new ObjectMapper();

    static {
        OBJECTMAPPER.registerModule(new ParameterNamesModule())
                        .registerModule(new Jdk8Module())
                        .registerModule(new JavaTimeModule());
    }

    public static ObjectMapper getObjectMapper() {
        return OBJECTMAPPER;
    }
}

