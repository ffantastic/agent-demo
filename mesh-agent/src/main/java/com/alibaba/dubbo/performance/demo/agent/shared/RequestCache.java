package com.alibaba.dubbo.performance.demo.agent.shared;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RequestCache<CachedType> {
    private ThreadLocal<Map<Long, CachedType>> localCache = new ThreadLocal<Map<Long, CachedType>>() {
        @Override
        protected Map<Long, CachedType> initialValue() {
            return new HashMap<>();
        }
    };

    private ConcurrentHashMap<Long, CachedType> globalCache = new ConcurrentHashMap<>();

    public void Cache(Long reqId, CachedType value, boolean useLocalCache) {
        if (useLocalCache) {
            localCache.get().put(reqId, value);
        } else {
            globalCache.put(reqId, value);
        }
    }

    public CachedType Remove(Long reqId) throws Exception {
        boolean existLocalCache = true;
        Map<Long, CachedType> _localCache = localCache.get();
        CachedType data = _localCache.get(reqId);
        if (data == null) {
            data = globalCache.get(reqId);
            if (data == null) {
                throw new Exception("cache is not exist in neither local nor global store : key " + reqId);
            }

            existLocalCache = false;
        }

        if (existLocalCache) {
            _localCache.remove(reqId);
        } else {
            globalCache.remove(reqId);
        }

        return data;
    }
}
