package dev.razafindratelo.arsmedia.repository;

import dev.razafindratelo.arsmedia.repository.model.JApiKey;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiKeyRepository extends JpaRepository<JApiKey, String> {
  Optional<JApiKey> findByApiKey(String apiKey);

  @Query("SELECT ak FROM JApiKey ak WHERE ak.apiKey = :apiKey AND ak.expiration > :now")
  Optional<JApiKey> findByApiKeyAndNotExpired(
      @Param("apiKey") String apiKey, @Param("now") LocalDateTime now);
}
