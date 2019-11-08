package com.example.androidids;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.bun.miitmdid.core.ErrorCode;
import com.bun.miitmdid.core.MdidSdkHelper;

import java.lang.reflect.Method;
import java.util.UUID;

public class DeviceUtils {

    static {
        System.loadLibrary("ids");
        System.loadLibrary("A3AEECD8");
    }

    public static final String TAG = "DeviceUtils";

    /**
     * 获取设备的OAID，此方法将异步返回结果
     *
     * @param context  Android Context
     * @param callback 结果回掉
     * @return 回掉方法的注册状态
     */
    @Nullable
    public static void getOAID(Context context, OaidCallback callback) {
        int status = MdidSdkHelper.InitSdk(context, true, (isSupport, supplier) -> {
            Logger.d(TAG, "getOAID :: callback : " + isSupport);
            if (callback != null) {
                callback.onReceiveOaid(isSupport ? supplier.getOAID() : "");
            }
        });
        switch (status) {
            case ErrorCode.INIT_ERROR_MANUFACTURER_NOSUPPORT:
                Logger.e(TAG, "getOAID :: vendor not support");
                break;
            case ErrorCode.INIT_ERROR_DEVICE_NOSUPPORT:
                Logger.e(TAG, "getOAID :: device not support");
                break;
            case ErrorCode.INIT_ERROR_LOAD_CONFIGFILE:
                Logger.e(TAG, "getOAID :: supplierconfig.json not configure or not right");
                break;
            case ErrorCode.INIT_ERROR_RESULT_DELAY:
                Logger.e(TAG, "getOAID :: result will be returned async");
                break;
            case ErrorCode.INIT_HELPER_CALL_ERROR:
                Logger.e(TAG, "getOAID :: unable to get OAID by reflection");
                break;
        }
    }

    @NonNull
    public static String getAndroidId(Context context) {
        String androidId = "";
        try {
            androidId = Settings.System.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            Logger.e(TAG, "getAndroidId :: unable to get Android_Id");
            e.printStackTrace(System.err);
        }
        return androidId == null ? "" : androidId;
    }

    /**
     * 默认返回第一个卡槽的IMEI
     *
     * @param context Android Context
     * @return 返回第一个卡槽的IMEI，如果没有返回null
     */
    @NonNull
    public static String getIMEI(Context context) {
        return getIMEI(context, 0);
    }

    /**
     * 获取手机对应卡槽的IMEI
     *
     * @param context Android Context
     * @param slotId  卡槽的位置，0表示卡1，1表示卡2
     * @return 返回对应卡槽的IMEI，如果没有返回null
     */
    @NonNull
    public static String getIMEI(Context context, int slotId) {
        String imei;
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null) imei = "";

        if (Build.VERSION.SDK_INT >= 21) {
            // 大于Android 5.0 根据具体版本获取IMEI
            if (Build.VERSION.SDK_INT >= 23 &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                            != PackageManager.PERMISSION_GRANTED) {
                // 无权限，直接返回null
                imei = "";
            } else if (Build.VERSION.SDK_INT >= 29) {
                // >= Android 10 无法获取，返回null
                imei = "";
            } else if (Build.VERSION.SDK_INT >= 26) {
                // >= Android 8.0 调用原生方法获取第一个IMEI
                imei = tm.getImei(slotId);
            } else {
                // < Android 8.0
                if (Build.VERSION.SDK_INT >= 23 && tm.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
                    // 手机网络制式为GSM
                    imei = tm.getDeviceId(slotId);
                } else {
                    // 手机网络制式非GSM，通过反射获取(反射需要Android 5.0)
                    try {
                        Method rGetImei = tm.getClass().getMethod("getImei", int.class);
                        imei = (String) rGetImei.invoke(tm, slotId);
                    } catch (Exception e) {
                        Logger.d(TAG, "getIMEICompat :: unable to get IMEI using Reflection");
                        e.printStackTrace(System.err);
                        imei = "";
                    }
                }
            }
        } else {
            // < Android 5.0
            if (slotId == 0) {
                // 只能通过此方法获取
                imei = tm.getDeviceId();
            } else {
                // 低版本无有效的方法获取多卡槽IMEI
                imei = "";
            }
        }

        return imei == null ? "" : imei;
    }

    /**
     * 获取手机的MEID
     *
     * @param context Android Context
     * @param slotId  卡槽的位置，目前仅能够测试卡槽1的MEID（绝大多数手机都最多有一个MEID）
     * @return 返回MEID，如果未能获取，则返回null
     */
    @NonNull
    public static String getMEID(Context context, int slotId) {
        String meid = null;
        // 6.0+ 无权限直接返回null
        if (Build.VERSION.SDK_INT >= 23 &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                        != PackageManager.PERMISSION_GRANTED) {
            // 无权限，直接返回空
            meid = "";
        }

        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null) meid = "";

        try {
            if (Build.VERSION.SDK_INT >= 29) {
                meid = null;
            } else if (Build.VERSION.SDK_INT >= 26) {
                meid = tm.getMeid(slotId);
            } else if (Build.VERSION.SDK_INT >= 21) {
                if (tm.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
                    Logger.d(TAG, "phone is CDMA");
                    if (Build.VERSION.SDK_INT >= 23) {
                        meid = tm.getDeviceId(slotId);
                    } else {
                        meid = tm.getDeviceId();
                    }
                } else {
                    Logger.d(TAG, "phone is GSM");
                    getMeidFromReflection();
                }
            } else {
                // 低版本无法获取
                meid = getMeidFromReflection();
            }

        } catch (Exception e) {
            Logger.d(TAG, "getMEID :: exception while get meid");
            e.printStackTrace(System.err);
        }

        return meid == null ? "" : meid;
    }

    /**
     * 通过反射的方式去获取MEID，此方法可能无法在某些手机上获取到反射方法，建议用作备用方法
     *
     * @return 返回MEID，如果未能获取，则返回null
     */
    @Nullable
    public static String getMeidFromReflection() {
        Logger.d(TAG, "call reflection method");
        try {
            Class clazz = Class.forName("android.os.SystemProperties");
            Method method = clazz.getMethod("get", String.class, String.class);
            return (String) method.invoke(null, "ril.cdma.meid", "");
        } catch (Exception e) {
            Logger.d(TAG, "unable to meid from reflection");
            e.printStackTrace(System.err);
        }

        return null;
    }

    public interface OaidCallback {
        void onReceiveOaid(String oaid);
    }

    @NonNull
    public static String getUniqueId() {
        Logger.d(TAG, "getUniqueId()");
        try {
//            UUID.fromString(Build.BOARD + Build.DISPLAY + Build.BRAND + Build.HOST + Build.HARDWARE);
//            return "null";
            return getCpuId();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "empty";
    }

    public static native String getCpuId();
}
