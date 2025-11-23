package api.expenses.expenses.integration

import api.expenses.expenses.enums.BanksEnum
import api.expenses.expenses.enums.GroupsEnum
import api.expenses.expenses.enums.MovementType
import api.expenses.expenses.repositories.MovementRepository
import api.expenses.expenses.services.movements.MovementImportFileService
import org.springframework.beans.factory.annotation.Autowired

class MovementImportFileServiceSpec extends CommonSpec {
    @Autowired
    MovementImportFileService expenseFileService

    @Autowired
    MovementRepository expenseRepository

    def setup(){
        expenseRepository.deleteAll()
    }

    def "Debería procesar correctamente el resumen de tarjeta de crédito VISA del banco GALICIA y guardar todos los movimientos"() {
        given: "Un archivo PDF simulado con todos los consumos de la tarjeta"
        var pdfFile = MockFileFactory.resumenGaliciaMock()

        when: "Se procesa el PDF para guardar los gastos en el repositorio"
        expenseFileService.importMovementsByFile(pdfFile, BanksEnum.GALICIA.name(), GroupsEnum.DEFAULT.name())

        then: "El repositorio contiene todos los movimientos esperados"
        var list = expenseRepository.findAll()
        list.size() == 7
        and: "El primer movimiento se corresponde correctamente con el detalle del PDF"
        with(list.getFirst()){
            cuotaActual == 6
            cuotasTotales == 6
            amount == 16316.66
            description == "LAZARO CABALLITO"
            bank == BanksEnum.GALICIA
            type == MovementType.CREDITO
            year == 2025
            month == 3
        }
    }
}
