package com.hamondev.shevery.api;

import androidx.annotation.RestrictTo;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

/**
 * Constants for the native Shevery Privilege API and the Shizuku Stub compatibility layer.
 * <p>
 * Native Shevery clients use {@link #PERMISSION_PRIVILEGED_API} and {@link #PROVIDER_SUFFIX},
 * while legacy {@code moe.shizuku.manager.permission.API_V23} requests are isolated in the
 * Shizuku Stub ({@link #SHIZUKU_STUB_APPLICATION_ID}).
 */
public final class SheveryApiConstants {

    private SheveryApiConstants() {
    }

    public static final int SERVER_VERSION = 14;
    public static final int SERVER_PATCH_VERSION = 5;

    // Binder interface
    public static final String BINDER_DESCRIPTOR = "moe.shizuku.server.IShizukuService";
    public static final int BINDER_TRANSACTION_transact = 1;

    // Package identifiers
    public static final String MANAGER_APPLICATION_ID = "com.hamondev.shevery";
    public static final String SHIZUKU_STUB_APPLICATION_ID = "moe.shizuku.privileged.api";

    // Native Shevery permissions (owned by com.hamondev.shevery)
    public static final String PERMISSION_GROUP_API = "com.hamondev.shevery.permission-group.API";
    public static final String PERMISSION_PRIVILEGED_API = "com.hamondev.shevery.permission.PRIVILEGED_API";
    public static final String PERMISSION_MANAGER = "com.hamondev.shevery.permission.MANAGER";

    // Legacy Shizuku permissions (offloaded to Shizuku Stub moe.shizuku.privileged.api)
    public static final String LEGACY_SHIZUKU_PERMISSION = "moe.shizuku.manager.permission.API_V23";
    public static final String LEGACY_SHIZUKU_PERMISSION_V22 = "moe.shizuku.manager.permission.API";
    public static final String LEGACY_SHIZUKU_MANAGER_PERMISSION = "moe.shizuku.manager.permission.MANAGER";

    // Client metadata markers
    public static final String META_DATA_CLIENT_SUPPORT = "com.hamondev.shevery.client.PRIVILEGED_API_SUPPORT";
    public static final String LEGACY_META_DATA_CLIENT_SUPPORT = "moe.shizuku.client.V3_SUPPORT";

    // ContentProvider authorities & methods
    public static final String PROVIDER_SUFFIX = ".shevery";
    public static final String LEGACY_PROVIDER_SUFFIX = ".shizuku";
    public static final String MANAGER_PROVIDER_AUTHORITY = MANAGER_APPLICATION_ID + PROVIDER_SUFFIX;
    public static final String MANAGER_LEGACY_PROVIDER_AUTHORITY = MANAGER_APPLICATION_ID + LEGACY_PROVIDER_SUFFIX;
    public static final String MANAGER_CONNECTOR_AUTHORITY = MANAGER_APPLICATION_ID + ".connector";

    public static final String METHOD_SEND_BINDER = "sendBinder";
    public static final String METHOD_GET_BINDER = "getBinder";
    public static final String METHOD_REQUEST_BINDER = "requestBinder";
    public static final String METHOD_SEND_USER_SERVICE = "sendUserService";

    // Extras & broadcast actions
    public static final String EXTRA_BINDER = "com.hamondev.shevery.intent.extra.BINDER";
    public static final String LEGACY_EXTRA_BINDER = "moe.shizuku.privileged.api.intent.extra.BINDER";
    public static final String EXTRA_PERMISSION_CHANNEL = "com.hamondev.shevery.intent.extra.PERMISSION_CHANNEL";
    public static final String EXTRA_REQUESTED_PERMISSION = "com.hamondev.shevery.intent.extra.REQUESTED_PERMISSION";

    public static final String ACTION_BINDER_RECEIVED = "com.hamondev.shevery.api.action.BINDER_RECEIVED";
    public static final String ACTION_REQUEST_PERMISSION = "com.hamondev.shevery.intent.action.REQUEST_PERMISSION";
    public static final String ACTION_REQUEST_BINDER = "com.hamondev.shevery.intent.action.REQUEST_BINDER";

    // Attach application channel negotiation
    public static final String ATTACH_APPLICATION_CLIENT_CHANNEL = "shevery:attach-client-channel";
    public static final String CHANNEL_SHEVERY_NATIVE = "shevery_native";
    public static final String CHANNEL_SHIZUKU_STUB = "shizuku_stub";

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static final String USER_SERVICE_ARG_TOKEN = "shizuku:user-service-arg-token";
}
