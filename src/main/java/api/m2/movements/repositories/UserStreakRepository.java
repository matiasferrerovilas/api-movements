package api.m2.movements.repositories;

import api.m2.movements.entities.UserStreak;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserStreakRepository extends JpaRepository<UserStreak, Long> {
    Optional<UserStreak> findByUserIdAndWorkspaceId(Long userId, Long workspaceId);
}
