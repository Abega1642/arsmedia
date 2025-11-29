package dev.razafindratelo.arsmedia.repository;

import static java.time.LocalDateTime.now;

import dev.razafindratelo.arsmedia.repository.model.JAuthCode;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthCodeRepository extends JpaRepository<JAuthCode, String> {

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Transactional
  @Query("UPDATE JAuthCode jAc SET jAc.deadline = :endDate WHERE jAc.user.id = :userId")
  int disableAuthCodesAtADate(
      @Param("userId") String userId, @Param("endDate") LocalDateTime endDate);

  @Transactional
  default int disableAuthCodes(String userId) {
    return disableAuthCodesAtADate(userId, now());
  }

  Optional<JAuthCode> findByUserIdAndCode(String userId, String code);
}
