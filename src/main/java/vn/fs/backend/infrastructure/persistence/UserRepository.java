package vn.fs.backend.infrastructure.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import vn.fs.backend.domain.model.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByEmail(String email);
    User findByUsername(String username);
    boolean existsByEmailIgnoreCaseAndUserIdNot(String email, Long userId);
    boolean existsByUsernameIgnoreCaseAndUserIdNot(String username, Long userId);

    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByUsernameIgnoreCase(String username);
    Optional<User> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);

    @Override
    @EntityGraph(attributePaths = "roles")
    List<User> findAll();

    @Override
    @EntityGraph(attributePaths = "roles")
    Optional<User> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.userId = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    @Query(
            value = """
                    select u.userId
                    from User u
                    where (:keyword is null
                           or lower(u.username) like lower(concat('%', :keyword, '%'))
                           or lower(u.email) like lower(concat('%', :keyword, '%'))
                           or lower(u.name) like lower(concat('%', :keyword, '%')))
                      and (:enabled is null or u.status = :enabled)
                      and (:role is null or exists (
                          select r.id
                          from u.roles r
                          where upper(r.name) = upper(:role)
                      ))
                    """,
            countQuery = """
                    select count(u)
                    from User u
                    where (:keyword is null
                           or lower(u.username) like lower(concat('%', :keyword, '%'))
                           or lower(u.email) like lower(concat('%', :keyword, '%'))
                           or lower(u.name) like lower(concat('%', :keyword, '%')))
                      and (:enabled is null or u.status = :enabled)
                      and (:role is null or exists (
                          select r.id
                          from u.roles r
                          where upper(r.name) = upper(:role)
                      ))
                    """)
    Page<Long> searchIdsForAdministration(
            @Param("keyword") String keyword,
            @Param("enabled") Boolean enabled,
            @Param("role") String role,
            Pageable pageable);

    @EntityGraph(attributePaths = "roles")
    @Query("select distinct u from User u where u.userId in :ids")
    List<User> findAllWithRolesByIdIn(@Param("ids") List<Long> ids);

    @Query("""
            select count(distinct u)
            from User u
            join u.roles r
            where u.status = true and r.name = 'ROLE_ADMIN'
            """)
    long countActiveAdministrators();


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update User u " +
            "   set u.failedAttempt = 0, " +
            "       u.lockedUntil   = null, " +
            "       u.lastLoginAt   = :now " +
            " where lower(u.username) = lower(:login) " +
            "    or lower(u.email)    = lower(:login)")
    int resetLoginState(@Param("login") String login,
                        @Param("now")   LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update User u " +
            "   set u.failedAttempt = u.failedAttempt + 1 " +
            " where (lower(u.username) = lower(:login) " +
            "    or lower(u.email)    = lower(:login)) " +
            "   and (u.lockedUntil is null or u.lockedUntil < :now)")
    int bumpFailureCounter(@Param("login") String login,
                           @Param("now")   LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update User u " +
            "   set u.lockedUntil = :lockUntil " +
            " where (lower(u.username) = lower(:login) " +
            "    or lower(u.email)    = lower(:login)) " +
            "   and (u.lockedUntil is null or u.lockedUntil < :now) " +
            "   and u.failedAttempt >= :maxFail")
    int applyLockIfExceeded(@Param("login")     String login,
                            @Param("maxFail")   int maxFail,
                            @Param("lockUntil") LocalDateTime lockUntil,
                            @Param("now")       LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update User u set u.password = :hashed, u.status = true where lower(u.email) = lower(:email)")
    int updatePasswordByEmail(@Param("email") String email,
                              @Param("hashed") String hashed);

    @Query(value =
            "select date(u.register_date) d, count(*) v " +
                    "from `user` u " +
                    "where u.register_date >= :from " +
                    "group by date(u.register_date) " +
                    "order by d", nativeQuery = true)
    List<Object[]> newUsersByDateFrom(@Param("from") LocalDate from);

    @Query(value =
            "select count(*) " +
                    "from `user` u " +
                    "where u.register_date >= :from", nativeQuery = true)
    long countNewUsersFrom(@Param("from") LocalDate from);
}
