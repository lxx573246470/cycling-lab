package com.cyclinglab.platform.training;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingRecordSampleRepository extends JpaRepository<TrainingRecordSampleEntity, Long> {

    Page<TrainingRecordSampleEntity> findAllBySession_Id(UUID sessionId, Pageable pageable);

    long countBySession_Id(UUID sessionId);

    void deleteAllBySession_Id(UUID sessionId);
}
