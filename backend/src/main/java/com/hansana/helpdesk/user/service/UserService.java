package com.hansana.helpdesk.user.service;

import com.hansana.helpdesk.auth.dto.UserResponse;
import com.hansana.helpdesk.user.entity.User;
import com.hansana.helpdesk.user.entity.UserRole;
import com.hansana.helpdesk.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponse> findUsers(UserRole role, Boolean active) {
        List<User> users;
        if (role != null && active != null) {
            users = userRepository.findByRoleAndActiveOrderByFirstNameAscLastNameAsc(role, active);
        } else if (role != null) {
            users = userRepository.findByRoleOrderByFirstNameAscLastNameAsc(role);
        } else if (active != null) {
            users = userRepository.findByActiveOrderByFirstNameAscLastNameAsc(active);
        } else {
            users = userRepository.findAllByOrderByFirstNameAscLastNameAsc();
        }

        return users.stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());
    }
}
