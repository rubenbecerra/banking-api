package com.banking.shared.outbox.infrastructure.persistence;

import com.banking.shared.outbox.domain.model.OutboxEvent;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventMapper {

    public OutboxEvent toDomain(OutboxEventEntity entity) {
        if (entity == null) {
            return null;
        }
        return new OutboxEvent(
                entity.getId(),
                entity.getAggregateType(),
                entity.getAggregateId(),
                entity.getEventType(),
                entity.getPayload(),
                entity.getCreatedAt(),
                entity.getClaimedAt(),
                entity.getPublishedAt()
        );
    }

    public OutboxEventEntity toEntity(OutboxEvent event) {
        if (event == null) {
            return null;
        }
        OutboxEventEntity entity = new OutboxEventEntity();
        entity.setId(event.id());
        entity.setAggregateType(event.aggregateType());
        entity.setAggregateId(event.aggregateId());
        entity.setEventType(event.eventType());
        entity.setPayload(event.payload());
        entity.setCreatedAt(event.createdAt());
        // claimedAt and publishedAt are intentionally not set: the entity has no
        // setters for them and only the conditional UPDATEs move them.
        return entity;
    }
}
