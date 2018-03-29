/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.exception;

import org.springframework.dao.DataAccessException;

public class DocumentDBAccessException extends DataAccessException {
    public DocumentDBAccessException(String msg) {
        super(msg);
    }

    public DocumentDBAccessException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
