package api.m2.movements.movements.projections;

public interface WorkspaceSummaryProjection {
    Long getAccountId();

    String getAccountName();

    Long getOwnerId();

    String getOwnerEmail();

    Long getMembersCount();
}
