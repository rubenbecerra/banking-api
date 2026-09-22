package com.banking.transactions.application.usecase;

import com.banking.transactions.domain.model.PageResult;
import com.banking.transactions.domain.model.Transaction;

public interface GetTransactionsPort {
    PageResult<Transaction> getTransactionsForAccount(String iban,
                                                      String authenticatedEmail,
                                                      int page, int size, String sortBy);
    PageResult<Transaction> adminGetTransactionsByIban(String iban,
                                                       int page, int size,String sortBy);
}