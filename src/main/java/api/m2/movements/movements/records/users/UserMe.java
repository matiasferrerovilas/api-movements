package api.m2.movements.movements.records.users;

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
            List<String> userRole
    ) { }
}
