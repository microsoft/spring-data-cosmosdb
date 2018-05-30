/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.exception;

import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;

public class DocumentDBAccessException extends DataAccessException {
    public DocumentDBAccessException(String msg) {
        super(msg);
    }

    public DocumentDBAccessException(@Nullable String msg, @Nullable Throwable cause) {
        super(msg, cause);
    }
}
