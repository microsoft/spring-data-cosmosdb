/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core.generator;

import com.microsoft.azure.cosmosdb.SqlQuerySpec;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import org.springframework.lang.NonNull;

public class CountQueryGenerator extends AbstractQueryGenerator implements QuerySpecGenerator {

    @Override
    public com.microsoft.azure.documentdb.SqlQuerySpec generate(DocumentQuery query) {
        return super.generateQuery(query, "SELECT VALUE COUNT(1) FROM r");
    }

    @Override
    public SqlQuerySpec generateAsync(@NonNull DocumentQuery query) {
        return super.generateAsyncQuery(query, "SELECT VALUE COUNT(1) FROM r");
    }
}
