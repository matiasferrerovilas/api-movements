package api.m2.movements.movements.projections;

import api.m2.movements.movements.enums.WorkspaceRole;

public interface MembershipSummaryProjection {
    Long getWorkspaceId();

    String getWorkspaceName();
    Long getMembershipId();

    WorkspaceRole getRole();
}
