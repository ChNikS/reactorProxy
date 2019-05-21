package ru.chns.proxy.service;

import reactor.core.publisher.Mono;

public interface CacheProxyService {
    /**
     * Main cache service function - getting file
     * @param filename
     * @return
     */
    Mono<byte[]> getFile(String filename);
}
