package dev.razafindratelo.arsmedia.repository;

import dev.razafindratelo.arsmedia.repository.model.job.AudioExtractionJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AudioExtractionJobRepository extends JpaRepository<AudioExtractionJob, String> {}
