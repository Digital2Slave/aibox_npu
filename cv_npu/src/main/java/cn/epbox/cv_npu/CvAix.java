package cn.epbox.cv_npu;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Vector;


public class CvAix {
    static {
        System.loadLibrary("rknn4j");
    }

    private final static String TAG = "CvAix";

    // For input and output
    private String mAiModelFolder = "";
    private String mRknnModelName = "";
    private String mRknnLabelName = "";
    private String mSaveImageName = "";
    public EpboxCV mEpboxCV = new EpboxCV();

    // For inference
    private Vector<String> mlabels = new Vector<String>();


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    //public native static int[] grayProc(int[] pixels, int w, int h);
    public native static EpboxCV AiboxPhoneDamageDetect(Bitmap bmp, String ai_model_folder, String img_name, String rknn_model_name, String rknn_label_name);

    public CvAix (String ai_model_folder, String rknn_model_name, String rknn_label_name, String img_name) {
        mAiModelFolder = ai_model_folder;
        mRknnModelName = rknn_model_name;
        mRknnLabelName = rknn_label_name;
        mSaveImageName = img_name;
    }

    public EpboxCV DoAiboxPhoneDamageDetect(Bitmap bmp_in) {
        //1. 将assets内的模型文件同步到ai_model_folder文件夹内
        checkModelFilesReady(mAiModelFolder, mRknnModelName, mRknnLabelName);
        Log.d(TAG, "DoAiboxPhoneDamageDetect-checkModelFilesReady: "+" save model files from " + mAiModelFolder + " done!");

        //2. 高分辨率图片下采样
        //Log.d(TAG, "DoAiboxPhoneDamageDetect-resizeBitmap: bmp_in width " + bmp_in.getWidth() + " height " + bmp_in.getHeight());
        //Bitmap bmp = downsampleBitmap(bmp_in, 0.5f);
        //Log.d(TAG, "DoAiboxPhoneDamageDetect-resizeBitmap: bmp width " + bmp.getWidth() + " height " + bmp.getHeight());

        //3. 加载损耗检测模型对应的标签
        String rknn_label_path = mAiModelFolder + mRknnLabelName;
        try {
            loadLabelName(rknn_label_path, mlabels);
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (int i = 0; i < mlabels.size(); i++) {
            Log.d(TAG, "DoAiboxPhoneDamageDetect: " + mlabels.get(i));
        }
        Log.d(TAG, "DoAiboxPhoneDamageDetect: "+"load rknn model label " + rknn_label_path + " done!");

        //4. 手机图片进行损耗检测
        mEpboxCV = doRknnCore(bmp_in);
        return mEpboxCV;
    }

    //!< 手机正面图片初筛损耗检测。
    private EpboxCV doRknnCore(Bitmap mbmp) {
        // bmp 转为 byte[]
        Bitmap bmp = mbmp.copy(mbmp.getConfig(), true);
        int bmpChannelNum = getBitmapChannelCount(bmp);
        Log.d(TAG, "doRknnCore: bmp channel num " + bmpChannelNum);

        // run rknn model
        mEpboxCV = AiboxPhoneDamageDetect(bmp, mAiModelFolder, mSaveImageName, mRknnModelName, mRknnLabelName);

        return mEpboxCV;
    }

    private void checkModelFilesReady(String ai_model_folder, String rknn_model_name, String rknn_label_name) {
        // !< Check ai_model_folder exists.
        File folder = new File(ai_model_folder);
        if (!folder.exists()) {
            folder.mkdir();
        }
        // !< Model files.
        String[] model_files = {
                rknn_label_name,
                rknn_model_name
        };

        // Loop model_files
        for (String model_file: model_files) {
            // Set variables
            InputStream in = null;
            OutputStream out = null;

            try {
                // input
                String input_file_path = "/assets/" + model_file;
                Log.d(TAG, "getAbsolutePathOfAssets: input_path->" + input_file_path);
                in = getClass().getResourceAsStream(input_file_path);

                // output
                File out_file = new File(ai_model_folder, model_file);
                String output_file_path = out_file.getAbsolutePath();
                Log.d(TAG, "getAbsolutePathOfAssets: output_path->" + output_file_path);

                // Copy input to output if need.
                if (!out_file.exists()) {
                    out = new FileOutputStream(out_file);
                    // https://blog.csdn.net/qq_46574738/article/details/123581756
                    byte[] buf = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(buf)) > 0) {
                        out.write(buf, 0, bytesRead);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // close in and out
                try {
                    if(in != null){
                        in.close();
                    }
                    if(out != null){
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void loadLabelName(final String locationFilename, Vector<String> labels) throws IOException {
        InputStream is = new FileInputStream(locationFilename);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = reader.readLine()) != null) {
            labels.add(line);
        }
        reader.close();
    }


    private static Bitmap downsampleBitmap(Bitmap originalBitmap, float scale_) {
        // 获取原始宽度和高度
        int originalWidth = originalBitmap.getWidth();
        int originalHeight = originalBitmap.getHeight();

        // 计算目标宽度和高度
        int targetWidth = (int) (originalWidth * scale_);
        int targetHeight = (int) (originalHeight * scale_);

        // 使用 createScaledBitmap 方法调整尺寸
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true);

        return resizedBitmap;
    }


    private int getBitmapChannelCount(Bitmap bitmap) {
        Bitmap.Config config = bitmap.getConfig();
        if (config == Bitmap.Config.ARGB_8888) {
            return 4;
        } else if (config == Bitmap.Config.RGB_565) {
            return 3;
        } else if (config == Bitmap.Config.ALPHA_8) {
            return 1;
        } else {
            // 如果是未知配置，这里可以抛出异常或者返回默认值
            throw new IllegalArgumentException("Unsupported Bitmap.Config: " + config);
        }
    }

    private static float computeRectJoinUnion(RectF box1, RectF box2) {
        float x1 = box1.left, y1 = box1.top, w1 = box1.right - box1.left, h1 = box1.bottom - box1.top;
        float x2 = box2.left, y2 = box2.top, w2 = box2.right - box2.left, h2 = box2.bottom - box2.top;

        // 检查是否没有重叠
        if (x1 > x2 + w2 || y1 > y2 + h2 || x2 > x1 + w1 || y2 > y1 + h1) {
            return 0.0f;
        }

        // 计算交集矩形的宽度和高度
        float joinRectW = Math.min(x1 + w1, x2 + w2) - Math.max(x1, x2);
        float joinRectH = Math.min(y1 + h1, y2 + h2) - Math.max(y1, y2);

        // 计算交集面积
        float joinUnionArea = joinRectW * joinRectH;

        // 计算两个矩形的面积
        float box1Area = w1 * h1;
        float box2Area = w2 * h2;

        // 返回交并比 (IoU)
        return 1.0f * joinUnionArea / (box1Area + box2Area - joinUnionArea);
    }

    public void saveBitmapAsJpeg(Bitmap bitmap, File file) throws IOException {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            // 将 Bitmap 压缩为 JPEG 格式，并将其写入输出流
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
}
