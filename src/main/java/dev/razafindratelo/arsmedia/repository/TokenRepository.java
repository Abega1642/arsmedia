package dev.razafindratelo.arsmedia.repository;

import dev.razafindratelo.arsmedia.repository.model.token.JToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenRepository extends JpaRepository<JToken, String> {
  Optional<JToken> findByValue(String apiKey);
}
