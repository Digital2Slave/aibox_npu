package cn.epbox.cv_npu;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class ImageResizeAndConvert {

    public static byte[] resizeAndConvert(byte[] byteArray, int targetWidth, int targetHeight) {
        // 1. 将 byte[] 转换为 Bitmap
        Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);

        // 2. Resize Bitmap
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);

        // 3. 将 ARGB_8888 转换为 RGB_888
        Bitmap rgbBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(rgbBitmap);
        canvas.drawBitmap(resizedBitmap, 0, 0, null);

        // 4. 将 Bitmap 转换回 byte[]
        int size = rgbBitmap.getRowBytes() * rgbBitmap.getHeight();
        ByteBuffer buffer = ByteBuffer.allocate(size);
        rgbBitmap.copyPixelsToBuffer(buffer);
        return buffer.array();
    }
    public static byte[] resizeAndConvert(Bitmap bitmap, int targetWidth, int targetHeight) {
        // 1. Resize Bitmap
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);

        // 2. 创建 ARGB_8888 Bitmap
        Bitmap argbBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(argbBitmap);
        canvas.drawBitmap(resizedBitmap, 0, 0, null);

        // 3. 提取 RGB 通道的 byte[]
        int size = argbBitmap.getRowBytes() * argbBitmap.getHeight()*4;
        ByteBuffer buffer = ByteBuffer.allocate(size);
        argbBitmap.copyPixelsToBuffer(buffer);
        return buffer.array();
    }

    public static byte[] resizeAndConvertToRGB(Bitmap bitmap, int targetWidth, int targetHeight) {
        // 1. Resize Bitmap
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);

        // 2. 创建 RGB_888 Bitmap
        Bitmap rgbBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(rgbBitmap);
        canvas.drawBitmap(resizedBitmap, 0, 0, null);

        // 3. 提取 RGB 通道的 byte[]
        int size = rgbBitmap.getRowBytes() * rgbBitmap.getHeight();
        ByteBuffer buffer = ByteBuffer.allocate(size);
        rgbBitmap.copyPixelsToBuffer(buffer);
        return buffer.array();
    }


    //!<
    public static byte[] resizeAndConvertToRGB(byte[] imageData, int newWidth, int newHeight) {
        // 1. 将byte[]转换为Bitmap
        Bitmap sourceBitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);

        // 2. Resize图片
        Matrix matrix = new Matrix();
        matrix.postScale((float) newWidth / sourceBitmap.getWidth(), (float) newHeight / sourceBitmap.getHeight());
        Bitmap resizedBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight(), matrix, true);

        // 3. 将Bitmap转换为ARGB_8888的ByteBuffer
        ByteBuffer buffer = ByteBuffer.allocate(resizedBitmap.getWidth() * resizedBitmap.getHeight() * 4);
        resizedBitmap.copyPixelsToBuffer(buffer);

        // 4. 从ByteBuffer中提取RGB值
        int pixelCount = resizedBitmap.getWidth() * resizedBitmap.getHeight();
        byte[] rgbBytes = new byte[pixelCount * 3];
        int index = 0;
        for (int i = 0; i < pixelCount; i++) {
            int argb = buffer.getInt();
            int red = Color.red(argb);
            int green = Color.green(argb);
            int blue = Color.blue(argb);

            rgbBytes[index++] = (byte) red;
            rgbBytes[index++] = (byte) green;
            rgbBytes[index++] = (byte) blue;
        }

        // 5. 清理资源
        sourceBitmap.recycle();
        resizedBitmap.recycle();

        return rgbBytes;
    }
}