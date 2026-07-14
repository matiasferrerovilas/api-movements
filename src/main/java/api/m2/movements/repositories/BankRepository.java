package api.m2.movements.repositories;

import api.m2.movements.entities.commons.Bank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankRepository extends JpaRepository<Bank, Long> {
    Optional<Bank> findByDescription(String description);

    List<Bank> findByDescriptionIn(Collection<String> descriptions);
}

