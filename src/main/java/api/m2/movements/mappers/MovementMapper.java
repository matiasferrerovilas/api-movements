package api.m2.movements.mappers;

import api.m2.movements.entities.Movement;
import api.m2.movements.records.LastIngresoRecord;
import api.m2.movements.records.movements.ExpenseToUpdate;
import api.m2.movements.records.movements.MovementRecord;
import api.m2.movements.records.movements.MovementToAdd;
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
    @Mapping(target = "account", ignore = true)
    void updateMovement(ExpenseToUpdate changesToMovement, @MappingTarget Movement movement);


    MovementRecord toRecord(Movement movement);
    LastIngresoRecord toLastIngreso(Movement ingreso);
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "type", source = "type")
    Movement toEntity(MovementToAdd movementToAdd);

    List<MovementRecord> toRecord(List<Movement> movement);

}
