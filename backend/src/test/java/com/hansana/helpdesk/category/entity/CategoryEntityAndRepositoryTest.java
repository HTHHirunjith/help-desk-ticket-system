package com.hansana.helpdesk.category.entity;

import com.hansana.helpdesk.category.repository.CategoryRepository;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryEntityAndRepositoryTest {

    @Test
    void categoryEntityMapsToCategoriesTable() throws NoSuchFieldException {
        Table table = Category.class.getAnnotation(Table.class);
        assertNotNull(table);
        assertEquals("categories", table.name());

        assertEquals("name", columnName(Category.class.getDeclaredField("name")));
        assertEquals("description", columnName(Category.class.getDeclaredField("description")));
        assertEquals("active", columnName(Category.class.getDeclaredField("active")));
        assertEquals("created_at", columnName(Category.class.getDeclaredField("createdAt")));
        assertEquals("updated_at", columnName(Category.class.getDeclaredField("updatedAt")));
    }

    @Test
    void categoryDefaultActiveIsTrue() {
        Category category = new Category();
        assertTrue(category.isActive());

        Category paramCategory = new Category("BILLING", "Billing issues");
        assertTrue(paramCategory.isActive());
        assertEquals("BILLING", paramCategory.getName());
        assertEquals("Billing issues", paramCategory.getDescription());
    }

    @Test
    void categoryPrePersistSetsTimestamps() {
        Category category = new Category("TECHNICAL", "Tech support");
        assertNullOrEmpty(category.getCreatedAt());
        assertNullOrEmpty(category.getUpdatedAt());

        category.onCreate();

        assertNotNull(category.getCreatedAt());
        assertNotNull(category.getUpdatedAt());
    }

    @Test
    void categoryRepositoryDeclaresCaseInsensitiveQueries() throws NoSuchMethodException {
        Method findByName = CategoryRepository.class.getMethod("findByNameIgnoreCase", String.class);
        assertEquals(Optional.class, findByName.getReturnType());
        Query query = findByName.getAnnotation(Query.class);
        assertNotNull(query);
        assertTrue(query.value().toUpperCase(Locale.ROOT).contains("LOWER"));

        Method existsByName = CategoryRepository.class.getMethod("existsByNameIgnoreCase", String.class);
        assertEquals(boolean.class, existsByName.getReturnType());
        Query existsQuery = existsByName.getAnnotation(Query.class);
        assertNotNull(existsQuery);
        assertTrue(existsQuery.value().toUpperCase(Locale.ROOT).contains("LOWER"));
    }

    @Test
    void categoryNameCaseInsensitiveLookupInStore() {
        Map<String, Category> store = new HashMap<>();
        Category cat = new Category("BUG_REPORT", "Bug reports");
        cat.setId(UUID.randomUUID());
        store.put(cat.getName().toLowerCase(Locale.ROOT), cat);

        Category found = store.get("bug_report".toLowerCase(Locale.ROOT));
        assertNotNull(found);
        assertEquals("BUG_REPORT", found.getName());
        assertEquals(cat.getId(), found.getId());
    }

    private static String columnName(Field field) {
        Column column = field.getAnnotation(Column.class);
        assertNotNull(column);
        return column.name().isEmpty() ? field.getName() : column.name();
    }

    private static void assertNullOrEmpty(Object obj) {
        assertTrue(obj == null);
    }
}
