# Agentic Inventory - Brand Guidelines & Logo System

Official design specification and logo assets guide for **Agentic Inventory / Smart Inventory System**.

---

## 🎨 1. Brand Identity & Concept Strategy

The **Agentic Inventory** identity bridges physical enterprise inventory management with intelligent AI telemetry and demand forecasting.

### Official Mark: **Interlocking Monogram ('A' & 'I')**
* **Mark Type**: Geometric Monogram Symbol
* **Geometry**: An interlocking 'A' (Agentic) and 'I' (Inventory) monogram featuring a central pencil shaft with a pure white cone tip (`#FFFFFF`) and a solid black lead point (`#000000`).
* **Visual Meaning**:
  * **Outer Apex 'A'**: Agentic AI workspace, precision architecture, and enterprise analytics.
  * **Central Axis 'I'**: Inventory control, stock telemetry, and immutable transaction audit logs.
  * **White Cone & Black Lead Point**: Craft, drafting, and intelligent planning.

---

## 🌈 2. Color Palette & Dark / Light Theme Specs

| Palette Component | HEX Code | RGB | Role / Usage |
| :--- | :--- | :--- | :--- |
| **Primary Sky / AI Glow** | `#38BDF8` | `56, 189, 248` | Primary AI accent, active states, key brand elements |
| **Telemetry Blue** | `#0284C7` | `2, 132, 199` | Top facet gradient, primary buttons, light-theme accent |
| **Deep Ocean Blue** | `#2563EB` | `37, 99, 235` | Left facet gradient, links, focus indicators |
| **Neural Indigo** | `#6366F1` | `99, 102, 241` | Right facet gradient, machine learning indicators |
| **Dark Theme Body (Dark Mode)** | `#0B0F19` | `11, 15, 25` | Default dark workspace background |
| **Dark Theme Surface** | `#111827` | `17, 24, 39` | Dark mode surface & card container |
| **Light Theme Body (Light Mode)** | `#F8FAFC` | `248, 250, 252` | Default light workspace background |
| **Light Theme Surface** | `#FFFFFF` | `255, 255, 255` | Light mode surface & card container |
| **Primary Text (Dark Mode)** | `#F8FAFC` | `248, 250, 252` | Headings & primary typography on dark |
| **Primary Text (Light Mode)** | `#0F172A` | `15, 23, 42` | Headings & primary typography on light |

---

## 📐 3. Logo Variants & File Inventory

All production vector and bitmap assets are generated in both `docs/assets/logo/` and `inventory-service/src/main/resources/static/assets/logo/`:

### Vector Masters (SVG)
* **`logo-symbol.svg`**: Standalone vector monogram symbol (256×256).
* **`logo-dark.svg`**: Horizontal lockup for Dark Theme backgrounds (`#0B0F19`).
* **`logo-light.svg`**: Horizontal lockup for Light Theme backgrounds (`#FFFFFF`).
* **`logo-stacked-dark.svg`**: Centered stacked lockup for Dark Theme.
* **`logo-stacked-light.svg`**: Centered stacked lockup for Light Theme.
* **`logo-mono-black.svg`**: Single-color black master (light backgrounds).
* **`logo-mono-white.svg`**: Single-color white master (dark backgrounds).
* **`logo-small-16.svg`**: 16px favicon cut.
* **`favicon.svg`**: Adaptive vector favicon with `@media (prefers-color-scheme: dark/light)` support.

### Web & App Icon Pack
* **`favicon.ico`**: Multi-resolution icon container (16×16, 32×32).
* **`favicon-16x16.png`**: 16×16 browser favicon.
* **`favicon-32x32.png`**: 32×32 browser favicon.
* **`apple-touch-icon.png`**: 180×180 iOS home screen icon.
* **`android-chrome-192x192.png`**: 192×192 Web App manifest icon.
* **`android-chrome-512x512.png`**: 512×512 Web App splash icon.
* **`logo-symbol-512.png`**: Centered 512×512 symbol PNG.
* **`logo-dark.png`** & **`logo-light.png`**: Horizontal lockup PNGs.
* **`site.webmanifest`**: Progressive Web App manifest file.

### Open Graph Social Cards (1200×630)
* **`og-image-dark.png`**: 1200×630 social media preview card for Dark Mode.
* **`og-image-light.png`**: 1200×630 social media preview card for Light Mode.

---

## ⚡ 4. Implementation Snippets for Web UIs

### Standard Web Head Integration
```html
<!-- Favicons & App Icons -->
<link rel="icon" type="image/svg+xml" href="/assets/logo/favicon.svg">
<link rel="icon" type="image/png" sizes="32x32" href="/assets/logo/favicon-32x32.png">
<link rel="icon" type="image/png" sizes="16x16" href="/assets/logo/favicon-16x16.png">
<link rel="apple-touch-icon" sizes="180x180" href="/assets/logo/apple-touch-icon.png">
<link rel="manifest" href="/assets/logo/site.webmanifest">

<!-- OpenGraph Metadata -->
<meta property="og:title" content="Smart Inventory - Agentic AI Workspace">
<meta property="og:description" content="Real-time stock telemetry, dynamic catalog management, and AI-driven demand forecasting.">
<meta property="og:image" content="/assets/logo/og-image-dark.png">
```

### Dynamic Theme Switcher Header Image
```html
<picture>
  <source srcset="/assets/logo/logo-dark.svg" media="(prefers-color-scheme: dark)">
  <img src="/assets/logo/logo-light.svg" alt="Agentic Inventory Logo" height="38">
</picture>
```

---

## 🛡️ 5. Clear Space & Usage Rules

* **Clear Space**: Maintain a minimum boundary around the logo equal to 50% of the symbol height (`0.5x`).
* **Minimum Size**:
  * Symbol: Minimum 16px.
  * Horizontal Lockup: Minimum height 24px.
* **Do Not**:
  * Stretch, skew, or rotate the logo geometry.
  * Apply low-contrast colors or drop shadows behind the mark.
  * Change the font family or letter-spacing of the wordmark.
