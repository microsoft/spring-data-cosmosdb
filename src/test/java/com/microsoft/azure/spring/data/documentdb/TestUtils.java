/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.documentdb;

import java.util.ArrayList;
import java.util.List;

public class TestUtils {
    public static <T> List<T> toList(Iterable<T> iterable) {
        if (iterable != null) {
            final List<T> list = new ArrayList<>();
            iterable.forEach(list::add);
            return list;
        }
        return null;
    }
}
