#include <shadowhook.h>
#include <sys/system_properties.h>
#include <android/log.h>
#include <string.h>
#include <jni.h>
#include <cstdio>

#define LOG_TAG "LocalTrace"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

int (*orig_system_property_get)(const char *, char *) = nullptr;


static JavaVM *g_jvm = nullptr;
static jclass g_HookConfigClass = nullptr;
static jmethodID g_getMethodIdMaxSize = nullptr;
static jmethodID g_getMethodIsMainThreadOnly = nullptr;

// 自定义的 __system_property_get 实现
int my_system_property_get(const char *name, char *value) {
    LOGI("Hooked __system_property_get: name=%s", name);

    if (strcmp(name, "debug.rhea.methodIdMaxSize") == 0) {
        JNIEnv *env;
        if (g_jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            LOGI("AttachCurrentThread failed!");
            return orig_system_property_get(name, value);
        }

        jlong maxSize = env->CallStaticLongMethod(g_HookConfigClass, g_getMethodIdMaxSize);
        snprintf(value, PROP_VALUE_MAX, "%ld", maxSize);
        return strlen(value);
    }

    if (strcmp(name, "debug.rhea.mainThreadOnly") == 0) {
        JNIEnv *env;
        if (g_jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            LOGI("AttachCurrentThread failed!");
            return orig_system_property_get(name, value);
        }

        jboolean mainThreadOnly = env->CallStaticBooleanMethod(g_HookConfigClass,
                                                               g_getMethodIsMainThreadOnly);
        snprintf(value, PROP_VALUE_MAX, "%s", mainThreadOnly ? "true" : "false");
        return strlen(value);
    }

    // 调用原始的 __system_property_get
    if (orig_system_property_get) {
        int result = orig_system_property_get(name, value);
        LOGI("Original __system_property_get result: name=%s, value=%s", name, value);
        return result;
    } else {
        LOGE("orig_system_property_get is NULL!");
        return 0;
    }
}

// 初始化 Hook
void init_hook() {
    LOGI("Initializing ShadowHook...");

    // 初始化 ShadowHook
    shadowhook_init(SHADOWHOOK_MODE_UNIQUE, true);

    // Hook __system_property_get
    shadowhook_hook_sym_name(
            "libc.so",
            "__system_property_get",
            (void *) my_system_property_get,
            (void **) &orig_system_property_get);

    if (orig_system_property_get) {
        LOGI("Hook __system_property_get success!");
    } else {
        LOGE("Failed to get original __system_property_get!");
    }
}

// 在 JNI_OnLoad 调用 Hook
JNIEXPORT jint

JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {

    JNIEnv *env;
    g_jvm = vm;

    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    jclass clazz = env->FindClass("com/jay/localtrace/HookConfig");  // 这里改成你的 Java 类的完整包名
    if (!clazz) return JNI_ERR;

    g_HookConfigClass = (jclass) env->NewGlobalRef(clazz);
    g_getMethodIdMaxSize = env->GetStaticMethodID(clazz, "getMethodIdMaxSize", "()J");
    g_getMethodIsMainThreadOnly = env->GetStaticMethodID(clazz, "isMainThreadOnly", "()Z");
    if (!g_getMethodIdMaxSize || !g_getMethodIsMainThreadOnly) {
        LOGI("Failed to find method IDs!");
        return JNI_ERR;
    }
    init_hook();
    return JNI_VERSION_1_6;
}
