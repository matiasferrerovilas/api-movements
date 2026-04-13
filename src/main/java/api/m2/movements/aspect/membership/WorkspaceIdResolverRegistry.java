package api.m2.movements.aspect.membership;

import api.m2.movements.enums.MembershipDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Registry que agrupa todos los WorkspaceIdResolver.
 * Spring inyecta automaticamente todas las implementaciones de WorkspaceIdResolver.
 */
@Component
@RequiredArgsConstructor
public class WorkspaceIdResolverRegistry {

    private final List<WorkspaceIdResolver> resolvers;

    /**
     * Resuelve el workspaceId delegando al resolver apropiado segun el dominio.
     *
     * @param domain   el dominio de la entidad
     * @param entityId el ID de la entidad
     * @return el ID del workspace
     * @throws IllegalArgumentException si no hay resolver para el dominio
     */
    public Long resolve(MembershipDomain domain, Long entityId) {
        return resolvers.stream()
                .filter(r -> r.supports(domain))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay resolver registrado para el dominio: " + domain))
                .resolveWorkspaceId(entityId);
    }
}
