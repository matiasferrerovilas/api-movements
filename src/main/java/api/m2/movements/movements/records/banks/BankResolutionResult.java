package api.m2.movements.movements.records.banks;

import api.m2.movements.movements.entities.commons.Bank;

public record BankResolutionResult(Bank bank, boolean wasAdded) {
}
