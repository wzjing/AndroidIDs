#include <android/log.h>
#include <jni.h>
#include <cstdlib>
#include <sys/mman.h>
#include <cstdio>
#include <cstring>
#include <cstdlib>
#include <arpa/inet.h>
#include <string>
#include <fstream>
#include <sstream>

#define LOGD(tag, format, ...) __android_log_print(ANDROID_LOG_DEBUG, tag, format, ## __VA_ARGS__)

static const char * TAG = "main.cpp";

#ifdef i386
static void get_cpu_id_by_asm(std::string & cpu_id)
{
    cpu_id.clear();

    unsigned int s1 = 0;
    unsigned int s2 = 0;
    asm volatile
    (
        "movl $0x01, %%eax; \n\t"
        "xorl %%edx, %%edx; \n\t"
        "cpuid; \n\t"
        "movl %%edx, %0; \n\t"
        "movl %%eax, %1; \n\t"
        : "=m"(s1), "=m"(s2)
    );
    if (0 == s1 && 0 == s2)
    {
        return;
    }
    char cpu[32] = { 0 };
    snprintf(cpu, sizeof(cpu), "%08X%08X", htonl(s2), htonl(s1));
    std::string(cpu).swap(cpu_id);
    return;
}
#else
#ifdef __arm__
static void get_cpu_id_by_asm(std::string & cpu_id) {
    unsigned int id = 0;
    asm volatile("mrs c0,0,%0,c0,c0,0":"=r"(id));
    cpu_id.assign(std::to_string(id));
}
#else
#ifdef __aarch64__
static void get_cpu_id_by_asm(std::string & cpu_id) {
    unsigned int id = 0;
//    asm volatile("mrs c0,0,%0,c0,c0,0":"=r"(id));
    asm volatile("mrs r0, cpsr");
    asm volatile("mrs %0, r0":"=r"(id));
    cpu_id.assign(std::to_string(id));
}
#else
static void get_cpu_id_by_asm(std::string & cpu_id) {
    cpu_id.assign("not support");
}
#endif // __aarch64__
#endif // __arm__
#endif // i386

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_androidids_DeviceUtils_getCpuId(JNIEnv *env, jclass clazz) {
    LOGD(TAG, "Java_com_example_androidids_DeviceUtils_getCpuId");
    std::string cpu_id;
    get_cpu_id_by_asm(cpu_id);
    return env->NewStringUTF(cpu_id.c_str());
}
