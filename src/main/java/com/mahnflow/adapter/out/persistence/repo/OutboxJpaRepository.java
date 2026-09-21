package com.mahnflow.adapter.out.persistence.repo;

import com.mahnflow.adapter.out.persistence.entity.OutboxEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

    // jobs that are not done yet, oldest first.
    List<OutboxEventEntity> findByProcessedAtIsNullOrderByCreatedAtAsc(Pageable page);
}
