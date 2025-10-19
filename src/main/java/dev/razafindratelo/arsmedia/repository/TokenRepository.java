package dev.razafindratelo.arsmedia.repository;

import dev.razafindratelo.arsmedia.model.token.TokenType;
import dev.razafindratelo.arsmedia.repository.model.token.JToken;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenRepository extends JpaRepository<JToken, String> {

  Optional<JToken> findByValue(String value);

  Optional<JToken> findByValueAndIsValid(String value, boolean isValid);

  List<JToken> findByUserEmailAndIsValid(String userEmail, boolean isValid);

  List<JToken> findByUserEmailAndTypeAndIsValid(String userEmail, TokenType type, boolean isValid);

  List<JToken> findByIsValidAndExpirationBefore(boolean isValid, LocalDateTime expiration);

  long countByUserEmailAndIsValid(String userEmail, boolean isValid);

  boolean existsByValueAndIsValid(String value, boolean isValid);

  Optional<JToken> findByValueAndUserEmailAndIsValid(
      String value, String userEmail, boolean isValid);

  @Query(
      "SELECT t FROM JToken t WHERE t.user.email = :userEmail AND t.isValid = :isValid ORDER BY"
          + " t.creation ASC")
  List<JToken> findByUserEmailAndIsValidOrderByCreationAsc(
      @Param("userEmail") String userEmail, @Param("isValid") boolean isValid);
}
