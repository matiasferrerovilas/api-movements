package api.m2.movements.events;

import api.m2.movements.records.categories.CategoryRecord;

public record CategoryUpdatedEvent(CategoryRecord category, Long workspaceId) {
}
