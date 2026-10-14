/*
 * Copyright (c) 2026 Vivek Umrao.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.smartinventory.inventory.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Service managing programmatic database transaction boundaries, auto-commit behavior,
 * and custom transaction execution callbacks.
 */
@Service
public class DatabaseTransactionManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseTransactionManager.class);

    private final PlatformTransactionManager transactionManager;

    @Autowired
    public DatabaseTransactionManager(@Autowired(required = false) PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
        log.info("Initialized DatabaseTransactionManager (TransactionManager active: {})", transactionManager != null);
    }

    /**
     * Executes a callback within a managed transaction.
     */
    public <T> T executeInTransaction(boolean readOnly, TransactionCallback<T> action) {
        if (transactionManager == null) {
            log.warn("PlatformTransactionManager not available. Executing action outside transaction boundary.");
            return action.doInTransaction(new SimpleTransactionStatus());
        }

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setReadOnly(readOnly);
        return template.execute(action);
    }

    /**
     * Executes a callback within a transaction with a specified isolation level and propagation.
     */
    public <T> T executeWithIsolation(int isolationLevel, int propagationBehavior, TransactionCallback<T> action) {
        if (transactionManager == null) {
            log.warn("PlatformTransactionManager not available. Executing action outside transaction boundary.");
            return action.doInTransaction(new SimpleTransactionStatus());
        }

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setIsolationLevel(isolationLevel);
        template.setPropagationBehavior(propagationBehavior);
        return template.execute(action);
    }

    /**
     * Executes a callback in a NEW transaction boundary (REQUIRES_NEW).
     */
    public <T> T executeInNewTransaction(TransactionCallback<T> action) {
        return executeWithIsolation(
                TransactionDefinition.ISOLATION_DEFAULT,
                TransactionDefinition.PROPAGATION_REQUIRES_NEW,
                action
        );
    }
}
