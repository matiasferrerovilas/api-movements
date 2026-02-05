package api.m2.movements.projections;

public interface AccountSummaryProjection {
    Long getAccountId();

    String getAccountName();

    Long getOwnerId();

    String getOwnerEmail();

    Long getMembersCount();
}
