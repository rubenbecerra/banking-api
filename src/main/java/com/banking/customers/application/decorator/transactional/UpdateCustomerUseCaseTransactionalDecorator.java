package com.banking.customers.application.decorator.transactional;

import com.banking.customers.application.usecase.UpdateCustomerPort;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Keeps transaction management separate from caching so both advisors can be applied
 * reliably to their own bean instead of competing on the same method.
 */
public class UpdateCustomerUseCaseTransactionalDecorator implements UpdateCustomerPort {

    private final UpdateCustomerPort target;
    private final TransactionTemplate transactionTemplate;

    public UpdateCustomerUseCaseTransactionalDecorator(UpdateCustomerPort target,
                                                       TransactionTemplate transactionTemplate) {
        this.target = target;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void update(Integer id, String name, Integer age, String gender) {
        transactionTemplate.executeWithoutResult(status ->
                target.update(id, name, age, gender));
    }

    @Override
    public void updateByEmail(String email, String name, Integer age, String gender) {
        transactionTemplate.executeWithoutResult(status ->
                target.updateByEmail(email, name, age, gender));
    }
}
