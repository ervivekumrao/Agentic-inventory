package com.smartinventory.catalog.config;

import com.smartinventory.catalog.model.Product;
import com.smartinventory.catalog.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
public class CatalogDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(CatalogDataInitializer.class);

    @Bean
    public CommandLineRunner initCatalogData(ProductRepository productRepository) {
        return args -> {
            if (productRepository.count() == 0) {
                log.info("🌱 Catalog database is empty. Fast-seeding products...");
                List<ProductInfo> sampleProducts = List.of(
                    new ProductInfo("LAPTOP-PRO-15", "Pro 15-inch Laptop", "High-performance laptop with 16GB RAM and 512GB SSD", "Electronics", 1299.99),
                    new ProductInfo("SMARTPHONE-X", "SmartPhone X", "OLED display smartphone with advanced dual camera", "Electronics", 899.50),
                    new ProductInfo("WIRELESS-BUDS", "Wireless Earbuds", "Active noise cancelling Bluetooth earbuds", "Electronics", 149.99),
                    new ProductInfo("MECHANICAL-KEYBOARD", "RGB Mechanical Keyboard", "Tactile switches with customizable RGB backlight", "Electronics", 89.99),
                    new ProductInfo("4K-MONITOR", "27-inch 4K Monitor", "IPS panel UHD monitor with HDR support", "Electronics", 349.00),

                    new ProductInfo("DENIM-JACKET", "Classic Denim Jacket", "100% cotton vintage wash denim jacket", "Apparel", 79.99),
                    new ProductInfo("RUNNING-SHOES", "AirStride Running Shoes", "Lightweight breathable athletic running shoes", "Apparel", 119.50),
                    new ProductInfo("MERINO-SWEATER", "Merino Wool Sweater", "Warm and soft premium wool sweater", "Apparel", 89.00),
                    new ProductInfo("COTTON-HOODIE", "Fleece Pullover Hoodie", "Comfortable everyday wear hoodie", "Apparel", 55.00),
                    new ProductInfo("LEATHER-BELT", "Genuine Leather Belt", "Durable leather belt with metallic buckle", "Apparel", 39.99),

                    new ProductInfo("BLENDER-PRO", "High-Speed Blender", "1200W professional blender for smoothies and soups", "Home & Kitchen", 129.99),
                    new ProductInfo("COFFEE-MAKER", "Espresso Machine", "15-bar pump espresso and cappuccino maker", "Home & Kitchen", 199.99),
                    new ProductInfo("AIR-FRYER", "Digital Air Fryer 5.8L", "Oil-less healthy air fryer with touch control", "Home & Kitchen", 109.99),
                    new ProductInfo("CHEF-KNIFE", "Damascus Chef Knife", "8-inch high carbon steel professional kitchen knife", "Home & Kitchen", 85.00),
                    new ProductInfo("CERAMIC-MUG-SET", "Ceramic Coffee Mug Set", "Set of 4 handcrafted ceramic mugs", "Home & Kitchen", 32.50),

                    new ProductInfo("ORGANIC-HONEY", "Raw Organic Honey", "100% pure unfiltered raw honey 32oz", "Groceries", 18.99),
                    new ProductInfo("ESPRESSO-BEANS", "Artisan Espresso Coffee Beans", "Dark roast whole bean coffee 1kg", "Groceries", 24.50),
                    new ProductInfo("ALMOND-BUTTER", "Natural Creamy Almond Butter", "No added sugar or palm oil", "Groceries", 12.99),
                    new ProductInfo("GREEN-TEA", "Japanese Sencha Green Tea", "Organic loose leaf green tea 250g", "Groceries", 15.00),
                    new ProductInfo("DARK-CHOCOLATE", "70% Dark Chocolate Bar", "Artisan single-origin dark chocolate", "Groceries", 4.99),

                    new ProductInfo("CORDLESS-DRILL", "20V Cordless Drill Driver", "Brushless motor drill with 2 batteries and charger", "Tools & Hardware", 149.99),
                    new ProductInfo("TOOL-SET", "150-Piece Mechanics Tool Set", "Chrome vanadium steel socket and wrench set", "Tools & Hardware", 129.00),
                    new ProductInfo("LED-WORK-LIGHT", "Rechargeable LED Work Light", "5000 lumens portable flood light", "Tools & Hardware", 59.99),
                    new ProductInfo("DIGITAL-MULTIMETER", "Auto-Ranging Digital Multimeter", "True RMS electrical tester meter", "Tools & Hardware", 39.99),
                    new ProductInfo("STAINLESS-SCREWS", "Assorted Stainless Screws Box", "500-piece wood and metal screw assortment", "Tools & Hardware", 24.99),

                    new ProductInfo("YOGA-MAT", "Eco-Friendly Yoga Mat", "Non-slip thick exercise and yoga mat with strap", "Sports & Outdoors", 29.99),
                    new ProductInfo("CAMPING-TENT", "4-Person Waterproof Camping Tent", "Easy setup dome tent for family camping", "Sports & Outdoors", 179.99),
                    new ProductInfo("HIKING-BACKPACK", "50L Hiking Backpack", "Ergonomic internal frame backpack with rain cover", "Sports & Outdoors", 109.00),
                    new ProductInfo("INSULATED-FLASK", "Stainless Steel Insulated Flask", "Keeps drinks cold for 24h or hot for 12h", "Sports & Outdoors", 34.99),
                    new ProductInfo("BIKE-HELMET", "Commuter Cycling Helmet", "Adjustable lightweight bicycle helmet", "Sports & Outdoors", 49.99)
                );

                List<Product> productsToSave = new ArrayList<>();
                Instant now = Instant.now();
                for (ProductInfo p : sampleProducts) {
                    Product product = new Product(p.sku, p.name, p.description, p.category, BigDecimal.valueOf(p.price), Map.of("brand", "SmartBrand", "warranty", "1 Year"));
                    product.setCreatedAt(now);
                    product.setUpdatedAt(now);
                    productsToSave.add(product);
                }
                productRepository.saveAll(productsToSave);
                log.info("✅ Successfully fast-seeded {} products into catalog!", productsToSave.size());
            }
        };
    }

    private record ProductInfo(String sku, String name, String description, String category, double price) {}
}
