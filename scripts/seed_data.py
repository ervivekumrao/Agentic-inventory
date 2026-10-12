#!/usr/bin/env python3
"""
Smart Inventory Management System - Sample Data & Security Seeder Tool
Populates default users, roles, sample catalog and inventory data into the microservices via REST APIs.
Single source of truth for initial data setup.
"""

import urllib.request
import urllib.error
import base64
import json
import time
import sys
import os

CATALOG_HEALTH_URL = os.getenv("CATALOG_HEALTH_URL", "http://localhost:8082/actuator/health")
INVENTORY_HEALTH_URL = os.getenv("INVENTORY_HEALTH_URL", "http://localhost:8081/actuator/health")

CATALOG_URL = os.getenv("CATALOG_URL", "http://localhost:8082/api/catalog")
INVENTORY_URL = os.getenv("INVENTORY_URL", "http://localhost:8081/api/inventory")
ADMIN_USERS_URL = os.getenv("ADMIN_USERS_URL", "http://localhost:8081/api/admin/users")

CATEGORIES = ["Electronics", "Apparel", "Home & Kitchen", "Groceries", "Tools & Hardware", "Sports & Outdoors"]
WAREHOUSES = ["Warehouse-North", "Warehouse-South", "Warehouse-East", "Warehouse-West", "Central-Hub"]

SAMPLE_PRODUCTS = [
    ("LAPTOP-PRO-15", "Pro 15-inch Laptop", "High-performance laptop with 16GB RAM and 512GB SSD", "Electronics", 1299.99),
    ("SMARTPHONE-X", "SmartPhone X", "OLED display smartphone with advanced dual camera", "Electronics", 899.50),
    ("WIRELESS-BUDS", "Wireless Earbuds", "Active noise cancelling Bluetooth earbuds", "Electronics", 149.99),
    ("MECHANICAL-KEYBOARD", "RGB Mechanical Keyboard", "Tactile switches with customizable RGB backlight", "Electronics", 89.99),
    ("4K-MONITOR", "27-inch 4K Monitor", "IPS panel UHD monitor with HDR support", "Electronics", 349.00),

    ("DENIM-JACKET", "Classic Denim Jacket", "100% cotton vintage wash denim jacket", "Apparel", 79.99),
    ("RUNNING-SHOES", "AirStride Running Shoes", "Lightweight breathable athletic running shoes", "Apparel", 119.50),
    ("MERINO-SWEATER", "Merino Wool Sweater", "Warm and soft premium wool sweater", "Apparel", 89.00),
    ("COTTON-HOODIE", "Fleece Pullover Hoodie", "Comfortable everyday wear hoodie", "Apparel", 55.00),
    ("LEATHER-BELT", "Genuine Leather Belt", "Durable leather belt with metallic buckle", "Apparel", 39.99),

    ("BLENDER-PRO", "High-Speed Blender", "1200W professional blender for smoothies and soups", "Home & Kitchen", 129.99),
    ("COFFEE-MAKER", "Espresso Machine", "15-bar pump espresso and cappuccino maker", "Home & Kitchen", 199.99),
    ("AIR-FRYER", "Digital Air Fryer 5.8L", "Oil-less healthy air fryer with touch control", "Home & Kitchen", 109.99),
    ("CHEF-KNIFE", "Damascus Chef Knife", "8-inch high carbon steel professional kitchen knife", "Home & Kitchen", 85.00),
    ("CERAMIC-MUG-SET", "Ceramic Coffee Mug Set", "Set of 4 handcrafted ceramic mugs", "Home & Kitchen", 32.50),

    ("ORGANIC-HONEY", "Raw Organic Honey", "100% pure unfiltered raw honey 32oz", "Groceries", 18.99),
    ("ESPRESSO-BEANS", "Artisan Espresso Coffee Beans", "Dark roast whole bean coffee 1kg", "Groceries", 24.50),
    ("ALMOND-BUTTER", "Natural Creamy Almond Butter", "No added sugar or palm oil", "Groceries", 12.99),
    ("GREEN-TEA", "Japanese Sencha Green Tea", "Organic loose leaf green tea 250g", "Groceries", 15.00),
    ("DARK-CHOCOLATE", "70% Dark Chocolate Bar", "Artisan single-origin dark chocolate", "Groceries", 4.99),

    ("CORDLESS-DRILL", "20V Cordless Drill Driver", "Brushless motor drill with 2 batteries and charger", "Tools & Hardware", 149.99),
    ("TOOL-SET", "150-Piece Mechanics Tool Set", "Chrome vanadium steel socket and wrench set", "Tools & Hardware", 129.00),
    ("LED-WORK-LIGHT", "Rechargeable LED Work Light", "5000 lumens portable flood light", "Tools & Hardware", 59.99),
    ("DIGITAL-MULTIMETER", "Auto-Ranging Digital Multimeter", "True RMS electrical tester meter", "Tools & Hardware", 39.99),
    ("STAINLESS-SCREWS", "Assorted Stainless Screws Box", "500-piece wood and metal screw assortment", "Tools & Hardware", 24.99),

    ("YOGA-MAT", "Eco-Friendly Yoga Mat", "Non-slip thick exercise and yoga mat with strap", "Sports & Outdoors", 29.99),
    ("CAMPING-TENT", "4-Person Waterproof Camping Tent", "Easy setup dome tent for family camping", "Sports & Outdoors", 179.99),
    ("HIKING-BACKPACK", "50L Hiking Backpack", "Ergonomic internal frame backpack with rain cover", "Sports & Outdoors", 109.00),
    ("INSULATED-FLASK", "Stainless Steel Insulated Flask", "Keeps drinks cold for 24h or hot for 12h", "Sports & Outdoors", 34.99),
    ("BIKE-HELMET", "Commuter Cycling Helmet", "Adjustable lightweight bicycle helmet", "Sports & Outdoors", 49.99)
]

