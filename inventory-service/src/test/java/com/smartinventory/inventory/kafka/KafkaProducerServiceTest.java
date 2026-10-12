package com.smartinventory.inventory.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private KafkaProducerService kafkaProducerService;

    @Test
    void sendStockUpdateEvent_shouldPublishToKafkaTopic() {
        StockUpdateEvent event = new StockUpdateEvent("SKU-001", 100, "IN");
        assertDoesNotThrow(() -> kafkaProducerService.sendStockUpdateEvent(event));
        verify(kafkaTemplate).send(eq("inventory-stock-events"), eq("SKU-001"), any(StockUpdateEvent.class));
    }
}
