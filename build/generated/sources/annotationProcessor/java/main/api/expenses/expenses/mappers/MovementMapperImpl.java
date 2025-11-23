package api.expenses.expenses.mappers;

import api.expenses.expenses.entities.Credito;
import api.expenses.expenses.entities.Currency;
import api.expenses.expenses.entities.Debito;
import api.expenses.expenses.entities.Ingreso;
import api.expenses.expenses.entities.Movement;
import api.expenses.expenses.entities.UserGroups;
import api.expenses.expenses.enums.BanksEnum;
import api.expenses.expenses.enums.MovementType;
import api.expenses.expenses.records.LastIngresoRecord;
import api.expenses.expenses.records.categories.CategoryRecord;
import api.expenses.expenses.records.currencies.CurrencyRecord;
import api.expenses.expenses.records.groups.UserGroupsRecord;
import api.expenses.expenses.records.groups.UserRecord;
import api.expenses.expenses.records.movements.ExpenseToUpdate;
import api.expenses.expenses.records.movements.MovementRecord;
import api.expenses.expenses.records.movements.MovementToAdd;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-11-23T17:56:34-0300",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-9.2.1.jar, environment: Java 25 (Amazon.com Inc.)"
)
@Component
public class MovementMapperImpl implements MovementMapper {

    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public Debito toDebito(MovementToAdd movementToAdd) {
        if ( movementToAdd == null ) {
            return null;
        }

        Debito.DebitoBuilder<?, ?> debito = Debito.builder();

        debito.amount( movementToAdd.amount() );
        debito.description( movementToAdd.description() );
        debito.date( movementToAdd.date() );
        debito.year( movementToAdd.year() );
        debito.month( movementToAdd.month() );
        debito.bank( movementToAdd.bank() );
        if ( movementToAdd.type() != null ) {
            debito.type( Enum.valueOf( MovementType.class, movementToAdd.type() ) );
        }

        return debito.build();
    }

    @Override
    public Credito toCredito(MovementToAdd movementToAdd) {
        if ( movementToAdd == null ) {
            return null;
        }

        Credito.CreditoBuilder<?, ?> credito = Credito.builder();

        credito.amount( movementToAdd.amount() );
        credito.description( movementToAdd.description() );
        credito.date( movementToAdd.date() );
        credito.year( movementToAdd.year() );
        credito.month( movementToAdd.month() );
        credito.bank( movementToAdd.bank() );
        if ( movementToAdd.type() != null ) {
            credito.type( Enum.valueOf( MovementType.class, movementToAdd.type() ) );
        }
        credito.cuotaActual( movementToAdd.cuotaActual() );
        credito.cuotasTotales( movementToAdd.cuotasTotales() );

        return credito.build();
    }

    @Override
    public Ingreso toIngreso(MovementToAdd movementToAdd) {
        if ( movementToAdd == null ) {
            return null;
        }

        Ingreso.IngresoBuilder<?, ?> ingreso = Ingreso.builder();

        ingreso.amount( movementToAdd.amount() );
        ingreso.description( movementToAdd.description() );
        ingreso.date( movementToAdd.date() );
        ingreso.year( movementToAdd.year() );
        ingreso.month( movementToAdd.month() );
        ingreso.bank( movementToAdd.bank() );
        if ( movementToAdd.type() != null ) {
            ingreso.type( Enum.valueOf( MovementType.class, movementToAdd.type() ) );
        }

        return ingreso.build();
    }

    @Override
    public void updateMovement(ExpenseToUpdate changesToMovement, Movement movement) {
        if ( changesToMovement == null ) {
            return;
        }

        if ( changesToMovement.amount() != null ) {
            movement.setAmount( changesToMovement.amount() );
        }
        if ( changesToMovement.description() != null ) {
            movement.setDescription( changesToMovement.description() );
        }
        if ( changesToMovement.date() != null ) {
            movement.setDate( changesToMovement.date() );
        }
        movement.setYear( changesToMovement.year() );
        movement.setMonth( changesToMovement.month() );
        if ( changesToMovement.bank() != null ) {
            movement.setBank( changesToMovement.bank() );
        }
    }

    @Override
    public MovementRecord toRecord(Movement movement) {
        if ( movement == null ) {
            return null;
        }

        Long id = null;
        BigDecimal amount = null;
        String description = null;
        LocalDate date = null;
        LocalDateTime createdAt = null;
        LocalDateTime updatedAt = null;
        CategoryRecord category = null;
        CurrencyRecord currency = null;
        int year = 0;
        int month = 0;
        BanksEnum bank = null;
        MovementType type = null;
        UserRecord users = null;
        UserGroupsRecord userGroups = null;

        id = movement.getId();
        amount = movement.getAmount();
        description = movement.getDescription();
        date = movement.getDate();
        createdAt = movement.getCreatedAt();
        updatedAt = movement.getUpdatedAt();
        category = categoryMapper.toRecord( movement.getCategory() );
        currency = currencyToCurrencyRecord( movement.getCurrency() );
        year = movement.getYear();
        month = movement.getMonth();
        bank = movement.getBank();
        type = movement.getType();
        users = userMapper.toRecord( movement.getUsers() );
        userGroups = userGroupsToUserGroupsRecord( movement.getUserGroups() );

        MovementRecord movementRecord = new MovementRecord( id, amount, description, date, createdAt, updatedAt, category, currency, year, month, bank, type, users, userGroups );

        return movementRecord;
    }

    @Override
    public LastIngresoRecord toLastIngreso(Ingreso ingreso) {
        if ( ingreso == null ) {
            return null;
        }

        Long id = null;
        BigDecimal amount = null;
        String description = null;
        CurrencyRecord currency = null;
        BanksEnum bank = null;

        id = ingreso.getId();
        amount = ingreso.getAmount();
        description = ingreso.getDescription();
        currency = currencyToCurrencyRecord( ingreso.getCurrency() );
        bank = ingreso.getBank();

        LastIngresoRecord lastIngresoRecord = new LastIngresoRecord( id, amount, description, currency, bank );

        return lastIngresoRecord;
    }

    @Override
    public List<MovementRecord> toRecord(List<Movement> movement) {
        if ( movement == null ) {
            return null;
        }

        List<MovementRecord> list = new ArrayList<MovementRecord>( movement.size() );
        for ( Movement movement1 : movement ) {
            list.add( toRecord( movement1 ) );
        }

        return list;
    }

    protected CurrencyRecord currencyToCurrencyRecord(Currency currency) {
        if ( currency == null ) {
            return null;
        }

        String symbol = null;

        symbol = currency.getSymbol();

        CurrencyRecord currencyRecord = new CurrencyRecord( symbol );

        return currencyRecord;
    }

    protected UserGroupsRecord userGroupsToUserGroupsRecord(UserGroups userGroups) {
        if ( userGroups == null ) {
            return null;
        }

        String description = null;
        Long id = null;

        description = userGroups.getDescription();
        id = userGroups.getId();

        UserGroupsRecord userGroupsRecord = new UserGroupsRecord( description, id );

        return userGroupsRecord;
    }
}
