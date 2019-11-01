package com.karumien.cloud.ais;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CacheEvictManager {
    
    @Autowired
    private CacheManager cacheManager;
    
    /**
     * Every 3 hours.
     */
    @Scheduled(fixedRate = 3 * 60 * 60 * 1000)
    public void evictUsers() {
        cacheManager.getCache("users").clear();
    }
    
    /**
     * Every 1 minutes.
     */
    @Scheduled(fixedRate = 60 * 1000)
    public void evictOnline() {
        cacheManager.getCache("online").clear();
    }
    
    /**
     * Every 15 mins.
     */
    @Scheduled(fixedRate = 15 * 60 * 1000)
    public void evictWorks() {
        cacheManager.getCache("works").clear();
    }
}
