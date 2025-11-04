package dev.razafindratelo.arsmedia.repository;

import dev.razafindratelo.arsmedia.repository.model.JApiClientSecret;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiClientSecretRepository extends JpaRepository<JApiClientSecret, String> {
  @Query(
      "SELECT acS FROM JApiClientSecret acS WHERE acS.apiClient.id = :clientId AND acS.secret ="
          + " :secret")
  Optional<JApiClientSecret> findByApiClientIdAndSecret(
      @Param("clientId") String clientId, @Param("secret") String secret);
}
