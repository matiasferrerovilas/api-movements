package api.m2.movements.projections;

import api.m2.movements.enums.WorkspaceRole;

public interface MembershipSummaryProjection {
    Long getWorkspaceId();

    String getWorkspaceName();
    Long getMembershipId();

    WorkspaceRole getRole();
}
