/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core.generator;

import com.microsoft.azure.documentdb.SqlQuerySpec;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;

public class CountQueryGenerator extends AbstractQueryGenerator implements QuerySpecGenerator {

    @Override
    public SqlQuerySpec generate(DocumentQuery query) {
        return super.generateQuery(query, "SELECT VALUE COUNT(1) FROM r");
    }
}
