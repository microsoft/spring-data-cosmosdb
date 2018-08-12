/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core.generator;

import com.microsoft.azure.documentdb.SqlQuerySpec;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;

public interface QuerySpecGenerator {

    /**
     * Generate find/delete/update SqlQuerySpec for documentDb client.
     *
     * @param query tree structured query condition.
     * @return SqlQuerySpec executed by documentDb client.
     */
    SqlQuerySpec generate(DocumentQuery query);
}
