package com.smartinventory.inventory.config;

import com.smartinventory.inventory.model.InventoryItem;
import com.smartinventory.inventory.model.RoleEntity;
import com.smartinventory.inventory.model.UserEntity;
import com.smartinventory.inventory.repository.InventoryRepository;
import com.smartinventory.inventory.repository.RoleRepository;
import com.smartinventory.inventory.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;

@Configuration
public class FastDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(FastDataInitializer.class);

    @Value("${app.security.default-admin-password:admin123}")
    private String adminDefaultPassword;

    @Value("${app.security.default-manager-password:manager123}")
    private String managerDefaultPassword;

    @Value("${app.security.default-operator-password:operator123}")
    private String operatorDefaultPassword;

    @Value("${app.security.default-viewer-password:viewer123}")
    private String viewerDefaultPassword;

    @Bean
    public CommandLineRunner initializeInventoryData(
            RoleRepository roleRepository,
            UserRepository userRepository,
            InventoryRepository inventoryRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            // Warn if default credentials are detected in the environment
            if ("admin123".equals(adminDefaultPassword)) {
                log.warn("⚠️ SECURITY NOTICE: Default administrator password is in use. Set APP_SECURITY_DEFAULT_ADMIN_PASSWORD in production environments.");
            }

            // 1. Seed Roles
            String[] roles = {"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_ENTRY_OPERATOR", "ROLE_USER"};
            for (String roleName : roles) {
                if (roleRepository.findByName(roleName).isEmpty()) {
                    roleRepository.save(new RoleEntity(roleName, "Default " + roleName));
                }
            }

            // 2. Seed Users
            if (!userRepository.existsByUsername("admin")) {
                UserEntity admin = new UserEntity("admin", passwordEncoder.encode(adminDefaultPassword));
                admin.setRoles(Set.of(roleRepository.findByName("ROLE_ADMIN").orElseThrow()));
                userRepository.save(admin);
            }
            if (!userRepository.existsByUsername("manager")) {
                UserEntity manager = new UserEntity("manager", passwordEncoder.encode(managerDefaultPassword));
                manager.setRoles(Set.of(roleRepository.findByName("ROLE_MANAGER").orElseThrow()));
                userRepository.save(manager);
            }
            if (!userRepository.existsByUsername("operator")) {
                UserEntity operator = new UserEntity("operator", passwordEncoder.encode(operatorDefaultPassword));
                operator.setRoles(Set.of(roleRepository.findByName("ROLE_ENTRY_OPERATOR").orElseThrow()));
                userRepository.save(operator);
            }
            if (!userRepository.existsByUsername("viewer")) {
                UserEntity viewer = new UserEntity("viewer", passwordEncoder.encode(viewerDefaultPassword));
                viewer.setRoles(Set.of(roleRepository.findByName("ROLE_USER").orElseThrow()));
                userRepository.save(viewer);
            }

            // 3. Seed Inventory Items if empty (using batch saveAll for maximum speed)
            if (inventoryRepository.count() == 0) {
                log.info("🌱 Inventory database is empty. Fast-seeding inventory items...");
                List<String> warehouses = List.of("Warehouse-North", "Warehouse-South", "Warehouse-East", "Warehouse-West", "Central-Hub");
                List<String> sampleSkus = List.of(
                    "LAPTOP-PRO-15", "SMARTPHONE-X", "WIRELESS-BUDS", "MECHANICAL-KEYBOARD", "4K-MONITOR",
                    "DENIM-JACKET", "RUNNING-SHOES", "MERINO-SWEATER", "COTTON-HOODIE", "LEATHER-BELT",
                    "BLENDER-PRO", "COFFEE-MAKER", "AIR-FRYER", "CHEF-KNIFE", "CERAMIC-MUG-SET",
                    "ORGANIC-HONEY", "ESPRESSO-BEANS", "ALMOND-BUTTER", "GREEN-TEA", "DARK-CHOCOLATE",
                    "CORDLESS-DRILL", "TOOL-SET", "LED-WORK-LIGHT", "DIGITAL-MULTIMETER", "STAINLESS-SCREWS",
                    "YOGA-MAT", "CAMPING-TENT", "HIKING-BACKPACK", "INSULATED-FLASK", "BIKE-HELMET"
                );

                List<InventoryItem> itemsToSave = new java.util.ArrayList<>();
                for (int idx = 0; idx < sampleSkus.size(); idx++) {
                    String sku = sampleSkus.get(idx);
                    int initialQty = (idx * 13) % 85 + 5;
                    int threshold = 10;
                    String warehouse = warehouses.get(idx % warehouses.size());

                    itemsToSave.add(new InventoryItem(sku, initialQty, threshold, warehouse));
                }
                inventoryRepository.saveAll(itemsToSave);
                log.info("✅ Successfully fast-seeded {} inventory items!", itemsToSave.size());
            }
        };
    }
}
