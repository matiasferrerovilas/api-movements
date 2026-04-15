package api.m2.movements.records.banks;

import api.m2.movements.entities.Bank;

public record BankResolutionResult(Bank bank, boolean wasAdded) {
}
