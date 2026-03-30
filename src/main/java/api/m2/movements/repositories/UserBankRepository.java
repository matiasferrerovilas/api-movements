package api.m2.movements.repositories;

import api.m2.movements.entities.UserBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBankRepository extends JpaRepository<UserBank, Long> {

    @Query("SELECT ub FROM UserBank ub JOIN FETCH ub.bank WHERE ub.user.id = :userId")
    List<UserBank> findByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndBankId(Long userId, Long bankId);

    void deleteByUserIdAndBankId(Long userId, Long bankId);
}
