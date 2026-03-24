package api.m2.movements.repositories;

import api.m2.movements.entities.Bank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BankRepository extends JpaRepository<Bank, Long> {
    Optional<Bank> findByDescription(String description);
}

