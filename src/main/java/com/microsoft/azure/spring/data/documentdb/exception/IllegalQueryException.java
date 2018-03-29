/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.exception;

import org.springframework.dao.DataAccessException;

public class IllegalQueryException extends DataAccessException {
    public IllegalQueryException(String msg) {
        super(msg);
    }

    public IllegalQueryException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
