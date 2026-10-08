#include "native_root.hpp"

#include <jni.h>
#include <windows.h>

#include <algorithm>
#include <cstdint>
#include <limits>
#include <string>
#include <vector>

#define RONOVA_JNI_EXPORT extern "C" __declspec(dllexport)

namespace {
constexpr std::size_t MAX_PAYLOAD = 64U * 1024U * 1024U;
constexpr jsize STATUS_SIZE = 14;
volatile LONG g_watchdog_running = 0;
volatile LONG g_watchdog_interval = 30000;

DWORD WINAPI root_watchdog_main(void*) {
    while (InterlockedCompareExchange(&g_watchdog_running, 1, 1) == 1) {
        Sleep(static_cast<DWORD>(InterlockedCompareExchange(&g_watchdog_interval, 0, 0)));
        if (InterlockedCompareExchange(&g_watchdog_running, 1, 1) == 1) {
            ronova::root::watchdog_pulse();
        }
    }
    return 0;
}

std::string from_jstring(JNIEnv* env, jstring value) {
    if (value == nullptr) return {};
    const char* raw = env->GetStringUTFChars(value, nullptr);
    if (raw == nullptr) {
        if (env->ExceptionCheck()) env->ExceptionClear();
        return {};
    }
    std::string result(raw);
    env->ReleaseStringUTFChars(value, raw);
    return result;
}

jlong pack(const ronova::root::Decision& decision) {
    if (!decision.available) return -1;
    return static_cast<jlong>((decision.accepted ? 1ULL : 0ULL)
        | ((static_cast<std::uint64_t>(decision.scope) & 0x3ULL) << 1U)
        | ((static_cast<std::uint64_t>(decision.reason) & 0xffULL) << 8U));
}
} // namespace

RONOVA_JNI_EXPORT jint JNICALL
Java_dev_ronova_coremod_nativebridge_NativeRootBridge_installSnapshot(
    JNIEnv* env, jclass, jbyteArray source
) {
    if (source == nullptr) return static_cast<jint>(ronova::root::Publication::Failed);
    const jsize length = env->GetArrayLength(source);
    if (length <= 0 || static_cast<std::size_t>(length) > MAX_PAYLOAD) {
        return static_cast<jint>(ronova::root::Publication::Failed);
    }
    std::vector<unsigned char> payload(static_cast<std::size_t>(length));
    env->GetByteArrayRegion(
        source, 0, length, reinterpret_cast<jbyte*>(payload.data())
    );
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return static_cast<jint>(ronova::root::Publication::Failed);
    }
    return static_cast<jint>(ronova::root::install(payload));
}

RONOVA_JNI_EXPORT jbyteArray JNICALL
Java_dev_ronova_coremod_nativebridge_NativeRootBridge_exportSnapshot(JNIEnv* env, jclass) {
    const std::vector<unsigned char> payload = ronova::root::export_snapshot();
    if (payload.empty()
        || payload.size() > static_cast<std::size_t>(std::numeric_limits<jsize>::max())) {
        return nullptr;
    }
    jbyteArray result = env->NewByteArray(static_cast<jsize>(payload.size()));
    if (result != nullptr) {
        env->SetByteArrayRegion(
            result, 0, static_cast<jsize>(payload.size()),
            reinterpret_cast<const jbyte*>(payload.data())
        );
    }
    return result;
}

RONOVA_JNI_EXPORT jlong JNICALL
Java_dev_ronova_coremod_nativebridge_NativeRootBridge_decide0(
    JNIEnv* env, jclass, jint domain, jlong generation, jlong sequence,
    jboolean identified, jlong uuid_most, jlong uuid_least,
    jstring registry_type, jstring runtime_class, jstring dimension,
    jdouble x, jdouble y, jdouble z, jboolean player, jlong facts,
    jint, jdouble
) {
    ronova::root::Request request;
    request.domain = domain;
    request.generation = generation;
    request.sequence = sequence;
    request.identified = identified == JNI_TRUE;
    request.uuid_most = uuid_most;
    request.uuid_least = uuid_least;
    request.registry_type = from_jstring(env, registry_type);
    request.runtime_class = from_jstring(env, runtime_class);
    request.dimension = from_jstring(env, dimension);
    request.x = x;
    request.y = y;
    request.z = z;
    request.player = player == JNI_TRUE;
    request.facts = facts;
    return pack(ronova::root::decide(request));
}

RONOVA_JNI_EXPORT jlong JNICALL
Java_dev_ronova_coremod_nativebridge_NativeRootBridge_capabilities(
    JNIEnv*, jclass, jlong uuid_most, jlong uuid_least
) {
    return ronova::root::capabilities(uuid_most, uuid_least);
}

RONOVA_JNI_EXPORT jlongArray JNICALL
Java_dev_ronova_coremod_nativebridge_NativeRootBridge_state(JNIEnv* env, jclass) {
    const ronova::root::Status status = ronova::root::status();
    jlong values[STATUS_SIZE]{};
    values[0] = (status.ready ? 1 : 0) | (status.projection_current ? 2 : 0);
    values[1] = status.generation;
    values[2] = status.sequence;
    values[3] = static_cast<jlong>(status.fingerprint);
    values[4] = status.policy_generation;
    values[5] = status.admission_generation;
    values[6] = status.reconciliation_clock;
    values[7] = status.publications;
    values[8] = status.stale_rejections;
    values[9] = status.conflicts;
    values[10] = status.drift_detections;
    values[11] = status.native_decisions;
    values[12] = status.authority_count;
    values[13] = status.judgment_count;

    jlongArray result = env->NewLongArray(STATUS_SIZE);
    if (result != nullptr) env->SetLongArrayRegion(result, 0, STATUS_SIZE, values);
    return result;
}

RONOVA_JNI_EXPORT void JNICALL
Java_dev_ronova_coremod_nativebridge_NativeRootBridge_acknowledge0(
    JNIEnv*, jclass, jlong generation, jlong sequence, jlong fingerprint
) {
    ronova::root::acknowledge(
        generation, sequence, static_cast<std::uint64_t>(fingerprint)
    );
}

RONOVA_JNI_EXPORT void JNICALL
Java_dev_ronova_coremod_nativebridge_NativeRootBridge_reset0(JNIEnv*, jclass) {
    ronova::root::reset();
}

RONOVA_JNI_EXPORT jint JNICALL
Java_dev_ronova_coremod_nativebridge_NativeRootBridge_bootstrap(JNIEnv*, jclass) {
    return ronova::root::capability_mask();
}

RONOVA_JNI_EXPORT void JNICALL
Java_dev_ronova_coremod_nativebridge_NativeRootBridge_startWatchdog0(
    JNIEnv*, jclass, jint interval
) {
    const LONG clamped = std::max<LONG>(15000, std::min<LONG>(60000, interval));
    InterlockedExchange(&g_watchdog_interval, clamped);
    if (InterlockedCompareExchange(&g_watchdog_running, 1, 0) == 0) {
        HANDLE thread = CreateThread(nullptr, 0, &root_watchdog_main, nullptr, 0, nullptr);
        if (thread != nullptr) CloseHandle(thread);
        else InterlockedExchange(&g_watchdog_running, 0);
    }
}

RONOVA_JNI_EXPORT void JNICALL
Java_dev_ronova_coremod_nativebridge_NativeRootBridge_shutdown(JNIEnv*, jclass) {
    InterlockedExchange(&g_watchdog_running, 0);
    ronova::root::reset();
}
