package api.m2.movements.services.category;

import api.m2.movements.entities.Category;
import api.m2.movements.enums.CategoryEnum;
import api.expenses.expenses.mappers.CategoryMapperImpl;
import api.m2.movements.records.categories.CategoryRecord;
import api.m2.movements.repositories.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class CategoryAddServiceTest {
    @Mock
    private CategoryRepository categoryRepository;
    @Spy
    private CategoryMapperImpl categoryMapper;

    @InjectMocks
    private CategoryAddService categoryAddService;

    @Test
    @DisplayName("Debe devolver una categoría existente sin crear una nueva")
    void addCategoryExisting() {
        var existing = Category.builder()
                .description("STREAMING")
                .build();
        when(categoryRepository.findByDescription("STREAMING")).thenReturn(Optional.of(existing));

        Category result = categoryAddService.addCategory("streaming");

        assertSame(existing, result);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe crear una nueva categoría si no existe")
    void addCategoryCreatesNew() {
        Category newCategory = Category.builder().description("NUEVA").build();

        when(categoryRepository.findByDescription("NUEVA")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(newCategory);

        Category result = categoryAddService.addCategory(" nueva ");

        assertEquals("NUEVA", result.getDescription());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("Debe mapear correctamente al record al buscar por descripción existente")
    void findCategoryOk() {
        Category category = Category.builder().description("SERVICIOS").build();
        CategoryRecord record = new CategoryRecord("SERVICIOS");

        when(categoryRepository.findByDescription("SERVICIOS")).thenReturn(Optional.of(category));

        CategoryRecord result = categoryAddService.findCategoryByDescription(" servicios ");

        assertEquals("SERVICIOS", result.description());
    }

    @Test
    @DisplayName("Debe lanzar excepción si la categoría no existe")
    void findCategoryNotFound() {
        when(categoryRepository.findByDescription("OTRA")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> categoryAddService.findCategoryByDescription("OTRA"));
    }

    @Test
    @DisplayName("Debe devolver categoría por lógica de Streaming")
    void getCategoryAtLoadDefaultStreaming() {
        Category category = Category.builder().description("STREAMING").build();
        CategoryRecord expected = new CategoryRecord("STREAMING");

        when(categoryRepository.findByDescription("STREAMING")).thenReturn(Optional.of(category));
        when(categoryMapper.toRecord(category)).thenReturn(expected);

        CategoryRecord result = categoryAddService.getCategoryAtLoadDefaultByStringHelper("Pago Netflix");

        assertEquals("STREAMING", result.description());
    }

    @Test
    @DisplayName("Debe devolver categoría por lógica de Servicios (Spotify)")
    void getCategoryAtLoadDefaultServicios() {
        Category category = Category.builder().description("SERVICIOS").build();
        CategoryRecord expected = new CategoryRecord("SERVICIOS");

        when(categoryRepository.findByDescription("SERVICIOS")).thenReturn(Optional.of(category));
        when(categoryMapper.toRecord(category)).thenReturn(expected);

        CategoryRecord result = categoryAddService.getCategoryAtLoadDefaultByStringHelper("Spotify Premium");

        assertEquals("SERVICIOS", result.description());
    }

    @Test
    @DisplayName("Debe devolver Sin Categoría en caso de descripción vacía o sin coincidencias")
    void getCategoryAtLoadDefaultFallback() {
        Category category = Category.builder()
                .description(CategoryEnum.SIN_CATEGORIA.getDescripcion())
                .build();
        var expected = new CategoryRecord(CategoryEnum.SIN_CATEGORIA.getDescripcion());

        when(categoryRepository.findByDescription("SIN CATEGORIA"))
                .thenReturn(Optional.of(category));

        CategoryRecord result1 = categoryAddService.getCategoryAtLoadDefaultByStringHelper("");

        assertEquals(expected.description(), result1.description());
    }
}
