package ru.chns.proxy.cache;
import reactor.core.publisher.Mono;

/**
 * Cache file class
 * path  - absolute path to cached file
 * size - size in bytes
 * status
 * dataLoad - shared Mono to avoid copy of request
 */
public class CacheFile {
    private String path;
    private int size;
    private CacheStatus status;
    private Mono<byte[]> dataLoad;

    public CacheFile(Mono<byte[]> dataLoad) {
        setOnDataLoad(dataLoad);
        setStatus(CacheStatus.DOWNLOADING);
    }
    /**
     * Checks if file is downloading
     * @return
     */
    public boolean isDownloading() {
        return getStatus() == CacheStatus.DOWNLOADING;
    }

    /**
     * Saving function for cache file
     * Sets proper state and updates data
     * @param path
     * @param size
     * @return
     */
    public CacheFile saved(String path, int size) {
        setStatus(CacheStatus.SAVED);
        setPath(path);
        setSize(size);
        return this;
    }


    public String getPath() {
        return path;
    }

    public int getSize() {
        return size;
    }

    public Mono<byte[]> onDataLoad() {
        return status==CacheStatus.DOWNLOADING ? dataLoad : null;
    }

    public CacheStatus getStatus() {
        return status;
    }
    // once we have path cache file is saved
    public void setPath(String path) {
        this.path = path;
    }

    public void setOnDataLoad(Mono<byte[]> dataLoad) {
        this.dataLoad = dataLoad;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setStatus(CacheStatus status) {
        this.status = status;
    }
}
