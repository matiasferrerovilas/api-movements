package api.m2.movements.exceptions;

public record ErrorResponse(String statusCode, String title, String detail) {
}
