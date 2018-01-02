/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.core.query;

import java.util.LinkedHashMap;


public class Criteria implements CriteriaDefinition {

    private String key;
    private Object value;

    public Criteria(String key, LinkedHashMap<String, Object> value) {
        this.key = key;
        this.value = value.get(key);
    }

    public Object getCriteriaObject() {
        return value;
    }

    public String getKey() {
        return key;
    }

    public static Criteria where(String key, Object value) {
        return new Criteria(key, (LinkedHashMap<String, Object>) value);
    }

    public Criteria is(Object o) {
        return this;
    }
}
