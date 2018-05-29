/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.exception;

import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;

public class IllegalCollectionException extends DataAccessException {
    public IllegalCollectionException(String msg) {
        super(msg);
    }

    public IllegalCollectionException(@Nullable String msg, @Nullable Throwable cause) {
        super(msg, cause);
    }
}
