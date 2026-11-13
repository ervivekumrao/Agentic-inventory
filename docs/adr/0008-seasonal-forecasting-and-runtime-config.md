# ADR 0008: Seasonal Holt-Winters Forecasting & Zero-Downtime Dynamic Runtime Configuration

## Status
Approved

## Context
Demand forecasting using simple 1st-degree polynomial regression (`np.polyfit(1)`) fails on seasonal, cyclical, or non-linear historical demand. Additionally, operational parameters (such as safety stock $Z$-scores, default reorder multipliers, and context limits) previously required container restarts to change.

## Decision
We enhance `forecasting-service` with **Holt-Winters Seasonal Demand Forecasting** and `inventory-service` with **Zero-Downtime Runtime Configuration**:

1. **Seasonal Demand Modeling**: `/forecast` evaluates historical series length and variance. When history $N \ge 14$, it applies Triple Exponential Smoothing (Holt-Winters additive/multiplicative model) to capture trend and seasonal cycles.
2. **Robust Alphanumeric SKU NER**: Named Entity Recognition (`_extract_entities`) dynamically matches non-hyphenated alphanumeric SKUs (`SKU12345`, `PROD_4421`) against live database context.
3. **Dynamic Configuration REST Engine**: `RuntimeConfigController` (`/api/admin/config`) provides runtime REST endpoints to view and update system parameters dynamically without restarting containers or interrupting active connections.

## Consequences
### Positive
- **Accurate Seasonal Reorder Points**: Reorder recommendations account for upcoming seasonal demand spikes and off-season lulls.
- **Zero-Downtime Administration**: Operations teams can adjust threshold rules and safety stock multipliers instantly at runtime.

### Negative
- **Increased Computational Complexity**: Seasonal forecasting requires minimum historical data points ($N \ge 14$) to compute valid seasonal indices.
