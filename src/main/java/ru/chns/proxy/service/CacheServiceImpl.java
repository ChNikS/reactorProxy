package ru.chns.proxy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.chns.proxy.cache.CacheFile;

import java.io.File;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caching service reactor implementation
 */
@Service
public class CacheServiceImpl implements CacheProxyService, ApplicationListener<ContextClosedEvent> {
    /**
     * Map for keeping files
     */
    public ConcurrentHashMap<String, CacheFile> cacheMap  = new ConcurrentHashMap<>();

    @Value("${baseUrl}")
    private String baseUrl;
    @Value("${tempFilePrefix}")
    private String tempFilePrefix;
    @Value("${cacheFileExtension}")
    private String cacheFileExtension;

    private static final Logger logger = LoggerFactory.getLogger(CacheServiceImpl.class);



    public Mono<byte[]> getFile(String filename) {
        logger.debug("Getting file -> "+filename);
        return readFromCache(filename);
    }

    /**
     * Getting cache
     * @param filename
     * @return
     */
    private Mono<byte[]> readFromCache(String filename) {
        logger.debug("Trying to get file: "+filename);
        //check if was asked to download
        if (cacheMap.containsKey(filename)) {
            CacheFile cachedFile = cacheMap.get(filename);
            if(cachedFile.isDownloading()) {
                logger.debug("File is already downloading, waiting it to finish");
                return cachedFile.onDataLoad().cache();
            } else {
                // reading data from cache file
                logger.debug("File was cached on the disk. Reading...");
                return readFileFromDisk(cachedFile.getPath(), cachedFile.getSize());
            }
        } else {
            logger.debug("New file to cache. Downloading...");
            return downLoadAndWrite(filename);
        }
    }

    /**
     * Downloading file to keep as cache
     * @param filename
     * @return
     */
    private Mono<byte[]> downLoadAndWrite(String filename) {
        logger.debug("Downloading file from server");
        Mono<DataBuffer> data = WebClient.create().get().uri(baseUrl+filename)
            .retrieve()
            .bodyToMono(DataBuffer.class)
            .doOnSuccess(res -> asyncSaveFile(filename, res)).cache();

        //cache to prevent from repeating request to the main server
        Mono<byte[]> dataLoaded = data.map(result -> readBytesFromDataBuffer(result)).cache();
        cacheMap.put(filename, new CacheFile(dataLoaded));
        return dataLoaded;
    }

    /**
     * File saving function based on AsynchronousFileChannel
     * @param filename
     * @param dataBuffer
     */
    private void asyncSaveFile(String filename, DataBuffer dataBuffer) {
        try {
            //creating temp file
            Path filePath = Files.createTempFile(tempFilePrefix, cacheFileExtension);
            logger.debug("Temporal file for cache was created: "+filePath);

            AsynchronousFileChannel channel = AsynchronousFileChannel.open(filePath, StandardOpenOption.WRITE);
            Mono<Path> result = DataBufferUtils.write(Mono.just(dataBuffer), channel)
                    .map(DataBufferUtils::release)
                    .then(Mono.just(filePath));
            result.doOnSuccess(path -> {
                // saving file handling from now read from file only
                logger.debug("Saving cache data into file");
                CacheFile cacheFile = cacheMap.get(filename);
                if(cacheFile != null) {
                    cacheMap.put(filename, cacheFile.saved(path.toString(), dataBuffer.capacity()));
                    logger.debug("Cache file was saved");
                }else {
                    logger.error("Cache record is missing");
                }

            }).doOnError(e -> {
                logger.error("Error saving cache file:"+e.getLocalizedMessage());
            }).subscribe();

        } catch (Exception e) {
            logger.error("Error saving cache file:"+e.getLocalizedMessage());
        }
    }

    /**
     * Reading cache from disk
     * @param filePath
     * @param size
     * @return
     */
    private Mono<byte[]> readFileFromDisk(String filePath, int size) {
        return DataBufferUtils.readAsynchronousFileChannel(() ->
                        AsynchronousFileChannel.open(Paths.get(filePath), StandardOpenOption.READ),
                new DefaultDataBufferFactory(), size)
                .next().map(result -> readBytesFromDataBuffer(result));
    }

    /**
     * In this project used DataBuffer to read and write data
     * This function is used for reading data as byte array
     * @param dataBuffer
     * @return
     */
    public byte[] readBytesFromDataBuffer(DataBuffer dataBuffer) {
        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(bytes);
        return bytes;
    }

    /**
     * Deleting all cached files on bean destruction
     * @param event
     */
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        logger.debug("Proxy server is finishing  work. Deleting cache files");
        cacheMap.forEach((k, v) -> new File(v.getPath()).delete());
    }
}
