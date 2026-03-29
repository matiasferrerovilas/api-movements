package api.m2.movements.records.settings;

import api.m2.movements.enums.UserSettingKey;

public record UserSettingResponse(UserSettingKey key, Long value) {
}
