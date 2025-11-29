package api.expenses.expenses.mappers;

import api.expenses.expenses.entities.Credito;
import api.expenses.expenses.entities.Debito;
import api.expenses.expenses.entities.Ingreso;
import api.expenses.expenses.entities.Movement;
import api.expenses.expenses.records.LastIngresoRecord;
import api.expenses.expenses.records.movements.MovementToAdd;
import api.expenses.expenses.records.movements.ExpenseToUpdate;
import api.expenses.expenses.records.movements.MovementRecord;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class,
        CurrencyMapper.class, UserMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MovementMapper {
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "userGroups", ignore = true)
    Debito toDebito(MovementToAdd movementToAdd);
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "userGroups", ignore = true)
    Credito toCredito(MovementToAdd movementToAdd);

    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "userGroups", ignore = true)
    Ingreso toIngreso(MovementToAdd movementToAdd);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "userGroups", ignore = true)
    void updateMovement(ExpenseToUpdate changesToMovement, @MappingTarget Movement movement);

    @Mapping(
            target = "cuotaActual",
            expression = "java(movement instanceof api.expenses.expenses.entities.Credito c ? c.getCuotaActual() : null)"
    )
    @Mapping(
            target = "cuotasTotales",
            expression = "java(movement instanceof api.expenses.expenses.entities.Credito c ? c.getCuotasTotales() : null)"
    )
    MovementRecord toRecord(Movement movement);
    LastIngresoRecord toLastIngreso(Ingreso ingreso);


    List<MovementRecord> toRecord(List<Movement> movement);

}
