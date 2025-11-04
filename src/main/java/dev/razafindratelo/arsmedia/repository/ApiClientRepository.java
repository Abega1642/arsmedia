package dev.razafindratelo.arsmedia.repository;

import dev.razafindratelo.arsmedia.repository.model.JApiClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiClientRepository extends JpaRepository<JApiClient, String> {}
