package com.mahnflow.adapter.out.persistence.repo;

import com.mahnflow.adapter.out.persistence.entity.OutboxEventEntity;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;
import java.util.UUID;

public interface OutboxJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

    // jobs that are not done yet, oldest first.
    List<OutboxEventEntity> findByProcessedAtIsNullOrderByCreatedAtAsc(Pageable page);

    // same as above, but the rows are locked (select ... for update skip locked).
    // a second worker skips the rows the first one holds instead of taking them too.
    // -2 is hibernate's value for SKIP_LOCKED.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select o from OutboxEventEntity o where o.processedAt is null order by o.createdAt asc")
    List<OutboxEventEntity> lockNextBatch(Pageable page);
}