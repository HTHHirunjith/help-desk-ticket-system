package com.hansana.helpdesk.user.repository;

import com.hansana.helpdesk.user.entity.User;
import com.hansana.helpdesk.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<User> findByEmail(@Param("email") String email);

    List<User> findByRoleAndActiveOrderByFirstNameAscLastNameAsc(UserRole role, boolean active);

    List<User> findByRoleOrderByFirstNameAscLastNameAsc(UserRole role);

    List<User> findByActiveOrderByFirstNameAscLastNameAsc(boolean active);

    List<User> findAllByOrderByFirstNameAscLastNameAsc();
}
