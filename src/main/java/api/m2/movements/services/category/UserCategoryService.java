package api.m2.movements.services.category;

import api.m2.movements.entities.Category;
import api.m2.movements.entities.User;
import api.m2.movements.entities.UserCategory;
import api.m2.movements.enums.DefaultCategory;
import api.m2.movements.exceptions.BusinessException;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.exceptions.PermissionDeniedException;
import api.m2.movements.mappers.UserCategoryMapper;
import api.m2.movements.records.categories.CategoryRecord;
import api.m2.movements.repositories.UserCategoryRepository;
import api.m2.movements.services.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCategoryService {

    private final UserCategoryRepository userCategoryRepository;
    private final CategoryAddService categoryAddService;
    private final UserCategoryMapper userCategoryMapper;
    private final UserService userService;

    public List<CategoryRecord> getActiveCategories() {
        var user = userService.getAuthenticatedUser();
        return userCategoryMapper.toRecordList(
                userCategoryRepository.findByUserIdAndIsActiveTrue(user.getId()));
    }

    @Transactional
    public CategoryRecord addCategory(String description) {
        var user = userService.getAuthenticatedUser();
        var category = categoryAddService.addCategory(description);
        return userCategoryMapper.toRecord(this.resolveUserCategory(user, category));
    }

    @Transactional
    public void addDefaultCategories(User user) {
        var category = categoryAddService.addCategory(DefaultCategory.SERVICIOS.getDescription());
        this.resolveUserCategory(user, category);
    }

    @Transactional
    public void addCategories(User user, List<String> descriptions) {
        descriptions.forEach(description -> {
            var category = categoryAddService.addCategory(description);
            this.resolveUserCategory(user, category);
        });
    }

    @Transactional
    public void deleteCategory(Long userCategoryId) {
        var user = userService.getAuthenticatedUser();
        var userCategory = userCategoryRepository.findById(userCategoryId)
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada"));

        if (!userCategory.getUser().getId().equals(user.getId())) {
            throw new PermissionDeniedException("No tenés permiso para eliminar esta categoría");
        }
        if (!userCategory.getCategory().isDeletable()) {
            throw new BusinessException("No se puede eliminar esta categoría");
        }

        userCategoryRepository.delete(userCategory);
    }

    private UserCategory resolveUserCategory(User user, Category category) {
        return userCategoryRepository.findByUserIdAndCategoryId(user.getId(), category.getId())
                .map(existing -> {
                    if (!existing.isActive()) {
                        existing.setActive(true);
                        return userCategoryRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> userCategoryRepository.save(
                        UserCategory.builder()
                                .user(user)
                                .category(category)
                                .build()));
    }
}
