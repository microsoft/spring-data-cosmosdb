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
    public static final List<String> HOBBIES = Arrays.asList("photography", "fishing");

    private static final Address ADDRESS_1 = new Address("201107", "Zixing Road", "Shanghai");
    private static final Address ADDRESS_2 = new Address("200000", "Xuhui", "Shanghai");
    public static final List<Address> ADDRESSES = Arrays.asList(ADDRESS_1, ADDRESS_2);

    public static final String DEFAULT_COLLECTION_NAME = "Person";
    public static final int DEFAULT_REQUEST_UNIT = 4000;
    public static final boolean DEFAULT_INDEXINGPOLICY_AUTOMATIC = true;
    public static final IndexingMode DEFAULT_INDEXINGPOLICY_MODE = IndexingMode.Consistent;

    public static final String COLLECTION_NAME = "Address";
    public static final int REQUEST_UNIT = 1000;
    public static final boolean INDEXINGPOLICY_AUTOMATIC = false;
    public static final IndexingMode INDEXINGPOLICY_MODE = IndexingMode.Lazy;
}
