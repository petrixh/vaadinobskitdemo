package com.example.application.data.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

//Spring default is singleton...
@Service
public class LeakyProtoInterfaceImpl implements LeakyProtoService {

    public static Logger logger = LoggerFactory.getLogger(LeakyProtoInterfaceImpl.class);

    //Memory leak list...
    private List<byte[]> leak = new ArrayList<>();

    @Override
    public synchronized int getLeakyListCount() {
        return leak.size();
    }

    @Override
    public synchronized long getBytesLeaked(){

        long totalBytes = 0l;
        for(int i=0; i<leak.size(); i++){
            totalBytes += leak.get(i).length;
        }

        return totalBytes;
    }

    @Override
    public synchronized void addToLeakyListSynchronized() {
        //Leaking memory...
        int bytesPerLeak = 1024 * 1024 * 1024; //1GB of ram...

        //Use all -1 threads of the system...
        int endExclusive = getLeakyThingsThreadCount();
        int bytesPerThread = bytesPerLeak / endExclusive;

        logger.info("Using {} threads to leak {} MB of ram...", endExclusive, (bytesPerLeak / 1024 / 1024));
        //Yes there are better ways of spawning threads :D
        IntStream.range(0, endExclusive).parallel().forEach(value -> {
            logger.info("Thread {} allocating {} bytes ({} MB)", Thread.currentThread().getId(), bytesPerThread, (bytesPerThread / 1024 / 1024));
            byte[] randomBytes = new byte[bytesPerThread];
            new Random().nextBytes(randomBytes);
            leak.add(randomBytes);
        });

        int leakyListCount = getLeakyListCount();

        logger.info("Done leaking ram... Leaky list now has {} byte arrays, totalling {} MB", leakyListCount, (getBytesLeaked() / 1024L / 1024L));

    }

    @Override
    public synchronized CompletableFuture<Integer> addToLeakyListAsync() {
        CompletableFuture<Integer> future = new CompletableFuture<>();

        future.completeAsync(() -> {
            addToLeakyListSynchronized();
            return leak.size();
        });

        return future;
    }

    @Override
    public synchronized void clearLeakyList(){
        leak.clear();
    }

    public static int getLeakyThingsThreadCount(){
        int systemThreads = Runtime.getRuntime().availableProcessors();
        return systemThreads < 2 ? 1 : systemThreads - 1;
    }

}
