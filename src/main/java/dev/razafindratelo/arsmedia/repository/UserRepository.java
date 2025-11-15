package dev.razafindratelo.arsmedia.repository;

import dev.razafindratelo.arsmedia.repository.model.JUser;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<JUser, String> {
  Optional<JUser> findByEmail(String email);

  @Modifying
  @Transactional
  void deleteByEmail(String email);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Transactional
  @Query(
      "UPDATE JUser u SET u.isActivated = :isActivated, u.updatedAt = :updatedAt WHERE u.email ="
          + " :email")
  int updateActivationByEmail(
      @Param("email") String email,
      @Param("isActivated") boolean isActivated,
      @Param("updatedAt") LocalDateTime updatedAt);
}
