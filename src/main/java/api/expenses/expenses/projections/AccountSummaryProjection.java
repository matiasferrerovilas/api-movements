package api.expenses.expenses.projections;

public interface AccountSummaryProjection {
    Long getAccountId();

    String getAccountName();

    Long getOwnerId();

    String getOwnerEmail();

    Long getMembersCount();
}
