package api.m2.movements.services.currencies;

import api.m2.movements.entities.WorkspaceCurrency;
import api.m2.movements.entities.commons.Currency;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.exceptions.PermissionDeniedException;
import api.m2.movements.records.currencies.CurrencyToAdd;
import api.m2.movements.records.currencies.WorkspaceCurrencyRecord;
import api.m2.movements.repositories.WorkspaceCurrencyRepository;
import api.m2.movements.services.workspaces.WorkspaceContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceCurrencyService {

    private final WorkspaceCurrencyRepository workspaceCurrencyRepository;
    private final CurrencyAddService currencyAddService;
    private final WorkspaceContextService workspaceContextService;

    public List<WorkspaceCurrencyRecord> getWorkspaceCurrencies() {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();
        return workspaceCurrencyRepository.findByWorkspaceId(workspaceId).stream()
                .map(this::toRecord)
                .toList();
    }

    @Transactional
    public WorkspaceCurrencyRecord addCurrency(CurrencyToAdd dto) {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();
        var currency = currencyAddService.addCurrency(dto.symbol(), dto.description());
        var workspaceCurrency = this.resolveWorkspaceCurrency(workspaceId, currency);
        return this.toRecord(workspaceCurrency);
    }

    @Transactional
    public void addDefaultCurrencies(Long workspaceId) {
        currencyAddService.getDefaultCurrencies()
                .forEach(currency -> this.resolveWorkspaceCurrency(workspaceId, currency));
    }

    @Transactional
    public void ensureCurrencyInWorkspace(Long workspaceId, Currency currency) {
        this.resolveWorkspaceCurrency(workspaceId, currency);
    }

    @Transactional
    public void deleteCurrency(Long workspaceCurrencyId) {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();
        var workspaceCurrency = workspaceCurrencyRepository.findById(workspaceCurrencyId)
                .orElseThrow(() -> new EntityNotFoundException("Moneda no encontrada"));

        if (!workspaceCurrency.getWorkspaceId().equals(workspaceId)) {
            throw new PermissionDeniedException("No tenés permiso para eliminar esta moneda");
        }

        workspaceCurrencyRepository.delete(workspaceCurrency);
    }

    private WorkspaceCurrency resolveWorkspaceCurrency(Long workspaceId, Currency currency) {
        return workspaceCurrencyRepository.findByWorkspaceIdAndCurrencyId(workspaceId, currency.getId())
                .orElseGet(() -> workspaceCurrencyRepository.save(
                        WorkspaceCurrency.builder()
                                .workspaceId(workspaceId)
                                .currency(currency)
                                .build()));
    }

    private WorkspaceCurrencyRecord toRecord(WorkspaceCurrency workspaceCurrency) {
        var currency = workspaceCurrency.getCurrency();
        return new WorkspaceCurrencyRecord(
                workspaceCurrency.getId(),
                currency.getSymbol(),
                currency.getDescription(),
                true
        );
    }
}
