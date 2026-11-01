--liquibase formatted sql

--changeset mati:065-add-movement-cash-date
-- Fecha en la que el movimiento sale realmente de la cuenta. El CREDITO se paga con el resumen de
-- la tarjeta, siempre el mes siguiente a su fecha; el resto cuenta el mismo día. Los totales de
-- plata gastada (resumen mensual, balance, evolución, proyecciones) agrupan por esta fecha. Los
-- presupuestos NO: topean el gasto al comprar, así que siguen por `date`.
ALTER TABLE movements
    ADD COLUMN cash_date DATE
        GENERATED ALWAYS AS (CASE WHEN `type` = 'CREDITO' THEN DATE_ADD(`date`, INTERVAL 1 MONTH) ELSE `date` END) STORED;
CREATE INDEX idx_movements_workspace_cash_date ON movements (workspace_id, cash_date);
