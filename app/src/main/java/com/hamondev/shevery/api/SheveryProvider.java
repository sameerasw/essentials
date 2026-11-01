package com.hamondev.shevery.api;

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import moe.shizuku.api.BinderContainer;

/**
 * Native ContentProvider for the Shevery Privilege API ({@code ${applicationId}.shevery}).
 * <p>
 * Unlike the legacy {@code ShizukuProvider}, {@code SheveryProvider} supports both
 * proactive fast-pull binder acquisition on startup ({@link SheveryPrivilege#connectFast(Context)})
 * and low-latency server push delivery under {@link SheveryApiConstants#PERMISSION_PRIVILEGED_API}.
 */
public class SheveryProvider extends ContentProvider {

    private static final String TAG = "SheveryProvider";

    public static final String METHOD_SEND_BINDER = SheveryApiConstants.METHOD_SEND_BINDER;
    public static final String METHOD_GET_BINDER = SheveryApiConstants.METHOD_GET_BINDER;
    public static final String METHOD_REQUEST_BINDER = SheveryApiConstants.METHOD_REQUEST_BINDER;
    public static final String ACTION_BINDER_RECEIVED = SheveryApiConstants.ACTION_BINDER_RECEIVED;
    public static final String PERMISSION = SheveryApiConstants.PERMISSION_PRIVILEGED_API;
    public static final String MANAGER_APPLICATION_ID = SheveryApiConstants.MANAGER_APPLICATION_ID;

    private static volatile boolean enableMultiProcess = false;
    private static volatile boolean isProviderProcess = false;
    private static BroadcastReceiver multiProcessReceiver;

    public static void enableMultiProcessSupport(boolean isProviderProcess) {
        SheveryProvider.isProviderProcess = isProviderProcess;
        SheveryProvider.enableMultiProcess = true;
    }

    public static void requestBinderForNonProviderProcess(@NonNull Context context) {
        if (isProviderProcess) {
            return;
        }
        if (multiProcessReceiver == null) {
            multiProcessReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent intent) {
                    intent.setExtrasClassLoader(BinderContainer.class.getClassLoader());
                    BinderContainer container = intent.getParcelableExtra(SheveryApiConstants.EXTRA_BINDER);
                    if (container == null) {
                        container = intent.getParcelableExtra(SheveryApiConstants.LEGACY_EXTRA_BINDER);
                    }
                    if (container != null && container.binder != null) {
                        SheveryPrivilege.onBinderReceived(container.binder, ctx.getPackageName());
                    }
                }
            };
            IntentFilter filter = new IntentFilter(ACTION_BINDER_RECEIVED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.getApplicationContext().registerReceiver(multiProcessReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                context.getApplicationContext().registerReceiver(multiProcessReceiver, filter);
            }
        }

        if (SheveryPrivilege.connectFast(context)) {
            return;
        }

        try {
            Bundle reply = context.getContentResolver().call(
                    Uri.parse("content://" + context.getPackageName() + SheveryApiConstants.PROVIDER_SUFFIX),
                    METHOD_GET_BINDER,
                    null,
                    new Bundle()
            );
            if (reply != null) {
                reply.setClassLoader(BinderContainer.class.getClassLoader());
                BinderContainer container = reply.getParcelable(SheveryApiConstants.EXTRA_BINDER);
                if (container != null && container.binder != null) {
                    SheveryPrivilege.onBinderReceived(container.binder, context.getPackageName());
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        super.attachInfo(context, info);
        if (info.multiprocess) {
            throw new IllegalStateException("android:multiprocess must be false");
        }
        if (!info.exported) {
            throw new IllegalStateException("android:exported must be true");
        }
        isProviderProcess = true;
    }

    @Override
    public boolean onCreate() {
        final Context ctx = getContext();
        if (ctx != null && !MANAGER_APPLICATION_ID.equals(ctx.getPackageName())) {
            // Non-blocking fast pull from Shevery Manager if already running
            try {
                SheveryPrivilege.connectFast(ctx);
            } catch (Throwable ignored) {
            }
        }
        return true;
    }

    @Nullable
    @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        if (extras == null) {
            extras = new Bundle();
        }
        extras.setClassLoader(BinderContainer.class.getClassLoader());

        Bundle reply = new Bundle();
        switch (method) {
            case METHOD_SEND_BINDER: {
                handleSendBinder(extras);
                break;
            }
            case METHOD_GET_BINDER:
            case METHOD_REQUEST_BINDER: {
                if (!handleGetBinder(reply)) {
                    return null;
                }
                break;
            }
            default:
                return null;
        }
        return reply;
    }

    private void handleSendBinder(@NonNull Bundle extras) {
        if (SheveryPrivilege.pingBinder()) {
            return;
        }
        BinderContainer container = extras.getParcelable(SheveryApiConstants.EXTRA_BINDER);
        if (container == null) {
            container = extras.getParcelable(SheveryApiConstants.LEGACY_EXTRA_BINDER);
        }
        Context ctx = getContext();
        if (container != null && container.binder != null && ctx != null) {
            Log.d(TAG, "Native Shevery binder received for " + ctx.getPackageName());
            SheveryPrivilege.onBinderReceived(container.binder, ctx.getPackageName());

            if (enableMultiProcess) {
                Intent intent = new Intent(ACTION_BINDER_RECEIVED)
                        .putExtra(SheveryApiConstants.EXTRA_BINDER, container)
                        .setPackage(ctx.getPackageName());
                ctx.sendBroadcast(intent);
            }
        }
    }

    private boolean handleGetBinder(@NonNull Bundle reply) {
        IBinder binder = SheveryPrivilege.getBinder();
        if (binder == null || !binder.pingBinder()) {
            return false;
        }
        BinderContainer container = new BinderContainer(binder);
        reply.putParcelable(SheveryApiConstants.EXTRA_BINDER, container);
        reply.putParcelable(SheveryApiConstants.LEGACY_EXTRA_BINDER, container);
        return true;
    }

    @Nullable
    @Override
    public final Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public final String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public final Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public final int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public final int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
}
