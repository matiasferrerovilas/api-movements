package api.m2.movements.events;

/**
 * El usuario le cambió la categoría a un movimiento. Lo escucha {@code CategoryRuleLearningHandler}
 * para aprender la regla del comercio.
 */
public record CategoryCorrectedEvent(Long workspaceId, String movementDescription, Long categoryId) {
}
