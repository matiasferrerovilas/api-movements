package api.m2.movements.mappers;

import api.m2.movements.records.users.UserBaseRecord;
import api.m2.movements.entities.movements.Movement;
import api.m2.movements.records.accounts.AccountBaseRecord;
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
        CurrencyMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MovementMapper {

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "workspaceId", ignore = true)
    @Mapping(target = "bank", ignore = true)
    void updateMovement(ExpenseToUpdate changesToMovement, @MappingTarget Movement movement);

    @Mapping(target = "bank", source = "movement.bank.description")
    @Mapping(target = "account", expression = "java(new AccountBaseRecord(movement.getWorkspaceId(), null))")
    @Mapping(target = "owner", expression = "java(new UserBaseRecord(null, movement.getOwnerId()))")
    MovementRecord toRecord(Movement movement);

    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "workspaceId", ignore = true)
    @Mapping(target = "bank", ignore = true)
    @Mapping(target = "type", source = "type")
    Movement toEntity(MovementToAdd movementToAdd);

    List<MovementRecord> toRecord(List<Movement> movement);

}

