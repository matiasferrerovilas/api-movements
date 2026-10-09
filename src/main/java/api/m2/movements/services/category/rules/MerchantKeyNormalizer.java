package api.m2.movements.services.category.rules;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Reduce la descripción de un movimiento a una clave de comercio estable, para que "Mercadona
 * Valencia 1234" y "MERCADONA VALENCIA" caigan en la misma regla.
 *
 * <p>Se pasa a mayúsculas sin acentos, se descartan números sueltos, conectores y ruido de tarjeta,
 * y se quedan las dos primeras palabras significativas. Palabras genéricas como TRANSFERENCIA,
 * RECIBO o BIZUM se conservan en la clave pero no cuentan como significativas: así "Bizum De Ana"
 * y "Bizum De Juan" no comparten regla.
 */
@Component
public class MerchantKeyNormalizer {

    public static final int MAX_KEY_LENGTH = 60;
    private static final int SIGNIFICANT_TOKENS = 2;
    private static final int MIN_TOKEN_LENGTH = 2;

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^A-Z0-9]+");
    private static final Pattern ONLY_DIGITS = Pattern.compile("^\\d+$");

    private static final Set<String> NOISE = Set.of(
            "DE", "DEL", "LA", "LAS", "LOS", "EL", "EN", "CON", "POR", "PARA", "AL", "Y",
            "SL", "SLU", "SA", "ES", "ESP", "ESPANA", "WWW", "COM",
            "COMPRA", "PAGO", "MOVIL", "TARJ", "TARJETA", "CONTACTLESS", "INTERNET", "TRANSACCION", "NORMAL");

    private static final Set<String> GENERIC = Set.of(
            "TRANSFERENCIA", "INMEDIATA", "FAVOR", "RECIBO", "BIZUM", "TRASPASO", "DEVOLUCION",
            "ADEUDO", "CONCEPTO", "ORDEN", "EMITIDA", "RECIBIDA");

    /** Clave completa del comercio, o cadena vacía si la descripción no tiene nada utilizable. */
    public String keyOf(String description) {
        return String.join(" ", this.keyTokens(description));
    }

    /**
     * Primera palabra significativa de la clave (ej. "LIDL" para "LIDL VALENCIA"). Sirve de respaldo
     * cuando la clave exacta no tiene regla. Vacía si la clave empieza con una palabra genérica.
     */
    public String headOf(String description) {
        var tokens = this.keyTokens(description);
        if (tokens.isEmpty() || GENERIC.contains(tokens.getFirst())) {
            return "";
        }
        return tokens.getFirst();
    }

    private List<String> keyTokens(String description) {
        if (description == null || description.isBlank()) {
            return List.of();
        }
        String plain = DIACRITICS.matcher(Normalizer.normalize(description, Normalizer.Form.NFD)).replaceAll("");
        String upper = NON_ALPHANUMERIC.matcher(plain.toUpperCase(Locale.ROOT)).replaceAll(" ").trim();

        var result = new ArrayList<String>();
        int significant = 0;
        int length = 0;
        for (String token : Arrays.asList(upper.split(" "))) {
            if (significant == SIGNIFICANT_TOKENS) {
                break;
            }
            if (token.length() < MIN_TOKEN_LENGTH || ONLY_DIGITS.matcher(token).matches() || NOISE.contains(token)) {
                continue;
            }
            length += token.length() + (result.isEmpty() ? 0 : 1);
            if (length > MAX_KEY_LENGTH) {
                break;
            }
            result.add(token);
            if (!GENERIC.contains(token)) {
                significant++;
            }
        }
        return result;
    }
}
