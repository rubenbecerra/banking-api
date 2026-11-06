package com.banking.ledger.infrastructure.persistence;

import com.banking.ledger.domain.model.EntryDirection;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EntryDirectionConverter implements AttributeConverter<EntryDirection,String> {
    @Override
    public String convertToDatabaseColumn(EntryDirection direction) {
        return switch (direction) {
            case DEBIT -> "D";
            case CREDIT -> "C";
            case null -> null;
        };
    }
    @Override
    public EntryDirection convertToEntityAttribute(String str) {
        return switch (str) {
            case "D" -> EntryDirection.DEBIT;
            case "C" -> EntryDirection.CREDIT;
            case null -> null;
            default -> throw new IllegalArgumentException("Unknown direction " + str);
        };
    }
}
