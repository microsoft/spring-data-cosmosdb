/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb;

import com.microsoft.azure.documentdb.IndexingMode;
import com.microsoft.azure.spring.data.documentdb.domain.Address;

import java.util.Arrays;
import java.util.List;

public class Constants {
    public static final List<String> HOBBIES    = Arrays.asList("photography", "fishing");

    private static final Address ADDRESS_1      = new Address("201107", "Zixing Road", "Shanghai");
    private static final Address ADDRESS_2      = new Address("200000", "Xuhui", "Shanghai");
    public static final List<Address> ADDRESSES = Arrays.asList(ADDRESS_1, ADDRESS_2);

    public static final String DEFAULT_COLLECTION_NAME           = "Person";
    public static final int DEFAULT_REQUEST_UNIT                 = 4000;
    public static final boolean DEFAULT_INDEXINGPOLICY_AUTOMATIC = true;
    public static final IndexingMode DEFAULT_INDEXINGPOLICY_MODE = IndexingMode.Consistent;
    public static final String[] DEFAULT_INCLUDEDPATHS           = {
            "{\"path\":\"/*\",\"indexes\":[" +
                    "{\"kind\":\"Range\",\"dataType\":\"Number\",\"precision\":-1}," +
                    "{\"kind\":\"Hash\",\"dataType\":\"String\",\"precision\":3}" +
                    "]}",
    };
    public static final String[] DEFAULT_EXCLUDEDPATHS           = {
    };

    public static final String TEST_COLLECTION_NAME           = "Role";
    public static final int TEST_REQUEST_UNIT                 = 1000;
    public static final boolean TEST_INDEXINGPOLICY_AUTOMATIC = false;
    public static final IndexingMode TEST_INDEXINGPOLICY_MODE = IndexingMode.Lazy;
    public static final String[] TEST_INCLUDEDPATHS           = {
            "{\"path\":\"/*\",\"indexes\":[" +
                    "{\"kind\":\"Hash\",\"dataType\":\"String\",\"precision\":3}",
            // No tail "]}" here because the azure may change the value of paths, and we just keep front part of string
            "{\"path\":\"/cache/*\",\"indexes\":[" +
                    "{\"kind\":\"Range\",\"dataType\":\"Number\",\"precision\":-1}",
    };
    public static final String[] TEST_EXCLUDEDPATHS           = {
            "{\"path\":\"/excluded/*\"}",
            "{\"path\":\"/props/*\"}",
    };

    public static final String TEST_DB_NAME                   = "template_it_db_pli";
    public static final String TEST_ID                        = "template_it_id_pli";
    public static final String TEST_FIRST_NAME                = "first_name_li";
    public static final String TEST_LAST_NAME                 = "last_name_p";
    public static final String TEST_LEVEL                     = "B";
    public static final String TEST_ROLE_NAME                 = "Developer";
}

