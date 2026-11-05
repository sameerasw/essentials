package com.hamondev.shevery.api;

import androidx.annotation.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class SheveryServiceConnections {

    private static final Map<String, SheveryServiceConnection> CACHE = new ConcurrentHashMap<>();

    private SheveryServiceConnections() {
    }

    @NonNull
    static SheveryServiceConnection get(SheveryPrivilege.UserServiceArgs args) {
        String key = args.tag != null ? args.tag : args.componentName.getClassName();
        return CACHE.computeIfAbsent(key, k -> new SheveryServiceConnection(args));
    }

    static void remove(SheveryServiceConnection connection) {
        CACHE.entrySet().removeIf(entry -> entry.getValue() == connection);
    }
}
