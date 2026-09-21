package com.banking.accounts.application.usecase;

import com.banking.accounts.domain.model.Account;
import java.math.BigDecimal;

public interface CreateAccountPort {
    Account execute(String ownerEmail, BigDecimal balance);
}