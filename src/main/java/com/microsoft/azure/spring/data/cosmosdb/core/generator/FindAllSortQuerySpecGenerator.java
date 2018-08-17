/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core.generator;

import com.microsoft.azure.documentdb.SqlParameterCollection;
import com.microsoft.azure.documentdb.SqlQuerySpec;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
public class FindAllSortQuerySpecGenerator implements QuerySpecGenerator {

    private static final String SQL_NOCASE = "COLLATE NOCASE";

    private static final String SQL_DESC = "DESC";

    private String getParameter(@NonNull Sort.Order order) {
        String parameter = "r." + order.getProperty();

        Assert.isTrue(!order.isIgnoreCase(), "Ignore case is not supported");

        if (order.isDescending()) {
            parameter += " " + SQL_DESC;
        }

        return parameter;
    }

    @Override
    public SqlQuerySpec generate(@NonNull DocumentQuery query) {
        Assert.isTrue(query.getSort().isSorted(), "query should not be unsorted");

        final Sort sort = query.getSort();
        final List<String> parameters = new ArrayList<>();

        sort.forEach(order -> parameters.add(this.getParameter(order)));

        final String queryHeader = "SELECT * FROM ROOT r ORDER BY";
        final String queryString = queryHeader + " " + String.join(",", parameters);

        return new SqlQuerySpec(queryString, new SqlParameterCollection());
    }
}
