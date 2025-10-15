package dev.razafindratelo.arsmedia.repository;

import dev.razafindratelo.arsmedia.repository.model.JAuthCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthCodeRepository extends JpaRepository<JAuthCode, String> {

  Optional<JAuthCode> findByUserIdAndCode(String userId, String code);
}
