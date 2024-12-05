//
// Created by tianzx on 22-10-26.
//

#ifndef CV_AIX_GLOBAL_H_H
#define CV_AIX_GLOBAL_H_H
#include <iostream>
#include <vector>
#include <string>
#include <sys/stat.h>

#include "opencv2/opencv.hpp"
#include "opencv2/core/base.hpp"
#include "opencv2/core/core_c.h"
#include "opencv2/core/types_c.h"
#include "opencv2/imgproc/imgproc_c.h"
#include "opencv2/highgui/highgui.hpp"

#define LOG_TAG "System.out"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

using namespace std;
using namespace cv;


// Utils
void checkRectValid(cv::Rect &boxRect, const int cols, const int rows);
void drawText2Image(const cv::Mat& drawIm, std::string text, cv::Point pt, int font_face = cv::FONT_HERSHEY_SIMPLEX, double font_scale = 1.0, cv::Scalar color = cv::Scalar(0, 255, 255), int thickness = 2);


#endif //CV_AIX_GLOBAL_H_H
