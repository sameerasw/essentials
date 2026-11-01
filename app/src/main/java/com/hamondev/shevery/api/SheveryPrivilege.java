package com.hamondev.shevery.api;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;
import static rikka.shizuku.ShizukuApiConstants.ATTACH_APPLICATION_API_VERSION;
import static rikka.shizuku.ShizukuApiConstants.ATTACH_APPLICATION_PACKAGE_NAME;
import static rikka.shizuku.ShizukuApiConstants.BIND_APPLICATION_PERMISSION_GRANTED;
import static rikka.shizuku.ShizukuApiConstants.BIND_APPLICATION_SERVER_PATCH_VERSION;
import static rikka.shizuku.ShizukuApiConstants.BIND_APPLICATION_SERVER_SECONTEXT;
import static rikka.shizuku.ShizukuApiConstants.BIND_APPLICATION_SERVER_UID;
import static rikka.shizuku.ShizukuApiConstants.BIND_APPLICATION_SERVER_VERSION;
import static rikka.shizuku.ShizukuApiConstants.BIND_APPLICATION_SHOULD_SHOW_REQUEST_PERMISSION_RATIONALE;
import static rikka.shizuku.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_ALLOWED;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;

import moe.shizuku.api.BinderContainer;
import moe.shizuku.server.IShizukuApplication;
import moe.shizuku.server.IShizukuService;
import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuApiConstants;
import rikka.shizuku.SystemServiceHelper;

/**
 * Native, low-latency Privilege API for Shevery ({@code com.hamondev.shevery.permission.PRIVILEGED_API}).
 * <p>
 * Key improvements over legacy Shizuku API:
 * <ul>
 *     <li><b>Fast Pull Acquisition ({@link #connectFast(Context)})</b>: Directly pulls the active
 *     server binder from Shevery's provider in a single IPC call without waiting for
 *     {@code IActivityManager} process/UID observers or retry backoffs.</li>
 *     <li><b>Lock-Free Listener Dispatch</b>: Uses {@link CopyOnWriteArraySet} for binder and
 *     permission callbacks instead of a single contended global lock.</li>
 *     <li><b>Dedicated Permission Scope</b>: Operates under {@link SheveryApiConstants#PERMISSION_PRIVILEGED_API},
 *     leaving {@code moe.shizuku.manager.permission.API_V23} isolated to the Shizuku Stub.</li>
 * </ul>
 */
public final class SheveryPrivilege {

    private static final String TAG = "SheveryPrivilege";
    private static final int TRANSACTION_attachApplication_v13 = 18;

    private static volatile IBinder binder;
    private static volatile IShizukuService service;

    private static volatile int serverUid = -1;
    private static volatile int serverApiVersion = -1;
    private static volatile int serverPatchVersion = -1;
    private static volatile String serverContext = null;
    private static volatile boolean permissionGranted = false;
    private static volatile boolean shouldShowRequestPermissionRationale = false;
    private static volatile boolean binderReady = false;

    public interface OnBinderReceivedListener {
        void onBinderReceived();
    }

    public interface OnBinderDeadListener {
        void onBinderDead();
    }

    public interface OnPermissionResultListener {
        void onPermissionResult(int requestCode, int grantResult);
    }

    private static final class ListenerEntry<T> {
        final T listener;
        final Handler handler;

