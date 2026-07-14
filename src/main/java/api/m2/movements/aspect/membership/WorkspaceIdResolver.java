package api.m2.movements.aspect.membership;

import api.m2.movements.enums.MembershipDomain;

/**
 * Resolver que extrae el workspaceId de una entidad dado su ID.
 * Cada dominio (MOVEMENT, INCOME, SUBSCRIPTION, BUDGET) tiene su propia implementacion.
 */
public interface WorkspaceIdResolver {

    /**
     * Indica si este resolver soporta el dominio dado.
     *
     * @param domain el dominio a verificar
     * @return true si este resolver maneja el dominio
     */
    boolean supports(MembershipDomain domain);

    /**
     * Resuelve el workspaceId de la entidad.
     *
     * @param entityId el ID de la entidad
     * @return el ID del workspace al que pertenece la entidad
     * @throws api.m2.movements.exceptions.EntityNotFoundException si la entidad no existe
     */
    Long resolveWorkspaceId(Long entityId);
}
