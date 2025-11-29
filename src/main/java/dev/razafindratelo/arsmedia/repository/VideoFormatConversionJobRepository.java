package dev.razafindratelo.arsmedia.repository;

import dev.razafindratelo.arsmedia.repository.model.job.VideoFormatConversionJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoFormatConversionJobRepository
    extends JpaRepository<VideoFormatConversionJob, String> {}