def make_request(url, method, data=None, headers=None):
    if headers is None:
        headers = {"Content-Type": "application/json"}

    req_data = json.dumps(data).encode("utf-8") if data else None
    req = urllib.request.Request(url, data=req_data, headers=headers, method=method)

    try:
        with urllib.request.urlopen(req) as response:
            return response.status, json.loads(response.read().decode("utf-8") or "{}")
    except urllib.error.HTTPError as e:
        body = {}
        try:
            body = json.loads(e.read().decode("utf-8") or "{}")
        except:
            pass
        return e.code, body
    except Exception as e:
        return 500, {"error": str(e)}

def wait_for_services():
    print("⏳ Checking if microservices are reachable...")
    for attempt in range(60):
        cat_status, cat_body = make_request(CATALOG_HEALTH_URL, "GET")
        inv_status, inv_body = make_request(INVENTORY_HEALTH_URL, "GET")

        cat_up = cat_status == 200 and cat_body.get("status") == "UP"
        inv_up = inv_status == 200 and inv_body.get("status") == "UP"

        if cat_up and inv_up:
            print("🚀 All microservices are online and ready!")
            return True

        time.sleep(0.5)

    print("❌ Error: Microservices did not become ready in time.")
    print("   Please start them first using Docker Compose:")
    print("   docker compose -f infra/docker-compose.dev.yml up --build -d")
    return False

def seed_users_and_roles(headers):
    print("\n🔐 Seeding default users and roles into Inventory Service...")
    users_to_create = [
        {"username": "admin", "password": "admin123", "roles": ["ROLE_ADMIN"]},
        {"username": "manager", "password": "manager123", "roles": ["ROLE_MANAGER"]},
        {"username": "operator", "password": "operator123", "roles": ["ROLE_ENTRY_OPERATOR"]},
        {"username": "viewer", "password": "viewer123", "roles": ["ROLE_USER"]}
    ]

    for u in users_to_create:
        req_headers = {"Content-Type": "application/json"} if u["username"] == "admin" else headers
        status, body = make_request(ADMIN_USERS_URL, "POST", u, headers=req_headers)
        if status in [200, 201]:
            print(f" ✅ [Seeded User] Username: {u['username']} | Roles: {u['roles']}")
        elif status == 409:
            print(f" ⚠️ [User Exists] Username: {u['username']}")
        else:
            print(f" ⚠️ [User Seed Info] Username: {u['username']} (Status: {status})")

def seed_data():
    if not wait_for_services():
        sys.exit(1)

    # Use default admin credentials (admin / admin123)
    auth_str = base64.b64encode(b"admin:admin123").decode("utf-8")
    inv_headers = {
        "Content-Type": "application/json",
        "Authorization": f"Basic {auth_str}"
    }

    # 1. Seed Users and Roles
    seed_users_and_roles(inv_headers)

    print(f"\n🌱 Bulk seeding {len(SAMPLE_PRODUCTS)} sample catalog products and inventory items...")

    catalog_payloads = []
    inventory_payloads = []

    for i, (sku, name, desc, category, price) in enumerate(SAMPLE_PRODUCTS):
        catalog_payloads.append({
            "sku": sku,
            "name": name,
            "description": desc,
            "category": category,
            "price": price,
            "attributes": {"brand": "SmartBrand", "warranty": "1 Year"}
        })

        initial_qty = (i * 13) % 85 + 5
        threshold = 10
        warehouse = WAREHOUSES[i % len(WAREHOUSES)]

        inventory_payloads.append({
            "sku": sku,
            "quantity": initial_qty,
            "reorderThreshold": threshold,
            "warehouseLocation": warehouse
        })

    cat_status, cat_res = make_request(CATALOG_URL + "/bulk", "POST", catalog_payloads)
    inv_status, inv_res = make_request(INVENTORY_URL + "/bulk", "POST", inventory_payloads, headers=inv_headers)

    print(f" ✅ [Bulk Catalog Seeding] Status: {cat_status}")
    print(f" ✅ [Bulk Inventory Seeding] Status: {inv_status}")

    print(f"\n🎉 Successfully seeded sample data into Catalog & Inventory services via setup script!")
    print("👉 You can now view them via:")
    print("   - Catalog Swagger UI: http://localhost:8082/swagger-ui.html")
    print("   - Inventory Swagger UI: http://localhost:8081/swagger-ui.html (admin / admin123)")

if __name__ == "__main__":
    seed_data()
