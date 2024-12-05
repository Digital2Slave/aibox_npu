#include "global.h"


void drawText2Image(const cv::Mat& drawIm, std::string text, cv::Point pt, int font_face, double font_scale, cv::Scalar color, int thickness)
{
    int baseline;
    cv::Size text_size = cv::getTextSize(text, font_face, font_scale, thickness, &baseline);
    cv::Point origin;
    origin.x = pt.x - text_size.width / 2;
    origin.y = pt.y - text_size.height / 2;
    cv::putText(drawIm, text, origin, font_face, font_scale, color, thickness, 8, 0);
}



void checkRectValid(cv::Rect &boxRect, const int cols, const int rows)
{
    // Check X
    if (boxRect.x < 0)
    {
        boxRect.x = 1;
    }
    if ((boxRect.width + boxRect.x) > cols)
    {
        boxRect.width = cols - boxRect.x;
    }
    // Check Y
    if (boxRect.y < 0)
    {
        boxRect.y = 1;
    }
    if ((boxRect.height + boxRect.y) > rows)
    {
        boxRect.height = rows - boxRect.y;
    }
}

