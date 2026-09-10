-- liquibase formatted sql

-- changeset mati:060-drop-table-monthly-summary-snapshot
-- El resumen mensual pasó a calcularse siempre on-demand (SUM contra movements). El snapshot
-- quedaba desactualizado en cuanto se cargaba un movimiento después de generarlo y el volumen es
-- chico, así que se elimina la tabla junto con su job.
DROP TABLE IF EXISTS monthly_summary_snapshot;
