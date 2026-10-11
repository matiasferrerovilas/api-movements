package api.m2.movements.events;

public record MovementDeletedEvent(Long movementId, Long workspaceId) {
}
