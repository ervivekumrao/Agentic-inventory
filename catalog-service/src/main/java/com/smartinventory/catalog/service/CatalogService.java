package com.smartinventory.catalog.service;

import com.smartinventory.catalog.model.Product;
import com.smartinventory.catalog.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class CatalogService {

    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);

    private final ProductRepository productRepository;

    public CatalogService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAllProducts() {
        log.trace("Querying all products from repository");
        return productRepository.findAll();
    }

    public Optional<Product> getProductById(String id) {
        log.trace("Querying product by id: {}", id);
        return productRepository.findById(id);
    }

    public Optional<Product> getProductBySku(String sku) {
        log.trace("Querying product by SKU: {}", sku);
        return productRepository.findBySku(sku);
    }

    public List<Product> getProductsByCategory(String category) {
        log.trace("Querying products by category: {}", category);
        return productRepository.findByCategory(category);
    }

    public Product createProduct(Product product) {
        log.debug("Creating product with SKU: {}", product.getSku());
        if (productRepository.existsBySku(product.getSku())) {
            log.warn("Attempted to create duplicate product with SKU: {}", product.getSku());
            throw new IllegalArgumentException("Product with SKU " + product.getSku() + " already exists.");
        }
        product.setCreatedAt(Instant.now());
        product.setUpdatedAt(Instant.now());
        Product saved = productRepository.save(product);
        log.debug("Saved product with ID: {} and SKU: {}", saved.getId(), saved.getSku());
        return saved;
    }

    public List<Product> createProductsBulk(List<Product> products) {
        log.debug("Bulk creating {} products", products.size());
        Instant now = Instant.now();
        for (Product p : products) {
            if (p.getCreatedAt() == null) p.setCreatedAt(now);
            if (p.getUpdatedAt() == null) p.setUpdatedAt(now);
        }
        List<Product> saved = productRepository.saveAll(products);
        log.debug("Successfully bulk-saved {} products", saved.size());
        return saved;
    }

    public Product updateProduct(String id, Product productDetails) {
        log.debug("Updating product ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product update failed, ID not found: {}", id);
                    return new IllegalArgumentException("Product not found with id: " + id);
                });

        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setCategory(productDetails.getCategory());
        product.setPrice(productDetails.getPrice());
        product.setAttributes(productDetails.getAttributes());
        product.setUpdatedAt(Instant.now());

        Product updated = productRepository.save(product);
        log.debug("Updated product saved successfully for ID: {}", id);
        return updated;
    }

    public void deleteProduct(String id) {
        log.debug("Deleting product ID: {}", id);
        if (!productRepository.existsById(id)) {
            log.warn("Product delete failed, ID not found: {}", id);
            throw new IllegalArgumentException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
        log.debug("Deleted product ID: {} successfully", id);
    }
}
