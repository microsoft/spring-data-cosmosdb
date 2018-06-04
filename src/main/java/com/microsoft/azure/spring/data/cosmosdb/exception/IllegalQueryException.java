/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.exception;

import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;

public class IllegalQueryException extends DataAccessException {
    public IllegalQueryException(String msg) {
        super(msg);
    }

    public IllegalQueryException(@Nullable String msg, @Nullable Throwable cause) {
        super(msg, cause);
    }
}
