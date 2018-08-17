/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.repository;

import com.microsoft.azure.spring.data.cosmosdb.domain.Memo;
import com.microsoft.azure.spring.data.cosmosdb.domain.Importance;
import com.microsoft.azure.spring.data.cosmosdb.repository.DocumentDbRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface MemoRepository extends DocumentDbRepository<Memo, String> {
    List<Memo> findMemoByDate(Date date);

    List<Memo> findMemoByImportance(Importance importance);

    List<Memo> findByDateBefore(Date date);

    List<Memo> findByDateBeforeAndMessage(Date date, String message);

    List<Memo> findByDateBeforeOrMessage(Date date, String message);
}
