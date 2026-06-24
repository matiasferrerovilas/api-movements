package api.m2.movements.movements.records.settings;

import api.m2.movements.movements.enums.UserSettingKey;

public record UserSettingResponse(UserSettingKey key, Long value) {
}
