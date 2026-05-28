package api.m2.movements.services.investments;

import api.m2.movements.annotations.RequiresMembership;
import api.m2.movements.entities.investments.Investment;
import api.m2.movements.enums.MembershipDomain;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.mappers.InvestmentMapper;
import api.m2.movements.records.investments.InvestmentRecord;
import api.m2.movements.records.investments.InvestmentToAdd;
import api.m2.movements.records.investments.InvestmentToUpdate;
import api.m2.movements.repositories.CurrencyRepository;
import api.m2.movements.repositories.InvestmentRepository;
import api.m2.movements.repositories.InvestmentTypeRepository;
import api.m2.movements.services.publishing.websockets.InvestmentPublishServiceWebSocket;
import api.m2.movements.services.user.UserService;
import api.m2.movements.services.workspaces.WorkspaceContextService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class InvestmentAddService {

    private final InvestmentRepository investmentRepository;
    private final InvestmentMapper investmentMapper;
    private final InvestmentTypeRepository investmentTypeRepository;
    private final CurrencyRepository currencyRepository;
    private final UserService userService;
    private final WorkspaceContextService workspaceContextService;
    private final InvestmentPublishServiceWebSocket investmentPublishService;

    @Transactional
    public InvestmentRecord add(@Valid InvestmentToAdd dto) {
        var workspace = workspaceContextService.getActiveWorkspace();
        var user = userService.getAuthenticatedUser();
        var currency = currencyRepository.findBySymbol(dto.currencySymbol())
                .orElseThrow(() -> new EntityNotFoundException("Moneda no encontrada: " + dto.currencySymbol()));
        var investmentType = investmentTypeRepository.findById(dto.investmentTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Tipo de inversión no encontrado: " + dto.investmentTypeId()));

        var investment = Investment.builder()
                .amount(dto.amount())
                .startDate(dto.startDate())
                .endDate(dto.endDate())
                .description(dto.description())
                .investmentType(investmentType)
                .currency(currency)
                .workspace(workspace)
                .owner(user)
                .build();

        var record = investmentMapper.toRecord(investmentRepository.save(investment));
        investmentPublishService.publishInvestmentAdded(record);
        log.info("Inversión creada: id={}", record.id());
        return record;
    }

    @Transactional
    @RequiresMembership(domain = MembershipDomain.INVESTMENT, idParamIndex = 1)
    public InvestmentRecord update(@Valid InvestmentToUpdate dto, Long id) {
        var investment = investmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inversión no encontrada: " + id));

        investmentMapper.updateInvestment(dto, investment);
        this.applyFkUpdates(dto, investment);

        var record = investmentMapper.toRecord(investmentRepository.save(investment));
        investmentPublishService.publishInvestmentUpdated(record);
        log.info("Inversión actualizada: id={}", id);
        return record;
    }

    @Transactional
    @RequiresMembership(domain = MembershipDomain.INVESTMENT)
    public void delete(Long id) {
        var investment = investmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inversión no encontrada: " + id));

        var record = investmentMapper.toRecord(investment);
        investmentRepository.delete(investment);
        investmentPublishService.publishInvestmentDeleted(record);
        log.info("Inversión eliminada: id={}", id);
    }

    private void applyFkUpdates(InvestmentToUpdate dto, Investment investment) {
        if (dto.currencySymbol() != null) {
            var currency = currencyRepository.findBySymbol(dto.currencySymbol())
                    .orElseThrow(() -> new EntityNotFoundException("Moneda no encontrada: " + dto.currencySymbol()));
            investment.setCurrency(currency);
        }
        if (dto.investmentTypeId() != null) {
            var type = investmentTypeRepository.findById(dto.investmentTypeId())
                    .orElseThrow(() -> new EntityNotFoundException("Tipo de inversión no encontrado: " + dto.investmentTypeId()));
            investment.setInvestmentType(type);
        }
    }
}
