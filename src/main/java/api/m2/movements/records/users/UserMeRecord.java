package api.m2.movements.records.users;

public record UserMeRecord(
        Long id,
        String email,
        String givenName,
        String familyName,
        boolean isFirstLogin,
        String userType,
        boolean hasSeenTour
) { }
