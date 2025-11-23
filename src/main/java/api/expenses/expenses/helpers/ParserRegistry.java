package api.expenses.expenses.helpers;

import api.expenses.expenses.enums.BanksEnum;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ParserRegistry {
    private final List<PdfExtractprHelper> parsers;
    private final Map<BanksEnum, PdfExtractprHelper> registry = new EnumMap<>(BanksEnum.class);

    @PostConstruct
    void init() {
        for (PdfExtractprHelper parser : parsers) {
            var existing = registry.put(parser.getBank(), parser);
            if (existing != null) {
                throw new IllegalStateException("Duplicate parser found for bank: " + parser.getBank());
            }
        }
    }

    public PdfExtractprHelper getParser(BanksEnum bank) {
       return Optional.ofNullable(registry.get(bank))
                .orElseThrow(() -> new IllegalArgumentException("No parser registered for bank: " + bank));
    }
}
