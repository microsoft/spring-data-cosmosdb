/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb;

import com.microsoft.azure.documentdb.IndexingMode;

public class Constants {
    private Constants() {
        // Hide the implicit public one
    }

    public static final String DEFAULT_COLLECTION_NAME = "";
    public static final String DEFAULT_REQUEST_UNIT = "4000";
    public static final boolean DEFAULT_INDEXINGPOLICY_AUTOMATIC = true;
    public static final IndexingMode DEFAULT_INDEXINGPOLICY_MODE = IndexingMode.Consistent;
    public static final String DEFAULT_REPOSITORY_IMPLEMENT_POSTFIX = "Impl";

    public static final String ID_PROPERTY_NAME = "id";

    public static final String DOCUMENTDB_MODULE_NAME = "documentdb";
    public static final String DOCUMENTDB_MODULE_PREFIX = "documentdb";
    public static final String DOCUMENTDB_MAPPING_CONTEXT_NAME = "documentDbMappingContext";

    public static final String USER_AGENT_SUFFIX = "spring-data/2.0.2-SNAPSHOT";

    public static final String CRITERIA_AND = " AND ";
    public static final String CRITERIA_OR = ") OR (";
    public static final String CRITERIA_LEFT_BRACKET = "(";
    public static final String CRITERIA_RIGHT_BRACKET = ")";

    public static final String IS_EQUAL = "r.@?=@@";
}

