package api.m2.movements.services.settings;

import api.m2.movements.entities.User;
import api.m2.movements.entities.UserSetting;
import api.m2.movements.enums.UserSettingKey;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.records.settings.UserSettingResponse;
import api.m2.movements.repositories.UserSettingRepository;
import api.m2.movements.services.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSettingService {

    private final UserSettingRepository userSettingRepository;
    private final UserService userService;

    public List<UserSettingResponse> getAll() {
        User user = userService.getAuthenticatedUser();
        return userSettingRepository.findAllByUser(user)
                .stream()
                .map(s -> new UserSettingResponse(s.getSettingKey(), s.getSettingValue()))
                .toList();
    }

    public UserSettingResponse getByKey(UserSettingKey key) {
        User user = userService.getAuthenticatedUser();
        return userSettingRepository.findByUserAndSettingKey(user, key)
                .map(s -> new UserSettingResponse(s.getSettingKey(), s.getSettingValue()))
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró un default configurado para: " + key.name()));
    }

    public UserSettingResponse upsert(UserSettingKey key, Long value) {
        User user = userService.getAuthenticatedUser();
        UserSetting setting = userSettingRepository.findByUserAndSettingKey(user, key)
                .orElseGet(() -> UserSetting.builder()
                        .user(user)
                        .settingKey(key)
                        .build());
        setting.setSettingValue(value);
        UserSetting saved = userSettingRepository.saveAndFlush(setting);
        return new UserSettingResponse(saved.getSettingKey(), saved.getSettingValue());
    }

    public void upsertForUser(User user, UserSettingKey key, Long value) {
        UserSetting setting = userSettingRepository.findByUserAndSettingKey(user, key)
                .orElseGet(() -> UserSetting.builder()
                        .user(user)
                        .settingKey(key)
                        .build());
        setting.setSettingValue(value);
        userSettingRepository.saveAndFlush(setting);
    }
}
