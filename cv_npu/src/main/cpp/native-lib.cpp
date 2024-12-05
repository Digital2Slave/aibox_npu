#include <jni.h>
#include <string>
#include <android/log.h>
#include <android/bitmap.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include "global.h"
#include "yolo11.h"


// https://www.thecodingnotebook.com/2020/04/image-processing-with-opencv-in-android.html
// https://github.com/opencv/opencv/blob/master/modules/java/generator/src/cpp/utils.cpp
// 将bitmap转换为Mat
void bitmapToMat(JNIEnv *env, jobject bitmap, Mat& dst, jboolean needUnPremultiplyAlpha) {
    AndroidBitmapInfo  info;
    void*              pixels = 0;

    try {
        assert( AndroidBitmap_getInfo(env, bitmap, &info) >= 0 );
        assert( info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 ||
                info.format == ANDROID_BITMAP_FORMAT_RGB_565 );
        LOGI("bitmapToMat bitmap info.format:%d", info.format);
        assert( AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0 );
        assert( pixels );
        dst.create(info.height, info.width, CV_8UC4);
        if( info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 )
        {
            Mat tmp(info.height, info.width, CV_8UC4, pixels);
            if(needUnPremultiplyAlpha) cvtColor(tmp, dst, COLOR_mRGBA2RGBA);
            else tmp.copyTo(dst);
        } else {
            // info.format == ANDROID_BITMAP_FORMAT_RGB_565
            Mat tmp(info.height, info.width, CV_8UC2, pixels);
            cvtColor(tmp, dst, COLOR_BGR5652RGBA);
        }
        AndroidBitmap_unlockPixels(env, bitmap);
        return;
    } catch(const cv::Exception& e) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, e.what());
        return;
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Unknown exception in JNI code {nBitmapToMat}");
        return;
    }
}

// https://github.com/Vaccae/AndroidOpenCVTesserartOCR
//将Mat转换为bitmap
jobject mat2bitmap(JNIEnv *env, cv::Mat &src, bool needPremultiplyAlpha, jobject bitmap_config) {

    jclass java_bitmap_class = (jclass) env->FindClass("android/graphics/Bitmap");

    jmethodID mid = env->GetStaticMethodID(
            java_bitmap_class,
            "createBitmap",
            "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;"
    );

    jobject bitmap = env->CallStaticObjectMethod(
            java_bitmap_class, mid, src.size().width, src.size().height, bitmap_config);

    AndroidBitmapInfo info;
    void *pixels = 0;

    try {
        assert(AndroidBitmap_getInfo(env, bitmap, &info) >= 0);
        assert(src.type() == CV_8UC1 || src.type() == CV_8UC3 || src.type() == CV_8UC4);
        assert(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0);
        assert(pixels);

        if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
            cv::Mat tmp(info.height, info.width, CV_8UC4, pixels);
            if (src.type() == CV_8UC1) {
                cvtColor(src, tmp, cv::COLOR_GRAY2RGBA);
            } else if (src.type() == CV_8UC3) {
                cvtColor(src, tmp, cv::COLOR_RGB2BGRA);
            } else if (src.type() == CV_8UC4) {
                if (needPremultiplyAlpha) {
                    cvtColor(src, tmp, cv::COLOR_RGBA2mRGBA);
                } else {
                    src.copyTo(tmp);
                }
            }
        } else {
            // info.format == ANDROID_BITMAP_FORMAT_RGB_565
            cv::Mat tmp(info.height, info.width, CV_8UC2, pixels);
            if (src.type() == CV_8UC1) {
                cvtColor(src, tmp, cv::COLOR_GRAY2BGR565);
            } else if (src.type() == CV_8UC3) {
                cvtColor(src, tmp, cv::COLOR_RGB2BGR565);
            } else if (src.type() == CV_8UC4) {
                cvtColor(src, tmp, cv::COLOR_RGBA2BGR565);
            }
        }
        AndroidBitmap_unlockPixels(env, bitmap);
        return bitmap;
    } catch (cv::Exception e) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("org/opencv/core/CvException");
        if (!je) je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, e.what());
        return bitmap;
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Unknown exception in JNI code {nMatToBitmap}");
        return bitmap;
    }
}


