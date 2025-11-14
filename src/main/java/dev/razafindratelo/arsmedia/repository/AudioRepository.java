package dev.razafindratelo.arsmedia.repository;

import dev.razafindratelo.arsmedia.repository.model.JAudio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AudioRepository extends JpaRepository<JAudio, String> {}
