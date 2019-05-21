package ru.chns.proxy.cache;

/**
 * Sates of cache file
 * DOWNLOADING - download was requested (no need to call download function)
 * SAVED - have saved file
 * DELETED - deleting file process was started, should call for download function
 */
public enum CacheStatus {
    DOWNLOADING, SAVED, DELETING
}
