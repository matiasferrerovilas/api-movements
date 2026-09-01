-- liquibase formatted sql
-- changeset mati:058_alter_table_movements_add_last_credit_payment

ALTER TABLE movements
    ADD COLUMN last_credit_payment DATE NULL;

-- Backfill para movimientos CREDITO existentes: fecha del movimiento + (cuotas_totales -
-- cuota_actual) meses, la fecha de la última cuota del plan. Usa cuota_actual en vez de asumir
-- siempre "-1" porque no todas las filas existentes están necesariamente en la cuota 1 (import de
-- PDF viejo, carga manual a mitad de plan) — con cuota_actual=1 el resultado es el mismo
-- "cuotas_totales - 1" que si siempre arrancara ahí.
UPDATE movements
SET last_credit_payment = DATE_ADD(date, INTERVAL (cuotas_totales - cuota_actual) MONTH)
WHERE type = 'CREDITO'
  AND last_credit_payment IS NULL
  AND cuota_actual IS NOT NULL
  AND cuotas_totales IS NOT NULL;
