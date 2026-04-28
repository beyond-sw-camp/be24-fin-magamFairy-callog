package org.example.backend.user.repository;


import org.example.backend.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findUserById(String id);
    boolean existsUserById(String id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findAllByEmailIn(List<String> emails);
    Optional<User> findByName(String name);
    Optional<User> findByIdx(Long idx);
}
