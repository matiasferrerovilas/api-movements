package api.m2.movements.projections;

import api.m2.movements.enums.AccountRole;

public interface MembershipSummaryProjection {
    Long getGroupId();

    String getGroupDescription();
    Long getMembershipId();

    boolean getIsDefault();

    AccountRole getRole();
}
