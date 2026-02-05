package api.m2.movements.services.ingreso;

import api.m2.movements.enums.CategoryEnum;
import api.m2.movements.services.category.CategoryAddService;
import api.m2.movements.services.movements.MovementAddService;
import api.m2.movements.services.movements.MovementGetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CronIngreso {
    private final MovementGetService movementGetService;
    private final MovementAddService movementAddService;
    private final CategoryAddService categoryAddService;

    //@Scheduled(cron = "*/10 * * * * *")
    public void createIngresoMovement() {
        var lastIngreso = movementGetService.getLastIngreso();
        //var descriptionGroup = groupGetService.getGroupByDescription(lastIngreso.group());
        var category = categoryAddService.findCategoryByDescription(CategoryEnum.HOGAR.getDescripcion());

        /*movementAddService.saveMovement(new MovementToAdd(lastIngreso.amount(),
                LocalDate.now(),
                lastIngreso.description(),
                category.description(),
                MovementType.INGRESO.name(),
                lastIngreso.currency().symbol(),
                0,
                0,
                lastIngreso.bank(),
                descriptionGroup.getDescription()));*/
    }
}
