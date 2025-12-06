package api.expenses.expenses.mappers;

import api.expenses.expenses.entities.Movement;
import api.expenses.expenses.records.LastIngresoRecord;
import api.expenses.expenses.records.movements.ExpenseToUpdate;
import api.expenses.expenses.records.movements.MovementRecord;
import api.expenses.expenses.records.movements.MovementToAdd;
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

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "userGroups", ignore = true)
    void updateMovement(ExpenseToUpdate changesToMovement, @MappingTarget Movement movement);


    MovementRecord toRecord(Movement movement);
    LastIngresoRecord toLastIngreso(Movement ingreso);
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "userGroups", ignore = true)
    @Mapping(target = "type", source = "type")
    Movement toEntity(MovementToAdd movementToAdd);

    List<MovementRecord> toRecord(List<Movement> movement);

}