        ListenerEntry(@NonNull T listener, @Nullable Handler handler) {
            this.listener = listener;
            this.handler = handler;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ListenerEntry)) return false;
            ListenerEntry<?> that = (ListenerEntry<?>) o;
            return Objects.equals(listener, that.listener) && Objects.equals(handler, that.handler);
        }

        @Override
        public int hashCode() {
            return Objects.hash(listener, handler);
        }
    }

    private static final CopyOnWriteArraySet<ListenerEntry<OnBinderReceivedListener>> RECEIVED_LISTENERS = new CopyOnWriteArraySet<>();
    private static final CopyOnWriteArraySet<ListenerEntry<OnBinderDeadListener>> DEAD_LISTENERS = new CopyOnWriteArraySet<>();
    private static final CopyOnWriteArraySet<ListenerEntry<OnPermissionResultListener>> PERMISSION_LISTENERS = new CopyOnWriteArraySet<>();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private static final IShizukuApplication SHEVERY_APPLICATION = new IShizukuApplication.Stub() {

        @Override
        public void bindApplication(Bundle data) {
            serverUid = data.getInt(BIND_APPLICATION_SERVER_UID, -1);
            serverApiVersion = data.getInt(BIND_APPLICATION_SERVER_VERSION, -1);
            serverPatchVersion = data.getInt(BIND_APPLICATION_SERVER_PATCH_VERSION, -1);
            serverContext = data.getString(BIND_APPLICATION_SERVER_SECONTEXT);
            permissionGranted = data.getBoolean(BIND_APPLICATION_PERMISSION_GRANTED, false);
            shouldShowRequestPermissionRationale = data.getBoolean(BIND_APPLICATION_SHOULD_SHOW_REQUEST_PERMISSION_RATIONALE, false);

            binderReady = true;
            Shizuku.syncFromShevery(
                    binder,
                    serverUid,
                    serverApiVersion,
                    serverPatchVersion,
                    serverContext,
                    permissionGranted,
                    shouldShowRequestPermissionRationale
            );
            dispatchBinderReceived();
        }

        @Override
        public void dispatchRequestPermissionResult(int requestCode, Bundle data) {
            boolean allowed = data != null && data.getBoolean(REQUEST_PERMISSION_REPLY_ALLOWED, false);
            permissionGranted = allowed;
            if (allowed) {
                shouldShowRequestPermissionRationale = false;
            }
            int result = allowed ? PackageManager.PERMISSION_GRANTED : PackageManager.PERMISSION_DENIED;
            Shizuku.syncPermissionResultFromShevery(requestCode, allowed);
            dispatchPermissionResult(requestCode, result);
        }

        @Override
        public void showPermissionConfirmation(int requestUid, int requestPid, String requestPackageName, int requestCode) {
        }
    };

    private static final IBinder.DeathRecipient DEATH_RECIPIENT = () -> {
        binderReady = false;
        onBinderReceived(null, null);
    };

    private SheveryPrivilege() {
    }

    /**
     * Fast-path pull acquisition: queries Shevery Manager's provider directly to obtain the
     * active server binder immediately without waiting for server-side process observers.
     *
     * @param context Application or Activity context
     * @return {@code true} if a live binder is already attached or was pulled immediately
     */
    public static boolean connectFast(@NonNull Context context) {
        if (pingBinder()) {
            return true;
        }
        final String pkg = context.getPackageName();
        final String[] authorities = new String[]{
                SheveryApiConstants.MANAGER_PROVIDER_AUTHORITY,
                SheveryApiConstants.MANAGER_LEGACY_PROVIDER_AUTHORITY,
                pkg + SheveryApiConstants.PROVIDER_SUFFIX
        };

        for (String authority : authorities) {
            try {
                Bundle extras = new Bundle();
                extras.putString(ATTACH_APPLICATION_PACKAGE_NAME, pkg);
                extras.putString(SheveryApiConstants.ATTACH_APPLICATION_CLIENT_CHANNEL, SheveryApiConstants.CHANNEL_SHEVERY_NATIVE);
                Bundle reply = context.getContentResolver().call(
                        Uri.parse("content://" + authority),
                        SheveryApiConstants.METHOD_REQUEST_BINDER,
                        pkg,
                        extras
                );
                if (reply != null) {
                    reply.setClassLoader(BinderContainer.class.getClassLoader());
                    BinderContainer container = reply.getParcelable(SheveryApiConstants.EXTRA_BINDER);
                    if (container == null) {
                        container = reply.getParcelable(SheveryApiConstants.LEGACY_EXTRA_BINDER);
                    }
                    if (container != null && container.binder != null && container.binder.pingBinder()) {
                        onBinderReceived(container.binder, pkg);
                        return true;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return pingBinder();
    }

    private static boolean attachApplicationFast(@NonNull IBinder targetBinder, @NonNull String packageName) throws RemoteException {
        Bundle args = new Bundle();
        args.putInt(ATTACH_APPLICATION_API_VERSION, SheveryApiConstants.SERVER_VERSION);
        args.putString(ATTACH_APPLICATION_PACKAGE_NAME, packageName);
        args.putString(SheveryApiConstants.ATTACH_APPLICATION_CLIENT_CHANNEL, SheveryApiConstants.CHANNEL_SHEVERY_NATIVE);
        args.putString(SheveryApiConstants.EXTRA_REQUESTED_PERMISSION, SheveryApiConstants.PERMISSION_PRIVILEGED_API);

        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(SheveryApiConstants.BINDER_DESCRIPTOR);
            data.writeStrongBinder(SHEVERY_APPLICATION.asBinder());
            data.writeInt(1);
            args.writeToParcel(data, 0);
            boolean result = targetBinder.transact(TRANSACTION_attachApplication_v13, data, reply, 0);
            reply.readException();
            return result;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static synchronized void onBinderReceived(@Nullable IBinder newBinder, @Nullable String packageName) {
        if (binder == newBinder && (newBinder == null || newBinder.pingBinder())) {
            return;
        }

        if (newBinder == null) {
            if (binder != null) {
                try {
                    binder.unlinkToDeath(DEATH_RECIPIENT, 0);
                } catch (Throwable ignored) {
                }
            }
            binder = null;
            service = null;
            serverUid = -1;
            serverApiVersion = -1;
            serverPatchVersion = -1;
            serverContext = null;
            permissionGranted = false;
            binderReady = false;

            dispatchBinderDead();
            return;
        }

        if (binder != null) {
            try {
                binder.unlinkToDeath(DEATH_RECIPIENT, 0);
            } catch (Throwable ignored) {
            }
        }

        binder = newBinder;
        service = IShizukuService.Stub.asInterface(newBinder);

        try {
            newBinder.linkToDeath(DEATH_RECIPIENT, 0);
        } catch (Throwable e) {
            Log.w(TAG, "linkToDeath failed", e);
        }

        if (packageName != null) {
            try {
                attachApplicationFast(newBinder, packageName);
            } catch (Throwable e) {
                Log.e(TAG, "attachApplicationFast failed for " + packageName, e);
            }
        }
    }

    /**
     * Synchronizes state when {@link Shizuku#onBinderReceived(IBinder, String)} attaches first,
     * avoiding a duplicate {@code attachApplication} Binder transaction.
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static void syncFromShizuku(
            @Nullable IBinder newBinder,
            int uid,
            int apiVer,
            int patchVer,
            @Nullable String seContext,
            boolean granted,
            boolean showRationale
    ) {
        binder = newBinder;
        service = newBinder != null ? IShizukuService.Stub.asInterface(newBinder) : null;
        serverUid = uid;
        serverApiVersion = apiVer;
        serverPatchVersion = patchVer;
        serverContext = seContext;
        permissionGranted = granted;
        shouldShowRequestPermissionRationale = showRationale;
        binderReady = newBinder != null && newBinder.pingBinder();
        if (binderReady) {
            dispatchBinderReceived();
        } else {
            dispatchBinderDead();
        }
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static void syncPermissionResultFromShizuku(int requestCode, boolean allowed) {
        permissionGranted = allowed;
        if (allowed) {
            shouldShowRequestPermissionRationale = false;
        }
        dispatchPermissionResult(
                requestCode,
                allowed ? PackageManager.PERMISSION_GRANTED : PackageManager.PERMISSION_DENIED
        );
    }

    public static void addBinderReceivedListener(@NonNull OnBinderReceivedListener listener) {
        addBinderReceivedListener(listener, null);
    }

    public static void addBinderReceivedListener(@NonNull OnBinderReceivedListener listener, @Nullable Handler handler) {
        RECEIVED_LISTENERS.add(new ListenerEntry<>(Objects.requireNonNull(listener), handler));
    }

    public static void addBinderReceivedListenerSticky(@NonNull OnBinderReceivedListener listener) {
        addBinderReceivedListenerSticky(listener, null);
    }

    public static void addBinderReceivedListenerSticky(@NonNull OnBinderReceivedListener listener, @Nullable Handler handler) {
        Objects.requireNonNull(listener);
        RECEIVED_LISTENERS.add(new ListenerEntry<>(listener, handler));
        if (binderReady && pingBinder()) {
            postToHandler(handler, listener::onBinderReceived);
        }
    }

    public static boolean removeBinderReceivedListener(@NonNull OnBinderReceivedListener listener) {
        return RECEIVED_LISTENERS.removeIf(entry -> entry.listener == listener);
    }

    public static void addBinderDeadListener(@NonNull OnBinderDeadListener listener) {
        addBinderDeadListener(listener, null);
    }

    public static void addBinderDeadListener(@NonNull OnBinderDeadListener listener, @Nullable Handler handler) {
        DEAD_LISTENERS.add(new ListenerEntry<>(Objects.requireNonNull(listener), handler));
    }

    public static boolean removeBinderDeadListener(@NonNull OnBinderDeadListener listener) {
        return DEAD_LISTENERS.removeIf(entry -> entry.listener == listener);
    }

    public static void addPermissionResultListener(@NonNull OnPermissionResultListener listener) {
        addPermissionResultListener(listener, null);
    }

    public static void addPermissionResultListener(@NonNull OnPermissionResultListener listener, @Nullable Handler handler) {
        PERMISSION_LISTENERS.add(new ListenerEntry<>(Objects.requireNonNull(listener), handler));
    }

    public static boolean removePermissionResultListener(@NonNull OnPermissionResultListener listener) {
        return PERMISSION_LISTENERS.removeIf(entry -> entry.listener == listener);
    }

    private static void postToHandler(@Nullable Handler handler, @NonNull Runnable action) {
        if (handler != null) {
            handler.post(action);
        } else if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run();
        } else {
            MAIN_HANDLER.post(action);
        }
    }

    private static void dispatchBinderReceived() {
        for (ListenerEntry<OnBinderReceivedListener> entry : RECEIVED_LISTENERS) {
            postToHandler(entry.handler, entry.listener::onBinderReceived);
        }
    }

    private static void dispatchBinderDead() {
        for (ListenerEntry<OnBinderDeadListener> entry : DEAD_LISTENERS) {
            postToHandler(entry.handler, entry.listener::onBinderDead);
        }
    }

    private static void dispatchPermissionResult(int requestCode, int result) {
        for (ListenerEntry<OnPermissionResultListener> entry : PERMISSION_LISTENERS) {
            postToHandler(entry.handler, () -> entry.listener.onPermissionResult(requestCode, result));
        }
    }

    @NonNull
    public static IShizukuService requireService() {
        IShizukuService current = service;
        if (current == null) {
            IBinder shizukuBinder = Shizuku.getBinder();
            if (shizukuBinder != null && shizukuBinder.pingBinder()) {
                current = IShizukuService.Stub.asInterface(shizukuBinder);
                binder = shizukuBinder;
                service = current;
            }
        }
        if (current == null) {
            throw new IllegalStateException("Shevery binder has not been received yet");
        }
        return current;
    }

    @Nullable
    public static IBinder getBinder() {
        IBinder current = binder;
        if (current != null && current.pingBinder()) {
            return current;
        }
        return Shizuku.getBinder();
    }

    public static boolean pingBinder() {
        IBinder current = getBinder();
        return current != null && current.pingBinder();
    }

    @NonNull
    public static SheveryBinderWrapper wrapBinder(@NonNull IBinder original) {
        return new SheveryBinderWrapper(original);
    }

    @Nullable
    public static IBinder getSystemService(@NonNull String name) {
        IBinder raw = SystemServiceHelper.getSystemService(name);
        return raw != null ? new SheveryBinderWrapper(raw) : null;
    }

    public static void transactRemote(@NonNull Parcel data, @Nullable Parcel reply, int flags) {
        try {
            requireService().asBinder().transact(SheveryApiConstants.BINDER_TRANSACTION_transact, data, reply, flags);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    public static SheveryRemoteProcess newProcess(@NonNull String[] cmd, @Nullable String[] env, @Nullable String dir) {
        try {
            return new SheveryRemoteProcess(requireService().newProcess(cmd, env, dir));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public static final class UserServiceArgs {

        final ComponentName componentName;
        int versionCode = 1;
        String processName = "";
        String tag;
        boolean debuggable = false;
        boolean daemon = true;
        boolean use32BitAppProcess = false;

        public UserServiceArgs(@NonNull ComponentName componentName) {
            this.componentName = Objects.requireNonNull(componentName, "componentName must not be null");
        }

        public UserServiceArgs daemon(boolean daemon) {
            this.daemon = daemon;
            return this;
        }

        public UserServiceArgs tag(@NonNull String tag) {
            this.tag = Objects.requireNonNull(tag, "tag must not be null");
            return this;
        }

        public UserServiceArgs version(int versionCode) {
            this.versionCode = versionCode;
            return this;
        }

        public UserServiceArgs debuggable(boolean debuggable) {
            this.debuggable = debuggable;
            return this;
        }

        public UserServiceArgs processNameSuffix(@NonNull String processNameSuffix) {
            this.processName = Objects.requireNonNull(processNameSuffix, "processNameSuffix must not be null");
            return this;
        }

        Bundle forAdd() {
            Bundle options = new Bundle();
            options.putParcelable(ShizukuApiConstants.USER_SERVICE_ARG_COMPONENT, componentName);
            options.putBoolean(ShizukuApiConstants.USER_SERVICE_ARG_DEBUGGABLE, debuggable);
            options.putInt(ShizukuApiConstants.USER_SERVICE_ARG_VERSION_CODE, versionCode);
            options.putBoolean(ShizukuApiConstants.USER_SERVICE_ARG_DAEMON, daemon);
            options.putBoolean(ShizukuApiConstants.USER_SERVICE_ARG_USE_32_BIT_APP_PROCESS, use32BitAppProcess);
            options.putString(ShizukuApiConstants.USER_SERVICE_ARG_PROCESS_NAME, processName);
            if (tag != null) {
                options.putString(ShizukuApiConstants.USER_SERVICE_ARG_TAG, tag);
            }
            return options;
        }

        Bundle forRemove(boolean remove) {
            Bundle options = new Bundle();
            options.putParcelable(ShizukuApiConstants.USER_SERVICE_ARG_COMPONENT, componentName);
            if (tag != null) {
                options.putString(ShizukuApiConstants.USER_SERVICE_ARG_TAG, tag);
            }
            options.putBoolean(ShizukuApiConstants.USER_SERVICE_ARG_REMOVE, remove);
            return options;
        }
    }

    public static void bindUserService(@NonNull UserServiceArgs args, @NonNull ServiceConnection conn) {
        SheveryServiceConnection connection = SheveryServiceConnections.get(args);
        connection.addConnection(conn);
        try {
            requireService().addUserService(connection, args.forAdd());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public static int peekUserService(@NonNull UserServiceArgs args, @NonNull ServiceConnection conn) {
        SheveryServiceConnection connection = SheveryServiceConnections.get(args);
        connection.addConnection(conn);
        try {
            Bundle bundle = args.forAdd();
            bundle.putBoolean(ShizukuApiConstants.USER_SERVICE_ARG_NO_CREATE, true);
            return requireService().addUserService(connection, bundle);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public static void unbindUserService(@NonNull UserServiceArgs args, @Nullable ServiceConnection conn, boolean remove) {
        if (remove) {
            try {
                requireService().removeUserService(null, args.forRemove(true));
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        } else {
            SheveryServiceConnection connection = SheveryServiceConnections.get(args);
            try {
                requireService().removeUserService(connection, args.forRemove(false));
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
            connection.clearConnections();
            SheveryServiceConnections.remove(connection);
        }
    }

    public static boolean isGranted() {
        return checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
    }

    public static int checkSelfPermission() {
        if (permissionGranted) {
            return PackageManager.PERMISSION_GRANTED;
        }
        try {
            permissionGranted = requireService().checkSelfPermission();
            return permissionGranted ? PackageManager.PERMISSION_GRANTED : PackageManager.PERMISSION_DENIED;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public static void requestPermission(int requestCode) {
        try {
            requireService().requestPermission(requestCode);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean shouldShowRequestPermissionRationale() {
        if (permissionGranted) return false;
        if (shouldShowRequestPermissionRationale) return true;
        try {
            shouldShowRequestPermissionRationale = requireService().shouldShowRequestPermissionRationale();
            return shouldShowRequestPermissionRationale;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public static int checkRemotePermission(@NonNull String permission) {
        if (serverUid == 0) return PackageManager.PERMISSION_GRANTED;
        try {
            return requireService().checkPermission(permission);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public static int getUid() {
        if (serverUid != -1) return serverUid;
        try {
            serverUid = requireService().getUid();
            return serverUid;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e) {
            return -1;
        }
    }

    public static int getVersion() {
        if (serverApiVersion != -1) return serverApiVersion;
        try {
            serverApiVersion = requireService().getVersion();
            return serverApiVersion;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e) {
            return -1;
        }
    }

    public static int getLatestServiceVersion() {
        return SheveryApiConstants.SERVER_VERSION;
    }

    public static int getServerPatchVersion() {
        return serverPatchVersion;
    }

    @Nullable
    public static String getSELinuxContext() {
        if (serverContext != null) return serverContext;
        try {
            serverContext = requireService().getSELinuxContext();
            return serverContext;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e) {
            return null;
        }
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static void exit() {
        try {
            requireService().exit();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static void attachUserService(@NonNull IBinder serviceBinder, @NonNull Bundle options) {
        try {
            requireService().attachUserService(serviceBinder, options);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static void dispatchPermissionConfirmationResult(int requestUid, int requestPid, int requestCode, @NonNull Bundle data) {
        try {
            requireService().dispatchPermissionConfirmationResult(requestUid, requestPid, requestCode, data);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static int getFlagsForUid(int uid, int mask) {
        try {
            return requireService().getFlagsForUid(uid, mask);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static void updateFlagsForUid(int uid, int mask, int value) {
        try {
            requireService().updateFlagsForUid(uid, mask, value);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }
}
