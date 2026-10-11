package api.m2.movements.records.workspaces;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;

@RegisterReflectionForBinding
public record WorkspaceBaseRecord(Long id, String name) {
}
