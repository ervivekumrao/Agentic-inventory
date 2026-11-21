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

package com.smartinventory.inventory.config;

import com.smartinventory.inventory.model.RoleEntity;
import com.smartinventory.inventory.model.UserEntity;
import com.smartinventory.inventory.repository.RoleRepository;
import com.smartinventory.inventory.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

/**
 * Initializes essential security roles and baseline administrative user accounts
 * required for system authentication and authorization at startup.
 */
@Configuration
public class SecurityDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(SecurityDataInitializer.class);

    @Value("${app.security.default-admin-password:admin123}")
    private String adminDefaultPassword;

    @Value("${app.security.default-manager-password:manager123}")
    private String managerDefaultPassword;

    @Value("${app.security.default-operator-password:operator123}")
    private String operatorDefaultPassword;

    @Value("${app.security.default-viewer-password:viewer123}")
    private String viewerDefaultPassword;

    @Bean
    public CommandLineRunner initializeSecurityData(
            @org.springframework.context.annotation.Lazy RoleRepository roleRepository,
            @org.springframework.context.annotation.Lazy UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            // Warn if default credentials are detected in the environment
            if ("admin123".equals(adminDefaultPassword) || "manager123".equals(managerDefaultPassword)
                    || "operator123".equals(operatorDefaultPassword) || "viewer123".equals(viewerDefaultPassword)) {
                log.warn("⚠️ SECURITY WARNING: Default user credentials are active (admin/admin123, manager/manager123, operator/operator123, viewer/viewer123). Please override via environment variables APP_SECURITY_DEFAULT_ADMIN_PASSWORD, APP_SECURITY_DEFAULT_MANAGER_PASSWORD, APP_SECURITY_DEFAULT_OPERATOR_PASSWORD, APP_SECURITY_DEFAULT_VIEWER_PASSWORD before production deployment.");
            }

            // 1. Seed Roles
            String[] roles = {"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_ENTRY_OPERATOR", "ROLE_USER"};
            for (String roleName : roles) {
                if (roleRepository.findByName(roleName).isEmpty()) {
                    roleRepository.save(new RoleEntity(roleName, "Default " + roleName));
                }
            }

            // 2. Seed Baseline Users
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

            log.info("🔐 Security roles and default user accounts successfully initialized.");
        };
    }
}
