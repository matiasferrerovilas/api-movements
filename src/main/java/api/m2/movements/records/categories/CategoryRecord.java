package api.m2.movements.records.categories;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;

@RegisterReflectionForBinding
public record CategoryRecord(
        Long id,
        String description,
        boolean isActive,
        boolean isDeletable,
        String iconName,
        String iconColor
) {
}
