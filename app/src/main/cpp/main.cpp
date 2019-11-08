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
#include <linux/kernel.h>

#define LOGD(tag, format, ...) __android_log_print(ANDROID_LOG_DEBUG, tag, format, ## __VA_ARGS__)

static const char *TAG = "main.cpp";

static void get_cpu_id_by_asm(std::string &cpu_id) {
    cpu_id.clear();
#if defined(i386)
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
#elif defined(__arm__)
    unsigned int id = 0;
    asm volatile("mrc p15,0,%0,c0,c0,0":"=r"(id));
    cpu_id.assign(std::to_string(id));
#elif defined(__aarch64__)
//    asm volatile("mrs r0, c0");
    char *id = (char *) malloc(sizeof(char) * 100);
    LOGD(TAG, "support __KERNEL__");
//    asm volatile("STR R0,%w0":"=r"(id));
//    asm volatile("str r0,[r1]");
    cpu_id.assign(id);
#else
    cpu_id.assign("not support abi");
#endif // ABI case
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_androidids_DeviceUtils_getCpuId(JNIEnv *env, jclass clazz) {
    LOGD(TAG, "Java_com_example_androidids_DeviceUtils_getCpuId");
    std::string cpu_id;
    get_cpu_id_by_asm(cpu_id);
    return env->NewStringUTF(cpu_id.c_str());
}
