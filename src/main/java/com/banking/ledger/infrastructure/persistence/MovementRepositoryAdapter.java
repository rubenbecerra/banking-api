package com.banking.ledger.infrastructure.persistence;

import com.banking.ledger.domain.model.Movement;
import com.banking.ledger.domain.repository.MovementRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MovementRepositoryAdapter implements MovementRepository {

    private final SpringDataMovementRepository repo;
    private final MovementMapper mapper;

    public MovementRepositoryAdapter(SpringDataMovementRepository springData, MovementMapper mapper) {
        this.repo = springData;
        this.mapper = mapper;
    }

    @Override
    public Movement save(Movement movement) {
        MovementEntity entity = mapper.toEntity(movement);
        return mapper.toDomain(repo.save(entity));
    }

    @Override
    public Optional<Movement> findById(Long id) {
        return repo.findById(id).map(mapper::toDomain);
    }
}
