package com.jay.localtrace;

public class HookConfig {

    private static long methodIdMaxSize = 100L; // 默认值
    private static Boolean mainThreadOnly = false;

    public static void setMethodIdMaxSize(long size) {
        methodIdMaxSize = size;
    }

    public static long getMethodIdMaxSize() {
        return methodIdMaxSize;
    }

    public static void setMainThreadOnly(Boolean mainThreadOnly) {
        HookConfig.mainThreadOnly = mainThreadOnly;
    }

    public static boolean isMainThreadOnly() {
        return mainThreadOnly;
    }
}
