package com.banking.accounts.application.usecase;

public interface DeleteAccountPort {
    void execute(String owner, String iban);
    void executeByAdmin(String iban);
}
