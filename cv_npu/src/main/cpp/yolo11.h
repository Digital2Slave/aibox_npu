#ifndef _RKNN_DEMO_YOLO11_H_
#define _RKNN_DEMO_YOLO11_H_

#include "rknn/3rdparty/rknpu2/include/rknn_api.h"
#include "rknn/utils/common.h"
#include "postprocess.h"
#include "global.h"


int init_yolo11_model(const char* model_path, rknn_app_context_t* app_ctx);

int release_yolo11_model(rknn_app_context_t* app_ctx);


int inference_yolo11_model(rknn_app_context_t* app_ctx, image_buffer_t* img, object_detect_result_list* od_results);


/**
 * @brief 将 cv::Mat 转换为 image_buffer_t
 *
 * @param mat 输入的 cv::Mat
 * @return image_buffer_t 转换后的 image_buffer_t 对象
 */
image_buffer_t matToImageBuffer(const cv::Mat& mat);

struct DetectResult {
    cv::Mat phone;
    int status;
    std::string detectRes;
    std::string msg;
};

DetectResult rknnRun(cv::Mat& src, std::string model_path_, std::string label_path_, std::string image_path_);

#endif //_RKNN_DEMO_YOLO11_H_