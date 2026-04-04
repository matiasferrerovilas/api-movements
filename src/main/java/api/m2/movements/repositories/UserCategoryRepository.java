package api.m2.movements.repositories;

import api.m2.movements.entities.UserCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCategoryRepository extends JpaRepository<UserCategory, Long> {

    @Query("SELECT uc FROM UserCategory uc JOIN FETCH uc.category WHERE uc.user.id = :userId AND uc.isActive = true")
    List<UserCategory> findByUserIdAndIsActiveTrue(@Param("userId") Long userId);

    Optional<UserCategory> findByUserIdAndCategoryId(Long userId, Long categoryId);
}
