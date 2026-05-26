package org.example.backend.user.repository;

import org.example.backend.user.model.User;
import org.example.backend.user.model.UserAccountStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findUserById(String id);
    boolean existsUserById(String id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findAllByEmailIn(List<String> emails);
    Optional<User> findByName(String name);
    Optional<User> findByIdx(Long idx);

    List<User> findAllByOrganizationIdx(Long organizationIdx);

    List<User> findAllByOrganization_IdxAndAccountStatus(Long organizationIdx, org.example.backend.user.model.UserAccountStatus accountStatus);

    List<User> findAllByCompanyName(String companyName);

    List<User> findAllByCompanyNameAndAccountStatus(String companyName, org.example.backend.user.model.UserAccountStatus accountStatus);

    List<User> findAllByRole(String role);

    @Query("SELECT u.idx FROM User u " +
           "WHERE u.organization.idx IN :organizationIdxs " +
           "AND u.role IN :roles " +
           "AND u.accountStatus = :accountStatus")
    Set<Long> findUserIdxByOrganizationIdxInAndRoleInAndAccountStatus(
            @Param("organizationIdxs") Collection<Long> organizationIdxs,
            @Param("roles") Collection<String> roles,
            @Param("accountStatus") UserAccountStatus accountStatus);

    @EntityGraph(attributePaths = {"organization"})
    Optional<User> findWithOrganizationByIdx(Long idx);
    // ↑ @EntityGraph 가 "organization 필드를 같이 fetch해" 라고 JPA에 지시
}
