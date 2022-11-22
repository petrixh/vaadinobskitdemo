package com.example.application.data.service;

import java.util.concurrent.CompletableFuture;

public interface LeakyProtoService {
    int getLeakyListCount();

    long getBytesLeaked();

    void addToLeakyListSynchronized();

    CompletableFuture<Integer> addToLeakyListAsync();

    void clearLeakyList();
}
