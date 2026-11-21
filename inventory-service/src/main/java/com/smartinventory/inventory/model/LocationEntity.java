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

package com.smartinventory.inventory.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "locations", indexes = {
    @Index(name = "idx_location_code", columnList = "code", unique = true)
})
public class LocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    @NotBlank(message = "Location code is mandatory")
    private String code;

    @Column(nullable = false, length = 128)
    @NotBlank(message = "Location name is mandatory")
    private String name;

    @Column(length = 64)
    private String type = "WAREHOUSE";

    private Integer capacity = 10000;

    public LocationEntity() {}

    public LocationEntity(String code, String name, String type, Integer capacity) {
        this.code = code;
        this.name = name;
        this.type = type != null ? type : "WAREHOUSE";
        this.capacity = capacity != null ? capacity : 10000;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
}
