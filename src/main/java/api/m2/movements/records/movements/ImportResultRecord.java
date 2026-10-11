package api.m2.movements.records.movements;

/**
 * Resultado de importar un extracto: cuántos movimientos se guardaron y cuántos se saltearon
 * porque ya estaban cargados.
 */
public record ImportResultRecord(int imported, int duplicated) {
}
