package api.m2.movements.helpers;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ParserRegistry {
    private final List<PdfExtractorHelper> parsers;
    private final Map<String, PdfExtractorHelper> registry = new HashMap<>();

    @PostConstruct
    void init() {
        for (PdfExtractorHelper parser : parsers) {
            var existing = registry.put(parser.getBank(), parser);
            if (existing != null) {
                throw new IllegalStateException("Duplicate parser found for bank: " + parser.getBank());
            }
        }
    }

    public PdfExtractorHelper getParser(String bank) {
       return Optional.ofNullable(registry.get(bank))
                .orElseThrow(() -> new IllegalArgumentException("No parser registered for bank: " + bank));
    }
}
