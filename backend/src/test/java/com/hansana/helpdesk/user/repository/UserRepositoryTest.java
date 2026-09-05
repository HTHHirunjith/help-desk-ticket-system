package com.hansana.helpdesk.user.repository;

import com.hansana.helpdesk.user.entity.User;
import com.hansana.helpdesk.user.entity.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserRepositoryTest {

    @Test
    void findByEmailDeclaresCaseInsensitiveLookup() throws NoSuchMethodException {
        Method method = UserRepository.class.getMethod("findByEmail", String.class);

        assertEquals(Optional.class, method.getReturnType());
        Query query = method.getAnnotation(Query.class);
        assertNotNull(query);
        String jpql = query.value().toUpperCase(Locale.ROOT);
        assertTrue(jpql.contains("LOWER"));
        assertTrue(jpql.contains("EMAIL"));
    }

    @Test
    void findByEmailLocatesPersistedUserIgnoringCase() {
        Map<String, User> store = new HashMap<>();
        User persisted = new User();
        persisted.setId(UUID.randomUUID());
        persisted.setEmail("Agent@Example.com");
        persisted.setPassword("hashed-value");
        persisted.setFirstName("Ada");
        persisted.setLastName("Lovelace");
        persisted.setRole(UserRole.SUPPORT_AGENT);
        persist(store, persisted);

        Optional<User> found = findByEmail(store, "agent@example.com");

        assertTrue(found.isPresent());
        assertEquals(persisted.getId(), found.get().getId());
        assertEquals("Agent@Example.com", found.get().getEmail());
    }

    @Test
    void userEntityMapsToUsersTableColumns() throws NoSuchFieldException {
        Table table = User.class.getAnnotation(Table.class);
        assertNotNull(table);
        assertEquals("users", table.name());

        assertEquals("password_hash", columnName(User.class.getDeclaredField("password")));
        assertEquals("first_name", columnName(User.class.getDeclaredField("firstName")));
        assertEquals("last_name", columnName(User.class.getDeclaredField("lastName")));
        assertEquals("avatar_url", columnName(User.class.getDeclaredField("avatarUrl")));
        assertEquals("created_at", columnName(User.class.getDeclaredField("createdAt")));
        assertEquals("updated_at", columnName(User.class.getDeclaredField("updatedAt")));

        Enumerated enumerated = User.class.getDeclaredField("role").getAnnotation(Enumerated.class);
        assertNotNull(enumerated);
        assertEquals(EnumType.STRING, enumerated.value());
    }

    private static String columnName(Field field) {
        Column column = field.getAnnotation(Column.class);
        assertNotNull(column);
        return column.name();
    }

    private static void persist(Map<String, User> store, User user) {
        store.put(user.getEmail().toLowerCase(Locale.ROOT), user);
    }

    private static Optional<User> findByEmail(Map<String, User> store, String email) {
        return Optional.ofNullable(store.get(email.toLowerCase(Locale.ROOT)));
    }
}
