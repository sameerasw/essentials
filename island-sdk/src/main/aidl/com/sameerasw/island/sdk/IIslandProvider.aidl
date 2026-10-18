package com.sameerasw.island.sdk;

import android.os.Bundle;
import android.view.MotionEvent;
import com.sameerasw.island.sdk.IIslandHost;
import com.sameerasw.island.sdk.ISurfaceCallback;

interface IIslandProvider {
    int getSchemaVersion();
    boolean isHostAuthorized();
    boolean grantHost();
    oneway void revokeHost();
    oneway void attach(IIslandHost host, in Bundle hostInfo);
    oneway void detach();
    oneway void onHostInfoChanged(in Bundle hostInfo);
    oneway void onAction(String itemKey, String actionId);
    oneway void requestSurface(String itemKey, in Bundle request, ISurfaceCallback callback);
    oneway void resizeSurface(String itemKey, int widthPx, int heightPx);
    oneway void releaseSurface(String itemKey);
    oneway void onSurfaceTouch(String itemKey, in MotionEvent event);
}