extern "C"
{
    JNIEXPORT jintArray JNICALL Java_cn_epbox_cv_1npu_CvAix_grayProc(
            JNIEnv *env,
            jobject instance,
            jintArray buf,
            jint w,
            jint h) {

        jboolean ptfalse = false;
        jint *cbuf = env->GetIntArrayElements(buf, &ptfalse);
        if (cbuf == NULL) {
            return 0;
        }

        cv::Mat imgData(h, w, CV_8UC4, (unsigned char *) cbuf);

        uchar *ptr = imgData.ptr(0);
        for (int i = 0; i < w * h; i++) {
            uchar grayScale = (uchar) (ptr[4 * i + 2] * 0.299 + ptr[4 * i + 1] * 0.587 +
                                       ptr[4 * i + 0] * 0.114);
            ptr[4 * i + 1] = grayScale;
            ptr[4 * i + 2] = grayScale;
            ptr[4 * i + 0] = grayScale;
        }

        int size = w * h;
        jintArray result = env->NewIntArray(size);
        env->SetIntArrayRegion(result, 0, size, cbuf);


        env->ReleaseIntArrayElements(buf, cbuf, 0);
        return result;
    }


    // https://www.thecodingnotebook.com/2020/04/image-processing-with-opencv-in-android.html
    // https://blog.csdn.net/Vaccae/article/details/111595943
    JNIEXPORT jobject JNICALL Java_cn_epbox_cv_1npu_CvAix_AiboxPhoneDamageDetect(
            JNIEnv *env,
            jobject p_this,
            jobject bmp,
            jstring ai_model_folder,
            jstring image_name_,
            jstring rknn_model_name_,
            jstring rknn_label_name_) {
        // 安卓assets文件夹路径转为string类型
        const char *ai_model_ = env->GetStringUTFChars(ai_model_folder, JNI_FALSE);
        std::string ai_model_path = ai_model_;

        // 图像名称转为string类型
        const char *img_name = env->GetStringUTFChars(image_name_, JNI_FALSE);
        std::string image_name = img_name;
        std::string image_crop_name = img_name;
        // 保存裁剪图片名字
        size_t dot_pos = image_name.rfind('.');
        if (dot_pos != std::string::npos) {
            // 插入 "_crop" 到点之前
            image_crop_name = image_name.substr(0, dot_pos) + "_detect" + image_name.substr(dot_pos);
        }
        std::string image_crop_path = ai_model_path + image_crop_name;

        // rknn模型名称和标签名称转为string类型
        const char *model_name = env->GetStringUTFChars(rknn_model_name_, JNI_FALSE);
        std::string rknn_model_name = model_name;
        const char *label_name = env->GetStringUTFChars(rknn_label_name_, JNI_FALSE);
        std::string rknn_label_name = label_name;
        std::string rknn_model_path = ai_model_path + rknn_model_name;
        std::string rknn_label_path = ai_model_path + rknn_label_name;

        // 获取原图片的参数
        jclass java_bitmap_class = (jclass) env->FindClass("android/graphics/Bitmap");
        jmethodID mid = env->GetMethodID(java_bitmap_class, "getConfig","()Landroid/graphics/Bitmap$Config;");
        jobject bitmap_config = env->CallObjectMethod(bmp, mid);

        // 生成源图像
        cv::Mat src;
        bitmapToMat(env, bmp, src, JNI_FALSE);
        LOGI("AiboxPhoneDamageDetect src.channels()=%d", src.channels());
        cv::cvtColor(src, src, cv::COLOR_RGBA2BGR);
        LOGI("AiboxPhoneDamageDetect src.channels()=%d", src.channels());

        LOGI("AiboxPhoneDamageDetect ai_model_path=%s", ai_model_path.c_str());
        LOGI("AiboxPhoneDamageDetect image_name=%s", image_name.c_str());
        LOGI("AiboxPhoneDamageDetect image_crop_path=%s", image_crop_path.c_str());
        LOGI("AiboxPhoneDamageDetect rknn_model_path=%s", rknn_model_path.c_str());
        LOGI("AiboxPhoneDamageDetect rknn_label_path=%s", rknn_label_path.c_str());

        // RKNN model
        DetectResult dr = rknnRun(src, rknn_model_path, rknn_label_path, image_crop_path);
        cv::Mat phone = dr.phone;
        int status = dr.status;
        std::string detectRes = dr.detectRes;
        std::string msg = dr.msg;
        LOGI("phone.channels=%d", phone.channels());
        LOGI("phone.status=%d", status);
        LOGI("phone.detect_res=%s", detectRes.c_str());
        LOGI("phone.msg=%s", msg.c_str());

        // 将phone转换为图片
        jobject phone_bitmap = mat2bitmap(env, phone, false, bitmap_config);

        // 1. 获取EpboxCV类
        jclass jcls = env->FindClass("cn/epbox/cv_npu/EpboxCV");
        if (jcls == NULL) {
            return 0;
        }

        // 2. 定义类里面的属性
        jfieldID jcls_phone = env->GetFieldID(jcls, "phone", "Landroid/graphics/Bitmap;");
        jfieldID jcls_status = env->GetFieldID(jcls, "status", "I");
        jfieldID jcls_detect_res  = env->GetFieldID(jcls, "detectRes", "Ljava/lang/String;");
        jfieldID jcls_msg = env->GetFieldID(jcls, "msg", "Ljava/lang/String;");

        // !< 首先实例化类
        jobject jobj = env->AllocObject(jcls);

        // !< 然后对类中的各属性赋值
        // 检测结果图片对象
        env->SetObjectField(jobj, jcls_phone, phone_bitmap);
        // 状态
        env->SetIntField(jobj, jcls_status, status);
        // 记录损耗检测信息
        env->SetObjectField(jobj, jcls_detect_res, env->NewStringUTF(detectRes.c_str()));
        // 记录日志信息
        env->SetObjectField(jobj, jcls_msg, env->NewStringUTF(msg.c_str()));

        return jobj;
    }
}