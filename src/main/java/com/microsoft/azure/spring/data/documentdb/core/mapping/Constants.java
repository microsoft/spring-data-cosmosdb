/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.core.mapping;

import com.microsoft.azure.documentdb.IndexingMode;

public class Constants {
    public static final String DEFAULT_COLLECTION_NAME           = "";
    public static final String DEFAULT_REQUEST_UNIT              = "4000";
    public static final boolean DEFAULT_INDEXINGPOLICY_AUTOMATIC = true;
    public static final IndexingMode DEFAULT_INDEXINGPOLICY_MODE = IndexingMode.Consistent;
}

