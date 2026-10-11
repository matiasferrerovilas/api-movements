package api.m2.movements.services.category.rules;

import java.util.Map;
import java.util.Optional;

/**
 * Foto de las reglas de un workspace, armada una vez por import para no consultar la base por
 * cada línea del extracto. Busca primero la clave exacta del comercio y, si no hay, la primera
 * palabra significativa (ej. una regla "LIDL VALENCIA" también resuelve "LIDL MADRID").
 */
public class CategoryMatcher {

    private final MerchantKeyNormalizer normalizer;
    private final Map<String, String> categoryByKey;
    private final Map<String, String> categoryByHead;

    public CategoryMatcher(MerchantKeyNormalizer normalizer, Map<String, String> categoryByKey, Map<String, String> categoryByHead) {
        this.normalizer = normalizer;
        this.categoryByKey = categoryByKey;
        this.categoryByHead = categoryByHead;
    }

    /** Descripción de la categoría que corresponde a esta descripción de movimiento, si hay regla. */
    public Optional<String> match(String description) {
        var key = normalizer.keyOf(description);
        if (key.isEmpty()) {
            return Optional.empty();
        }
        var exact = categoryByKey.get(key);
        if (exact != null) {
            return Optional.of(exact);
        }
        var head = normalizer.headOf(description);
        return head.isEmpty() ? Optional.empty() : Optional.ofNullable(categoryByHead.get(head));
    }
}
