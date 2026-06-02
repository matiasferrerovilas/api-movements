package api.m2.movements.helpers;

import api.m2.movements.exceptions.BusinessException;
import api.m2.movements.exceptions.ServiceException;
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
                throw new ServiceException("Duplicate parser found for bank: " + parser.getBank());
            }
        }
    }

    public PdfExtractorHelper getParser(String bank) {
       return Optional.ofNullable(registry.get(bank))
                .orElseThrow(() -> new BusinessException("No parser registered for bank: " + bank));
    }
}
