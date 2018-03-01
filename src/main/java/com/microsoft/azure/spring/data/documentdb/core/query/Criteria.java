/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.core.query;

import java.util.ArrayList;
import java.util.List;

public class Criteria implements CriteriaDefinition {

    private String key;
    private Object value;
    private List<Criteria> criteriaChain;

    public Criteria(String key) {
        this.criteriaChain = new ArrayList<>();
        this.criteriaChain.add(this);
        this.key = key;
    }

    protected Criteria(List<Criteria> criteriaChain, String key) {
        this.criteriaChain = criteriaChain;
        this.criteriaChain.add(this);
        this.key = key;
    }

    public Object getCriteriaObject() {
        return value;
    }

    public String getKey() {
        return key;
    }

    public static Criteria where(String key) {
        return new Criteria(key);
    }

    public Criteria is(Object o) {
        this.value = o;
        return this;
    }

    public Criteria and(String key) {
        return new Criteria(this.criteriaChain, key);
    }

    public List<Criteria> getCriteriaChain() {
        return criteriaChain;
    }
}
