package com.banking.ledger.domain.repository;

import com.banking.ledger.domain.model.Movement;

import java.util.Optional;

public interface MovementRepository {
    Movement save(Movement movement);
    Optional<Movement> findById(Long id);

}
