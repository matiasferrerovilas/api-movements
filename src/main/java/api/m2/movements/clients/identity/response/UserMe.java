package api.m2.movements.clients.identity.response;

import api.m2.movements.enums.WorkspaceRole;
import api.m2.movements.records.balance.PendingMonthlySummary;

import java.util.List;

public record UserMe(
        Long id,
        String email,
        String givenName,
        String familyName,
        String userType,
        Metadata metadata
) {
    public record Metadata(
            boolean isFirstLogin,
            boolean hasSeenTour,
            List<String> userRole,
            /** The caller's role in the workspace passed as {@code ?workspaceId=} — null if no
             * workspaceId was given, or if the caller isn't a member of that workspace. Distinct
             * from {@code userRole}, which is the global suite-wide role (ADMIN/FAMILY/GUEST). */
            WorkspaceRole workspaceRole,
            /** Set by api-movements (not api-identity) when serving its own {@code /v1/users/me}
             * gateway: the last closed month the caller hasn't dismissed yet in their default
             * workspace, or null. See {@code MonthCloseService}. */
            PendingMonthlySummary pendingMonthlySummary
    ) {
        /** api-identity's response has no {@code pendingMonthlySummary}; this is the shape the
         * IdentityClient deserializes into, and what callers that don't care about the month-close
         * gate keep using. api-movements' UserController fills it in for its own gateway response. */
        public Metadata(boolean isFirstLogin, boolean hasSeenTour, List<String> userRole, WorkspaceRole workspaceRole) {
            this(isFirstLogin, hasSeenTour, userRole, workspaceRole, null);
        }
    }
}
