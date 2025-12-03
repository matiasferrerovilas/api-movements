package api.expenses.expenses.services.ingreso;

import api.expenses.expenses.enums.CategoryEnum;
import api.expenses.expenses.enums.MovementType;
import api.expenses.expenses.records.movements.MovementToAdd;
import api.expenses.expenses.services.category.CategoryAddService;
import api.expenses.expenses.services.groups.GroupGetService;
import api.expenses.expenses.services.movements.MovementAddService;
import api.expenses.expenses.services.movements.MovementGetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
public class CronIngreso {
    private final MovementGetService movementGetService;
    private final MovementAddService movementAddService;
    private final GroupGetService groupGetService;
    private final CategoryAddService categoryAddService;

    //@Scheduled(cron = "*/10 * * * * *")
    public void createIngresoMovement() {
        var lastIngreso = movementGetService.getLastIngreso();
        var descriptionGroup = groupGetService.getGroupByDescription(lastIngreso.group());
        var category = categoryAddService.findCategoryByDescription(CategoryEnum.HOGAR.getDescripcion());

        movementAddService.saveMovement(new MovementToAdd(lastIngreso.amount(),
                LocalDate.now(),
                lastIngreso.description(),
                category.description(),
                MovementType.INGRESO.name(),
                lastIngreso.currency().symbol(),
                0,
                0,
                lastIngreso.bank(),
                descriptionGroup.getDescription()));
    }
}
