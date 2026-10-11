package api.m2.movements.records.workspaces;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;

@RegisterReflectionForBinding
public record WorkspaceDetail(Long id, String name, int membersCount, boolean isDefault) {
}
