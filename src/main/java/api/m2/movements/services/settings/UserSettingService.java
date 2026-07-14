package api.m2.movements.services.settings;

import api.m2.movements.entities.commons.Bank;
import api.m2.movements.entities.integrity.UserSetting;
import api.m2.movements.enums.UserSettingKey;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.records.settings.UserSettingResponse;
import api.m2.movements.repositories.BankRepository;
import api.m2.movements.repositories.UserSettingRepository;
import api.m2.movements.services.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserSettingService {

    private final UserSettingRepository userSettingRepository;
    private final UserService userService;
    private final BankRepository bankRepository;

    public List<UserSettingResponse> getAll() {
        Long userId = userService.getAuthenticatedUser().id();
        return userSettingRepository.findAllByUserId(userId)
                .stream()
                .map(s -> new UserSettingResponse(s.getSettingKey(), s.getSettingValue()))
                .toList();
    }

    public UserSettingResponse getByKey(UserSettingKey key) {
        Long userId = userService.getAuthenticatedUser().id();
        return userSettingRepository.findByUserIdAndSettingKey(userId, key)
                .map(s -> new UserSettingResponse(s.getSettingKey(), s.getSettingValue()))
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró un default configurado para: " + key.name()));
    }

    @Transactional
    public UserSettingResponse upsert(UserSettingKey key, Long value) {
        Long userId = userService.getAuthenticatedUser().id();
        UserSetting setting = userSettingRepository.findByUserIdAndSettingKey(userId, key)
                .orElseGet(() -> UserSetting.builder()
                        .userId(userId)
                        .settingKey(key)
                        .build());
        setting.setSettingValue(value);
        UserSetting saved = userSettingRepository.saveAndFlush(setting);
        return new UserSettingResponse(saved.getSettingKey(), saved.getSettingValue());
    }

    @Transactional
    public void upsertForUser(Long userId, UserSettingKey key, Long value) {
        userSettingRepository.upsertSetting(userId, key.name(), value);
    }

    @Transactional
    public void deleteByKey(UserSettingKey key) {
        Long userId = userService.getAuthenticatedUser().id();
        userSettingRepository.deleteByUserIdAndSettingKey(userId, key);
    }

    public Optional<Bank> getDefaultBank(Long userId) {
        return userSettingRepository.findByUserIdAndSettingKey(userId, UserSettingKey.DEFAULT_BANK)
                .flatMap(s -> bankRepository.findById(s.getSettingValue()));
    }

    public Optional<Long> getDefaultWorkspaceId(Long userId) {
        return userSettingRepository.findByUserIdAndSettingKey(userId, UserSettingKey.DEFAULT_WORKSPACE)
                .map(UserSetting::getSettingValue);
    }
}
