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

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DatabaseTransactionManagerTest {

    @Test
    void testExecuteInTransactionWithoutTransactionManager() {
        DatabaseTransactionManager txManager = new DatabaseTransactionManager(null);
        String result = txManager.executeInTransaction(true, status -> "SUCCESS");
        assertEquals("SUCCESS", result);
    }

    @Test
    void testExecuteWithIsolationWithoutTransactionManager() {
        DatabaseTransactionManager txManager = new DatabaseTransactionManager(null);
        String result = txManager.executeWithIsolation(
                TransactionDefinition.ISOLATION_READ_COMMITTED,
                TransactionDefinition.PROPAGATION_REQUIRED,
                status -> "ISOLATION_SUCCESS"
        );
        assertEquals("ISOLATION_SUCCESS", result);
    }

    @Test
    void testExecuteInNewTransactionWithoutTransactionManager() {
        DatabaseTransactionManager txManager = new DatabaseTransactionManager(null);
        String result = txManager.executeInNewTransaction(status -> "NEW_TX_SUCCESS");
        assertEquals("NEW_TX_SUCCESS", result);
    }

    @Test
    void testExecuteInTransactionWithTransactionManager() {
        PlatformTransactionManager mockTxManager = mock(PlatformTransactionManager.class);
        TransactionStatus mockStatus = new SimpleTransactionStatus();
        when(mockTxManager.getTransaction(any())).thenReturn(mockStatus);

        DatabaseTransactionManager txManager = new DatabaseTransactionManager(mockTxManager);
        String result = txManager.executeInTransaction(false, status -> "MOCKED_TX_SUCCESS");

        assertEquals("MOCKED_TX_SUCCESS", result);
        verify(mockTxManager).getTransaction(any());
        verify(mockTxManager).commit(mockStatus);
    }

    @Test
    void testExecuteInNewTransactionWithTransactionManager() {
        PlatformTransactionManager mockTxManager = mock(PlatformTransactionManager.class);
        TransactionStatus mockStatus = new SimpleTransactionStatus();
        when(mockTxManager.getTransaction(any())).thenReturn(mockStatus);

        DatabaseTransactionManager txManager = new DatabaseTransactionManager(mockTxManager);
        String result = txManager.executeInNewTransaction(status -> "NEW_MOCKED_TX_SUCCESS");

        assertEquals("NEW_MOCKED_TX_SUCCESS", result);
        verify(mockTxManager).getTransaction(any());
        verify(mockTxManager).commit(mockStatus);
    }
}
