package api.m2.movements.repositories;

import api.m2.movements.entities.User;
import api.m2.movements.entities.UserSetting;
import api.m2.movements.enums.UserSettingKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {

    Optional<UserSetting> findByUserAndSettingKey(User user, UserSettingKey settingKey);

    List<UserSetting> findAllByUser(User user);

    void deleteByUserAndSettingKey(User user, UserSettingKey settingKey);

    @Query("""
            SELECT u FROM User u
            JOIN UserSetting s ON s.user = u
            WHERE s.settingKey = :key
            AND s.settingValue = 1
            AND u.isFirstLogin = false
            """)
    List<User> findUsersWithSettingEnabled(@Param("key") UserSettingKey key);
}
