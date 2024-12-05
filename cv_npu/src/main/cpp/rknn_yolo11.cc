#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <string>

#include "global.h"
#include "yolo11.h"
#include "postprocess.h"
#include "rknn/utils/image_utils.h"


image_buffer_t matToImageBuffer(const cv::Mat& mat) {
    // BGR2RGB
    if (mat.channels() == 3) {
        cv::cvtColor(mat, mat, cv::COLOR_BGR2RGB);
    }

    image_buffer_t buffer;

    // 宽度和高度
    buffer.width = mat.cols;
    buffer.height = mat.rows;

    // 宽度步长和高度步长
    buffer.width_stride = mat.step[1]; // 每像素步长（即通道数）
    buffer.height_stride = mat.step[0]; // 每行步长

    // 数据格式映射
    switch (mat.type()) {
        case CV_8UC3:
            buffer.format = IMAGE_FORMAT_RGB888;
            break;
        case CV_8UC1:
            buffer.format = IMAGE_FORMAT_GRAY8;
            break;
        default:
            throw std::runtime_error("Unsupported cv::Mat type");
    }

    // 数据指针
    buffer.virt_addr = mat.data;

    // 数据大小（总字节数）
    buffer.size = mat.total() * mat.elemSize();

    // 文件描述符（如果无实际需求，可以初始化为 -1）
    buffer.fd = -1;

    return buffer;
}


DetectResult rknnRun(cv::Mat& src, std::string model_path_, std::string label_path_, std::string image_path_)
{
    DetectResult detectResult;
    std::string js_str = "{\"model_path\":" + model_path_ + ","
                         + "\"image_path\":" + image_path_ + ","
                         + "\"label_path\":" + label_path_ + ","
                         + "\"data\":[";
    std::string msg = "everything goes well.";

    int ret;
    rknn_app_context_t rknn_app_ctx;
    memset(&rknn_app_ctx, 0, sizeof(rknn_app_context_t));

    init_post_process(label_path_.c_str());

    ret = init_yolo11_model(model_path_.c_str(), &rknn_app_ctx);
    if (ret != 0)
    {
        msg = "init_yolo11_model fail!";
        //!< 释放资源
        deinit_post_process();
        detectResult.msg = msg;
    }
    else
    {
        // Mat转为image_buffer_t
        cv::Mat im = src.clone();
        image_buffer_t src_image = matToImageBuffer(im);

        // rknn模型推理
        object_detect_result_list od_results;
        ret = inference_yolo11_model(&rknn_app_ctx, &src_image, &od_results);
        if (ret != 0)
        {
            msg = "inference_yolo11_model fail!";
            detectResult.msg = msg;
        }
        else
        {
            // 画框和概率
            char text[256];
            cv::Mat drawImage = src.clone();
            for (int i = 0; i < od_results.count; i++)
            {
                object_detect_result *det_result = &(od_results.results[i]);

                std::string cls_name = coco_cls_to_name(det_result->cls_id);
                cls_name = "\"" + cls_name + "\"";
                std::string box_left = std::to_string(det_result->box.left);
                std::string box_top = std::to_string(det_result->box.top);
                std::string box_right = std::to_string(det_result->box.right);
                std::string box_bottom = std::to_string(det_result->box.bottom);
                std::string prop = std::to_string(det_result->prop);

                std::string detect_res = "{\"label_name\":" + cls_name + ","
                                         + "\"box_left\":" + box_left + ","
                                         + "\"box_top\":" + box_top + ","
                                         + "\"box_right\":" + box_right + ","
                                         + "\"box_bottom\":" + box_bottom + ","
                                         + "\"prop\":" + prop + "}";
                js_str += detect_res;
                if (i != od_results.count - 1)
                {
                    js_str += ",";
                }
                // 画框
                int x1 = det_result->box.left;
                int y1 = det_result->box.top;
                int x2 = det_result->box.right;
                int y2 = det_result->box.bottom;
                cv::Rect rect(x1, y1, x2-x1, y2-y1);
                checkRectValid(rect, drawImage.cols, drawImage.rows);
                cv::Scalar color(0, 255, 0);
                int thickness = 1;
                cv::rectangle(drawImage, rect, color, thickness);
                // 画标签和概率
                sprintf(text, "%s %.1f%%", coco_cls_to_name(det_result->cls_id), det_result->prop * 100);
                std::string label_info = text;
                cv::Point pt(x1, y1);
                int font_face = cv::FONT_HERSHEY_SIMPLEX;
                double font_scale = 0.3;
                cv::Scalar text_color = cv::Scalar(0, 255, 255);
                drawText2Image(drawImage, label_info, pt, font_face, font_scale, text_color, thickness);
            }
            // 返回结果
            js_str += "]}";
            // 保存图片
            //cv::imwrite(image_path_, drawImage);
            detectResult.phone = drawImage;
            detectResult.detectRes = js_str;
        }
        //!< 释放资源
        deinit_post_process();
        // 释放模型
        ret = release_yolo11_model(&rknn_app_ctx);
        if (ret != 0)
        {
            msg = "release_yolo11_model_2 fail!";
        }
        // 释放图片
//        if (src_image.virt_addr != NULL)
//        {
//            free(src_image.virt_addr);
//        }
        detectResult.msg = msg;
    }
    return detectResult;
}
