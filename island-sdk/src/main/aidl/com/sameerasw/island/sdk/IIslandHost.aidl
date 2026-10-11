package com.sameerasw.island.sdk;

import android.graphics.Bitmap;
import android.os.Bundle;

oneway interface IIslandHost {
    void publish(int schema, String json, in Bundle assets);
    void request(String itemKey, String type, in Bundle args);
    void snapshot(String itemKey, in Bitmap bitmap);
}
