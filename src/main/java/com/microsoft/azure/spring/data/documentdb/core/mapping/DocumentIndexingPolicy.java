/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb.core.mapping;

import com.microsoft.azure.documentdb.IndexingMode;
import com.microsoft.azure.spring.data.documentdb.Constants;
import org.springframework.data.annotation.Persistent;

import java.lang.annotation.*;

@Persistent
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DocumentIndexingPolicy {
    boolean automatic() default Constants.DEFAULT_INDEXINGPOLICY_AUTOMATIC;

    IndexingMode mode() default IndexingMode.Consistent; // Enum is not really compile time constant

    String[] includePaths() default {};

    String[] excludePaths() default {};
}
