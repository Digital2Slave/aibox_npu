package cn.epbox.cv_npu;

import android.graphics.Bitmap;

/**
 * Created by tianzx on 22-10-27.
 */

public class EpboxCV {

    private static final String TAG = "EpboxCV";

    public EpboxCV() {
        phone = null;
        status = 0;
        detectRes = "";
        msg = "";
    }

    // 提取图片区域对象
    public Bitmap phone;
    public int status;
    // 损耗检测结果
    public String detectRes;
    // 记录日志信息
    public String msg;


}
