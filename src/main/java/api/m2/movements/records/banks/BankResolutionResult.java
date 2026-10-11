package api.m2.movements.records.banks;

import api.m2.movements.entities.commons.Bank;
import api.m2.movements.services.banks.BankAddService;

/**
 * Resultado de resolver el banco de un usuario: el banco (existente o recién creado) y si hubo que
 * asociarlo al usuario. Es un valor interno de {@link BankAddService}, por eso es de paquete.
 */
public record BankResolutionResult(Bank bank, boolean wasAdded) {
}
