package api.m2.movements.services.investments;

import api.m2.movements.entities.investments.InvestmentType;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.exceptions.PermissionDeniedException;
import api.m2.movements.mappers.InvestmentTypeMapper;
import api.m2.movements.records.investments.InvestmentTypeRecord;
import api.m2.movements.records.investments.InvestmentTypeToAdd;
import api.m2.movements.records.investments.InvestmentTypeToUpdate;
import api.m2.movements.repositories.InvestmentTypeRepository;
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
        var workspace = workspaceContextService.getActiveWorkspace();
        var type = InvestmentType.builder()
                .name(dto.name())
                .iconName(dto.iconName() != null ? dto.iconName() : "QuestionOutlined")
                .iconColor(dto.iconColor() != null ? dto.iconColor() : "#d9d9d9")
                .workspace(workspace)
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
        if (!type.getWorkspace().getId().equals(workspaceId)) {
            throw new PermissionDeniedException("No tenés permiso para modificar este tipo de inversión");
        }
    }
}
