package api.m2.movements.repositories;

import api.m2.movements.entities.integrity.UserBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface UserBankRepository extends JpaRepository<UserBank, Long> {

    @Query("SELECT ub FROM UserBank ub JOIN FETCH ub.bank WHERE ub.userId = :userId")
    List<UserBank> findByUserId(@Param("userId") Long userId);

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO user_banks (user_id, bank_id)
            SELECT :userId, b.id FROM banks b WHERE b.id IN (:bankIds)
            """, nativeQuery = true)
    void linkBanksToUser(@Param("userId") Long userId, @Param("bankIds") Collection<Long> bankIds);

    boolean existsByUserIdAndBankId(Long userId, Long bankId);

    void deleteByUserIdAndBankId(Long userId, Long bankId);
}
