package api.m2.movements.investment.records;

import api.m2.movements.investment.enums.InvestmentCategory;

public record InvestmentTypeRecord(Long id, String name, String iconName, String iconColor, Long workspaceId, InvestmentCategory category) {
}
