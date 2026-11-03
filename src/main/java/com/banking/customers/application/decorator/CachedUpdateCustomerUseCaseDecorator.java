package com.banking.customers.application.decorator;

import com.banking.customers.application.usecase.UpdateCustomerPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

public class CachedUpdateCustomerUseCaseDecorator implements UpdateCustomerPort {

    private static final Logger log = LoggerFactory.getLogger(CachedUpdateCustomerUseCaseDecorator.class);

    private final UpdateCustomerPort target;
    private final CacheManager cacheManager;

    public CachedUpdateCustomerUseCaseDecorator(UpdateCustomerPort target, CacheManager cacheManager) {
        this.target = target;
        this.cacheManager = cacheManager;
    }

    @Override
    public void update(Integer id, String name, Integer age, String gender) {
        target.update(id, name, age, gender);
        evict("customer_by_id", id);
        clear("customer_by_email");
        clear("customers");
    }

    @Override
    public void updateByEmail(String email, String name, Integer age, String gender) {
        target.updateByEmail(email, name, age, gender);
        evict("customer_by_email", email);
        clear("customer_by_id");
        clear("customers");
    }

    private void evict(String cacheName, Object key) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(key);
            }
        } catch (RuntimeException ex) {
            log.warn("Cache EVICT failed [cache={}, key={}]. Entry may remain stale until TTL. Cause: {}",
                    cacheName, key, ex.getMessage());
        }
    }

    private void clear(String cacheName) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        } catch (RuntimeException ex) {
            log.warn("Cache CLEAR failed [cache={}]. Entries may remain stale until TTL. Cause: {}",
                    cacheName, ex.getMessage());
        }
    }
}
