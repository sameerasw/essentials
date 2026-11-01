package com.hamondev.shevery.api;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import moe.shizuku.server.IShizukuServiceConnection;

final class SheveryServiceConnection extends IShizukuServiceConnection.Stub {

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private final Set<ServiceConnection> connections = new CopyOnWriteArraySet<>();
    private final ComponentName componentName;
    private IBinder binder;
    private boolean dead = false;

    SheveryServiceConnection(SheveryPrivilege.UserServiceArgs args) {
        this.componentName = args.componentName;
    }

    void addConnection(@Nullable ServiceConnection conn) {
        if (conn != null) {
            connections.add(conn);
        }
    }

    void removeConnection(@Nullable ServiceConnection conn) {
        if (conn != null) {
            connections.remove(conn);
        }
    }

    void clearConnections() {
        connections.clear();
    }

    @Override
    public void connected(IBinder serviceBinder) {
        MAIN_HANDLER.post(() -> {
            for (ServiceConnection conn : connections) {
                conn.onServiceConnected(componentName, serviceBinder);
            }
        });

        this.binder = serviceBinder;
        try {
            this.binder.linkToDeath(this::died, 0);
        } catch (RemoteException ignored) {
        }
    }

    @Override
    public void died() {
        binder = null;
        if (dead) return;
        dead = true;

        MAIN_HANDLER.post(() -> {
            for (ServiceConnection conn : connections) {
                conn.onServiceDisconnected(componentName);
            }
            connections.clear();
            SheveryServiceConnections.remove(this);
        });
    }
}
