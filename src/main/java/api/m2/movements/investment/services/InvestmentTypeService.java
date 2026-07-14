package api.m2.movements.investment.services;

import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.exceptions.PermissionDeniedException;
import api.m2.movements.investment.entities.InvestmentType;
import api.m2.movements.investment.mappers.InvestmentTypeMapper;
import api.m2.movements.investment.records.InvestmentTypeRecord;
import api.m2.movements.investment.records.InvestmentTypeToAdd;
import api.m2.movements.investment.records.InvestmentTypeToUpdate;
import api.m2.movements.investment.repositories.InvestmentTypeRepository;
import api.m2.movements.services.workspaces.WorkspaceContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InvestmentTypeService {

    private final InvestmentTypeRepository investmentTypeRepository;
    private final InvestmentTypeMapper investmentTypeMapper;
    private final WorkspaceContextService workspaceContextService;

    public List<InvestmentTypeRecord> getByWorkspace() {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();
        return investmentTypeMapper.toRecordList(
                investmentTypeRepository.findByWorkspaceId(workspaceId));
    }

    @Transactional
    public InvestmentTypeRecord add(InvestmentTypeToAdd dto) {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();
        var type = InvestmentType.builder()
                .name(dto.name())
                .category(dto.category())
                .iconName(dto.iconName() != null ? dto.iconName() : "QuestionOutlined")
                .iconColor(dto.iconColor() != null ? dto.iconColor() : "#d9d9d9")
                .workspaceId(workspaceId)
                .build();
        return investmentTypeMapper.toRecord(investmentTypeRepository.save(type));
    }

    @Transactional
    public InvestmentTypeRecord update(Long id, InvestmentTypeToUpdate dto) {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();
        var type = investmentTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tipo de inversión no encontrado"));

        this.checkWorkspaceOwnership(type, workspaceId);

        if (dto.name() != null) {
            type.setName(dto.name());
        }
        if (dto.iconName() != null) {
            type.setIconName(dto.iconName());
        }
        if (dto.iconColor() != null) {
            type.setIconColor(dto.iconColor());
        }

        return investmentTypeMapper.toRecord(investmentTypeRepository.save(type));
    }

    @Transactional
    public void delete(Long id) {
        var workspaceId = workspaceContextService.getActiveWorkspaceId();
        var type = investmentTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tipo de inversión no encontrado"));

        this.checkWorkspaceOwnership(type, workspaceId);
        investmentTypeRepository.delete(type);
    }

    private void checkWorkspaceOwnership(InvestmentType type, Long workspaceId) {
        if (!type.getWorkspaceId().equals(workspaceId)) {
            throw new PermissionDeniedException("No tenés permiso para modificar este tipo de inversión");
        }
    }
}
