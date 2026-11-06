package com.banking.ledger.infrastructure.persistence;

import com.banking.ledger.domain.model.Movement;
import org.springframework.stereotype.Component;

@Component
public class MovementMapper {

    public Movement toDomain(MovementEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Movement(
                entity.getId(),
                entity.getType(),
                entity.getSourceIban(),
                entity.getTargetIban(),
                entity.getAmount(),
                entity.getCreatedAt()
        );
    }

    public MovementEntity toEntity(Movement movement) {
        if (movement == null) {
            return null;
        }
        MovementEntity entity = new MovementEntity();
        entity.setId(movement.getId());
        entity.setType(movement.getType());
        entity.setSourceIban(movement.getSourceIban());
        entity.setTargetIban(movement.getTargetIban());
        entity.setAmount(movement.getAmount());
        entity.setCreatedAt(movement.getCreatedAt());

        return entity;
    }
}
