/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core.generator;

import com.microsoft.azure.cosmosdb.SqlQuerySpec;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

@NoArgsConstructor
public class FindQuerySpecGenerator extends AbstractQueryGenerator implements QuerySpecGenerator {

    @Override
    public com.microsoft.azure.documentdb.SqlQuerySpec generate(@NonNull DocumentQuery query) {
        return super.generateQuery(query, "SELECT * FROM ROOT r");
    }

    public SqlQuerySpec generateAsync(@NonNull DocumentQuery query) {
        return super.generateQueryAsync(query, "SELECT * FROM ROOT r");
    }
}
