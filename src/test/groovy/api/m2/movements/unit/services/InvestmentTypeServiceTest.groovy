package api.m2.movements.unit.services

import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.exceptions.PermissionDeniedException
import api.m2.movements.investment.entities.InvestmentType
import api.m2.movements.investment.enums.InvestmentCategory
import api.m2.movements.investment.mappers.InvestmentTypeMapper
import api.m2.movements.investment.repositories.InvestmentTypeRepository
import api.m2.movements.investment.services.InvestmentTypeService
import api.m2.movements.investment.records.InvestmentTypeRecord
import api.m2.movements.investment.records.InvestmentTypeToAdd
import api.m2.movements.investment.records.InvestmentTypeToUpdate
import api.m2.movements.identity.services.workspaces.WorkspaceContextService
import spock.lang.Specification

class InvestmentTypeServiceTest extends Specification {

    InvestmentTypeRepository investmentTypeRepository = Mock(InvestmentTypeRepository)
    InvestmentTypeMapper investmentTypeMapper = Mock(InvestmentTypeMapper)
    WorkspaceContextService workspaceContextService = Mock(WorkspaceContextService)

    InvestmentTypeService service

    def setup() {
        service = new InvestmentTypeService(investmentTypeRepository, investmentTypeMapper, workspaceContextService)
    }

    def buildType(Long workspaceId = 1L) {
        return new InvestmentType(id: 1L, name: "Plazo Fijo", iconName: "BankOutlined",
                iconColor: "#1890ff", category: InvestmentCategory.PLAZO_FIJO, workspaceId: workspaceId)
    }

    // --- getByWorkspace ---

    def "getByWorkspace - should return types for active workspace"() {
        given:
        def workspaceId = 1L
        def type = buildType(workspaceId)
        def record = new InvestmentTypeRecord(1L, "Plazo Fijo", "BankOutlined", "#1890ff", workspaceId, InvestmentCategory.PLAZO_FIJO)

        workspaceContextService.getActiveWorkspaceId() >> workspaceId
        investmentTypeRepository.findByWorkspaceId(workspaceId) >> [type]
        investmentTypeMapper.toRecordList([type]) >> [record]

        when:
        def result = service.getByWorkspace()

        then:
        result.size() == 1
        result[0].name() == "Plazo Fijo"
    }

    // --- add ---

    def "add - should save type and return record"() {
        given:
        def dto = new InvestmentTypeToAdd("Cripto", InvestmentCategory.STOCK_ETF, "CryptoOutlined", "#faad14")
        def type = buildType(1L)
        def record = new InvestmentTypeRecord(1L, "Cripto", "CryptoOutlined", "#faad14", 1L, InvestmentCategory.STOCK_ETF)

        workspaceContextService.getActiveWorkspaceId() >> 1L
        investmentTypeRepository.save(_ as InvestmentType) >> type
        investmentTypeMapper.toRecord(type) >> record

        when:
        def result = service.add(dto)

        then:
        1 * investmentTypeRepository.save(_ as InvestmentType) >> { List args ->
            def saved = args[0] as InvestmentType
            assert saved.name == "Cripto"
            assert saved.category == InvestmentCategory.STOCK_ETF
            assert saved.workspaceId == 1L
            type
        }
        result.name() == "Cripto"
    }

    // --- update ---

    def "update - should update fields when type belongs to active workspace"() {
        given:
        def workspaceId = 1L
        def type = buildType(workspaceId)
        def dto = new InvestmentTypeToUpdate("FCI", "FundOutlined", "#52c41a")
        def record = new InvestmentTypeRecord(1L, "FCI", "FundOutlined", "#52c41a", workspaceId, InvestmentCategory.PLAZO_FIJO)

        workspaceContextService.getActiveWorkspaceId() >> workspaceId
        investmentTypeRepository.findById(1L) >> Optional.of(type)
        investmentTypeRepository.save(type) >> type
        investmentTypeMapper.toRecord(type) >> record

        when:
        def result = service.update(1L, dto)

        then:
        1 * investmentTypeRepository.save(type) >> type
        result.name() == "FCI"
    }

    def "update - should update only non-null fields"() {
        given:
        def workspaceId = 1L
        def type = buildType(workspaceId)
        type.iconColor = "#old"
        def dto = new InvestmentTypeToUpdate(null, null, "#52c41a")

        workspaceContextService.getActiveWorkspaceId() >> workspaceId
        investmentTypeRepository.findById(1L) >> Optional.of(type)
        investmentTypeRepository.save(type) >> type
        investmentTypeMapper.toRecord(type) >> Stub(InvestmentTypeRecord)

        when:
        service.update(1L, dto)

        then:
        type.iconColor == "#52c41a"
        type.name == "Plazo Fijo"
    }

    def "update - should throw EntityNotFoundException when type not found"() {
        given:
        investmentTypeRepository.findById(999L) >> Optional.empty()

        when:
        service.update(999L, new InvestmentTypeToUpdate("X", null, null))

        then:
        thrown(EntityNotFoundException)
        0 * investmentTypeRepository.save(_ as InvestmentType)
    }

    def "update - should throw PermissionDeniedException when type belongs to different workspace"() {
        given:
        def type = buildType(99L)
        workspaceContextService.getActiveWorkspaceId() >> 1L
        investmentTypeRepository.findById(1L) >> Optional.of(type)

        when:
        service.update(1L, new InvestmentTypeToUpdate("X", null, null))

        then:
        thrown(PermissionDeniedException)
        0 * investmentTypeRepository.save(_ as InvestmentType)
    }

    // --- delete ---

    def "delete - should delete when type belongs to active workspace"() {
        given:
        def workspaceId = 1L
        def type = buildType(workspaceId)

        workspaceContextService.getActiveWorkspaceId() >> workspaceId
        investmentTypeRepository.findById(1L) >> Optional.of(type)

        when:
        service.delete(1L)

        then:
        1 * investmentTypeRepository.delete(type)
    }

    def "delete - should throw EntityNotFoundException when type not found"() {
        given:
        investmentTypeRepository.findById(999L) >> Optional.empty()

        when:
        service.delete(999L)

        then:
        thrown(EntityNotFoundException)
        0 * investmentTypeRepository.delete(_ as InvestmentType)
    }

    def "delete - should throw PermissionDeniedException when type belongs to different workspace"() {
        given:
        def type = buildType(99L)
        workspaceContextService.getActiveWorkspaceId() >> 1L
        investmentTypeRepository.findById(1L) >> Optional.of(type)

        when:
        service.delete(1L)

        then:
        thrown(PermissionDeniedException)
        0 * investmentTypeRepository.delete(_ as InvestmentType)
    }
}
