package cn.epbox.aibox_npu;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import cn.epbox.cv_npu.CvAix;
import cn.epbox.cv_npu.EpboxCV;


public class MainActivity extends AppCompatActivity {
    private final static String TAG = "EPBOX_DEMO";
    private final static String dr_default_txt  = "ai model result will appear here...";
    private final static String rknn_model_name = "yolo11n.rknn";
    private final static String rknn_label_name = "coco_80_labels_list.txt";
    public static final int TAKE_PHOTO = 1;

    public static final int CHOOSE_PHOTO = 2;

    private ImageView picture;

    private Uri imageUri;
    private Bitmap m_input;
    private Bitmap m_bitmap_default;

    // output
    Button drButton = null;
    TextView drText = null;
    Bitmap m_phone = null;
    String m_result = "";

    private static String m_storage_state = Environment.getExternalStorageState();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button takePhoto = (Button) findViewById(R.id.take_photo);
        Button chooseFromAlbum = (Button) findViewById(R.id.choose_from_album);
        drButton = (Button) findViewById(R.id.dr_button);
        drText = (TextView) findViewById(R.id.dr_text);
        picture = (ImageView) findViewById(R.id.picture);

        // 设置默认展示图片
        m_bitmap_default = BitmapFactory.decodeResource(getResources(), R.drawable.head);
        picture.setImageBitmap(m_bitmap_default);

        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 创建File对象，用于存储拍照后的图片
                File outputImage = new File(getExternalCacheDir(), "20240709 _-2.jpg");
                try {
                    if (outputImage.exists()) {
                        outputImage.delete();
                    }
                    outputImage.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (Build.VERSION.SDK_INT < 24) {
                    imageUri = Uri.fromFile(outputImage);
                } else {
                    imageUri = FileProvider.getUriForFile(MainActivity.this, "cn.epbox.aibox_npu.fileprovider", outputImage);
                }
                // 启动相机程序
                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(intent, TAKE_PHOTO);
                drText.setText(dr_default_txt);
            }
        });
        chooseFromAlbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{ Manifest.permission. WRITE_EXTERNAL_STORAGE }, 1);
                } else {
                    openAlbum();
                }
                drText.setText(dr_default_txt);
            }
        });

        drButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Argument 1: input bitmap
                Bitmap input = m_input;
                String inputImagePath = saveImage(input, "epbox/cv_image/", "in_");
                Log.d(TAG, "onClick: " + inputImagePath);

                //Argument 2: epbox/model_files/
                String ai_model_folder = getStoragePath() + "/model_files/";
                Log.d(TAG, "onClick: " + ai_model_folder);

                //Argument 3: image name
                String img_name = "202412_-2.jpg";

                //!< 开始计时
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
                String date_start = sdf.format(new java.util.Date());

                //!< native-cpp
                CvAix cvAix = new CvAix(ai_model_folder, rknn_model_name, rknn_label_name, img_name);

                EpboxCV dr = cvAix.DoAiboxPhoneDamageDetect(input);

                //!< 结束计时
                String date_end = sdf.format(new java.util.Date());
                String time_expend = getTimeExpend(date_start, date_end);

                //!< 展示输出内容
                m_phone = dr.phone;
                m_result = "Aibox_npu调用cv_npu安卓库方法耗时(毫秒): " + time_expend +
                        "\nStatus:" + dr.status +
                        "\nDetectRes:" + dr.detectRes +
                        "\nMsg:" + dr.msg;

                //!< 展示检测结果并保存检测结果图片
                drText.setText(m_result);
                String saveImagePath = saveImage(m_phone, "epbox/cv_image/", "out_");
                displayImage(saveImagePath);
            }
        });

    }

    private static Bitmap preCropInputImage(Bitmap input) {
        Bitmap input_crop = input.copy(input.getConfig(), true);

        return input_crop;
    }


    private static String getStoragePath() {
        String res = "";
        if (m_storage_state.equals(Environment.MEDIA_MOUNTED)) {
            File folder = new File(Environment.getExternalStorageDirectory(), "epbox/");
            if (!folder.exists()) {
                folder.mkdir();
            }
            res = folder.getAbsolutePath();
        }
        return res;
    }


    private static String saveImage(Bitmap bitmap, String sub_folder, String pre_fix) {
        File imageFilepath = null;
        // Check for SD card
        if (m_storage_state.equals(Environment.MEDIA_MOUNTED)) {
            File folder = new File(Environment.getExternalStorageDirectory(), sub_folder);
            if (!folder.exists()) {
                folder.mkdir();
            }

            BufferedOutputStream outStream = null;
            //String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
            String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmssSSS").format(new Date());
            // Getting filepath
            imageFilepath = new File(folder.getPath() + File.separator + pre_fix + timestamp + ".jpg");
            //imageFilepath = new File(folder.getPath() + File.separator + pre_fix + ".PNG");
            try {
                outStream = new BufferedOutputStream(new FileOutputStream(imageFilepath));
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                outStream.flush();
                outStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return imageFilepath.getAbsolutePath();
    }

    private void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent, CHOOSE_PHOTO); // 打开相册
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openAlbum();
                } else {
                    Toast.makeText(this, "You denied the permission", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    try {
                        // 将拍摄的照片显示出来
                        Bitmap bm = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        // 拍照图片进行90度旋转
                        Matrix matrix = new Matrix();
                        int height = bm.getHeight();
                        int width = bm.getWidth();
                        matrix.setRotate(90);
                        Bitmap bitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
                        // 对旋转后图片进行预裁剪
                        Bitmap sizeBitmap = Bitmap.createBitmap(bitmap, 86, 124, bitmap.getWidth() - 170, bitmap.getHeight() - 276);
                        m_input = sizeBitmap;
                        picture.setImageBitmap(m_input);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            case CHOOSE_PHOTO:
                if (resultCode == RESULT_OK) {
                    // 判断手机系统版本号
                    if (Build.VERSION.SDK_INT >= 19) {
                        // 4.4及以上系统使用这个方法处理图片
                        handleImageOnKitKat(data);
                    } else {
                        // 4.4以下系统使用这个方法处理图片
                        handleImageBeforeKitKat(data);
                    }
                }
                break;
            default:
                break;
        }
    }

    @TargetApi(19)
    private void handleImageOnKitKat(Intent data) {
        String imagePath = null;
        Uri uri = data.getData();
        Log.d("TAG", "handleImageOnKitKat: uri is " + uri);
        if (DocumentsContract.isDocumentUri(this, uri)) {
            // 如果是document类型的Uri，则通过document id处理
            String docId = DocumentsContract.getDocumentId(uri);
            if("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1]; // 解析出数字格式的id
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // 如果是content类型的Uri，则使用普通方式处理
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            // 如果是file类型的Uri，直接获取图片路径即可
            imagePath = uri.getPath();
        }
        displayImage(imagePath); // 根据图片路径显示图片
    }

    private void handleImageBeforeKitKat(Intent data) {
        Uri uri = data.getData();
        String imagePath = getImagePath(uri, null);
        displayImage(imagePath);
    }

    private String getImagePath(Uri uri, String selection) {
        String path = null;
        // 通过Uri和selection来获取真实的图片路径
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    private void displayImage(String imagePath) {
        if (imagePath != null) {
            m_input = BitmapFactory.decodeFile(imagePath);
            picture.setImageBitmap(m_input);
        } else {
            Toast.makeText(this, "failed to get image", Toast.LENGTH_SHORT).show();
        }
    }

    // https://www.cnblogs.com/ymtianyu/p/5623095.html
    private long getTimeMillis(String strTime) {
        long returnMillis = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        Date d = null;
        try {
            d = sdf.parse(strTime);
            returnMillis = d.getTime();
        } catch (ParseException e) {
            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
        }
        return returnMillis;
    }

    // 传入开始时间和结束时间字符串来计算消耗时长
    private String getTimeExpend(String startTime, String endTime){
        //传入字串类型 2016/06/28 08:30
        long longStart = getTimeMillis(startTime); //获取开始时间毫秒数
        long longEnd = getTimeMillis(endTime);  //获取结束时间毫秒数
        long longExpend = longEnd - longStart;  //获取时间差

        long longHours = longExpend / (60 * 60 * 1000); //根据时间差来计算小时数
        long longMinutes = (longExpend - longHours * (60 * 60 * 1000)) / (60 * 1000);   //根据时间差来计算分钟数

        //return longHours + ":" + longMinutes;
        return longExpend+"ms";
    }

    // 传入结束时间和消耗时长来计算开始时间
    private String getTimeString(String endTime, String expendTime){
        //传入字串类型 end:2016/06/28 08:30 expend: 03:25
        long longEnd = getTimeMillis(endTime);
        String[] expendTimes = expendTime.split(":");   //截取出小时数和分钟数
        long longExpend = Long.parseLong(expendTimes[0]) * 60 * 60 * 1000 + Long.parseLong(expendTimes[1]) * 60 * 1000;
        SimpleDateFormat sdfTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        return sdfTime.format(new Date(longEnd - longExpend));
    }
}