package com.hamondev.shevery.api;

import android.os.RemoteException;

/**
 * System properties access through the native Shevery Privilege API.
 */
public final class SheverySystemProperties {

    private SheverySystemProperties() {
    }

    public static String get(String key) throws RemoteException {
        return SheveryPrivilege.requireService().getSystemProperty(key, null);
    }

    public static String get(String key, String def) throws RemoteException {
        return SheveryPrivilege.requireService().getSystemProperty(key, def);
    }

    public static int getInt(String key, int def) throws RemoteException {
        return Integer.decode(SheveryPrivilege.requireService().getSystemProperty(key, Integer.toString(def)));
    }

    public static long getLong(String key, long def) throws RemoteException {
        return Long.decode(SheveryPrivilege.requireService().getSystemProperty(key, Long.toString(def)));
    }

    public static boolean getBoolean(String key, boolean def) throws RemoteException {
        return Boolean.parseBoolean(SheveryPrivilege.requireService().getSystemProperty(key, Boolean.toString(def)));
    }

    public static void set(String key, String val) throws RemoteException {
        SheveryPrivilege.requireService().setSystemProperty(key, val);
    }
}
