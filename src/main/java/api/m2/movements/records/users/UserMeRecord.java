package api.m2.movements.records.users;

public record UserMeRecord(
        Long id,
        String email,
        boolean isFirstLogin,
        String userType
) { }
