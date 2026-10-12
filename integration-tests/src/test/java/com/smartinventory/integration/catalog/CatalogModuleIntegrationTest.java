package com.smartinventory.integration.catalog;

import com.smartinventory.catalog.CatalogApplication;
import com.smartinventory.catalog.repository.ProductRepository;
import com.smartinventory.catalog.service.CatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = CatalogApplication.class, properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration"
})
class CatalogModuleIntegrationTest {

    @MockitoBean
    private ProductRepository productRepository;

    @Autowired(required = false)
    private CatalogService catalogService;

    @Test
    void testCatalogModuleContextLoads() {
        assertNotNull(catalogService);
    }
}
