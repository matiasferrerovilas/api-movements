package api.m2.movements.repositories;

import api.m2.movements.entities.User;
import api.m2.movements.entities.UserSetting;
import api.m2.movements.enums.UserSettingKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {

    Optional<UserSetting> findByUserAndSettingKey(User user, UserSettingKey settingKey);

    List<UserSetting> findAllByUser(User user);
}
