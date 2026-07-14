package api.m2.movements.repositories;

import api.m2.movements.entities.integrity.UserSetting;
import api.m2.movements.enums.UserSettingKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {

    Optional<UserSetting> findByUserIdAndSettingKey(Long userId, UserSettingKey settingKey);

    List<UserSetting> findAllByUserId(Long userId);

    void deleteByUserIdAndSettingKey(Long userId, UserSettingKey settingKey);

    @Modifying
    @Query(value = """
            INSERT INTO user_settings (user_id, setting_key, setting_value, created_at, updated_at)
            VALUES (:userId, :settingKey, :settingValue, NOW(), NOW())
            ON DUPLICATE KEY UPDATE setting_value = :settingValue, updated_at = NOW()
            """, nativeQuery = true)
    void upsertSetting(@Param("userId") Long userId,
                       @Param("settingKey") String settingKey,
                       @Param("settingValue") Long settingValue);

    @Query("""
            SELECT s.userId FROM UserSetting s
            WHERE s.settingKey = :key
            AND s.settingValue = 1
            """)
    List<Long> findUserIdsWithSettingEnabled(@Param("key") UserSettingKey key);
}
