package com.devs.sketchimagedemo;

import android.content.res.Resources;
import android.graphics.Matrix;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.google.android.material.tabs.TabLayout;
import androidx.percentlayout.widget.PercentFrameLayout;
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
//import android.support.annotation.NonNull;
import androidx.annotation.NonNull;
import 	androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.widget.Toast;



import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import com.devs.imagetosketch.R;
import com.devs.sketchimage.SketchImage;

public class MainActivity extends AppCompatActivity  {

    public static final int TAKE_PHOTO = 1;
    public static final int CHOOSE_PHOTO = 2;
    private ImageView picture;
    private Uri imageUri;
    private static final String TAG = MainActivity.class.getSimpleName();
    private ImageView target;
    private Bitmap bmOriginal;
    private Bitmap bitmap;
    private SketchImage sketchImage;
    private static int MAX_PROGRESS = 100;
    private int effectType = SketchImage.ORIGINAL_TO_GRAY;


    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.main,menu);
        return true;
    }
//    创建关于窗口
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.about:
                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                dialog.setTitle("关于我们");
                dialog.setMessage("数字图像处理三级项目\n" +
                        "小组成员：赵鑫阳 高雨蒙 栾阔 李宇航\n" +
                        "指导教师：赵彦涛\n" +
                        "技术支持：h821021@126.com");
                dialog.setCancelable(true);
                dialog.show();
                break;
                default:
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        Button takePhoto = (Button) findViewById(R.id.take_photo);
        Button chooseFromAlbum = (Button) findViewById(R.id.choose_from_album);
        picture = (ImageView) findViewById(R.id.picture);
        target = (ImageView) findViewById(R.id.iv_target);

        // 拍照
        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 创建File对象，用于存储拍照后的图片
                File outputImage = new File(getExternalCacheDir(), "output_image.jpg");
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
                    imageUri = FileProvider.getUriForFile(MainActivity.this, "com.mydomain.fileprovider", outputImage);
                }
                // 启动相机程序
                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(intent, TAKE_PHOTO);
            }
        });
        // 相册
        chooseFromAlbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{ Manifest.permission. WRITE_EXTERNAL_STORAGE }, 1);
                } else {
                    openAlbum();
                }
            }
        });




        // 打开应用后默认的图片
        Bitmap bmOriginal = BitmapFactory.decodeResource(getResources(), R.drawable.usr);
        picture.setImageBitmap(bmOriginal);
        target.setImageBitmap(bmOriginal);

        sketchImage = new SketchImage.Builder(this, bmOriginal).build();

        final SeekBar seek = (SeekBar) findViewById(R.id.simpleSeekBar);
        final ProgressBar pb = (ProgressBar) findViewById(R.id.ProgressBar);
        final TextView tvPB = (TextView) findViewById(R.id.TextView_ProgressBar);

        tvPB.setText(String.format("%d %%", MAX_PROGRESS));
        seek.setMax(MAX_PROGRESS);
        seek.setProgress(MAX_PROGRESS);
        target.setImageBitmap(sketchImage.getImageAs(effectType, MAX_PROGRESS));

        final TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab().setText("灰度图"));
        tabLayout.addTab(tabLayout.newTab().setText("素描图"));
        tabLayout.addTab(tabLayout.newTab().setText("彩色素描图"));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                effectType = tabLayout.getSelectedTabPosition();
                tvPB.setText(String.format("%d %%", MAX_PROGRESS));
                seek.setMax(MAX_PROGRESS);
                seek.setProgress(MAX_PROGRESS);
                target.setImageBitmap(sketchImage.getImageAs(effectType, MAX_PROGRESS));
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
       });


       seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
           @Override
           public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
               tvPB.setText(String.format("%d %%", seekBar.getProgress()));
           }

           @Override
           public void onStartTrackingTouch(SeekBar seekBar) {

               pb.setVisibility(View.INVISIBLE);
           }

           @Override
           public void onStopTrackingTouch(SeekBar seekBar) {
               pb.setVisibility(View.INVISIBLE);
               target.setImageBitmap(sketchImage.getImageAs(effectType,
                       seekBar.getProgress()));
           }
       });

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
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));


                        // 获得图片的宽高.
                        int width = bitmap.getWidth();
                        int height = bitmap.getHeight();
                        float newWidth = 300;
                        float newHeight = 400;
                        // 计算缩放比例.
                        float scaleWidth = ((float) newWidth) / width;
                        float scaleHeight = ((float) newHeight) / height;
                        // 取得想要缩放的matrix参数.
                        Matrix matrix = new Matrix();
                        matrix.postScale(scaleWidth, scaleHeight);
                        // 得到新的图片.
                        Bitmap bmOriginal = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
                        //Bitmap bmOriginal = bitmap;




                        target = (ImageView) findViewById(R.id.iv_target);
                        target.setImageBitmap(bmOriginal);

                        sketchImage = new SketchImage.Builder(this, bmOriginal).build();

                        final SeekBar seek = (SeekBar) findViewById(R.id.simpleSeekBar);
                        final ProgressBar pb = (ProgressBar) findViewById(R.id.ProgressBar);
                        final TextView tvPB = (TextView) findViewById(R.id.TextView_ProgressBar);

                        tvPB.setText(String.format("%d %%", MAX_PROGRESS));
                        seek.setMax(MAX_PROGRESS);
                        seek.setProgress(MAX_PROGRESS);
                        target.setImageBitmap(sketchImage.getImageAs(effectType, MAX_PROGRESS));


                        final TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
                        tabLayout.addTab(tabLayout.newTab().setText("灰度图"));
                        tabLayout.addTab(tabLayout.newTab().setText("素描图"));
                        tabLayout.addTab(tabLayout.newTab().setText("彩色素描图"));
                        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                            @Override
                            public void onTabSelected(TabLayout.Tab tab) {
                                effectType = tabLayout.getSelectedTabPosition();
                                tvPB.setText(String.format("%d %%", MAX_PROGRESS));
                                seek.setMax(MAX_PROGRESS);
                                seek.setProgress(MAX_PROGRESS);
                                target.setImageBitmap(sketchImage.getImageAs(effectType, MAX_PROGRESS));
                            }

                            @Override
                            public void onTabUnselected(TabLayout.Tab tab) {

                            }

                            @Override
                            public void onTabReselected(TabLayout.Tab tab) {

                            }
                        });




                        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                            @Override
                            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                                tvPB.setText(String.format("%d %%", seekBar.getProgress()));
                            }

                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar) {

                                pb.setVisibility(View.INVISIBLE);
                            }

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar) {
                                pb.setVisibility(View.INVISIBLE);
                                target.setImageBitmap(sketchImage.getImageAs(effectType, seekBar.getProgress()));
                            }
                        });




                        picture.setImageBitmap(bitmap);
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
            //相册的照片
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            Bitmap bmOriginal = bitmap;


            target = (ImageView) findViewById(R.id.iv_target);
            target.setImageBitmap(bmOriginal);

            sketchImage = new SketchImage.Builder(this, bmOriginal).build();

            final SeekBar seek = (SeekBar) findViewById(R.id.simpleSeekBar);
            final ProgressBar pb = (ProgressBar) findViewById(R.id.ProgressBar);
            final TextView tvPB = (TextView) findViewById(R.id.TextView_ProgressBar);

            tvPB.setText(String.format("%d %%", MAX_PROGRESS));
            seek.setMax(MAX_PROGRESS);
            seek.setProgress(MAX_PROGRESS);
            target.setImageBitmap(sketchImage.getImageAs(effectType, MAX_PROGRESS));

            final TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
            tabLayout.addTab(tabLayout.newTab().setText("灰度图"));
            tabLayout.addTab(tabLayout.newTab().setText("素描图"));
            tabLayout.addTab(tabLayout.newTab().setText("彩色素描图"));
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    effectType = tabLayout.getSelectedTabPosition();
                    tvPB.setText(String.format("%d %%", MAX_PROGRESS));
                    seek.setMax(MAX_PROGRESS);
                    seek.setProgress(MAX_PROGRESS);
                    target.setImageBitmap(sketchImage.getImageAs(effectType, MAX_PROGRESS));
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {

                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {

                }
            });


            seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    tvPB.setText(String.format("%d %%", seekBar.getProgress()));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                    pb.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    pb.setVisibility(View.INVISIBLE);
                    target.setImageBitmap(sketchImage.getImageAs(effectType,
                            seekBar.getProgress()));
                }
            });




            picture.setImageBitmap(bitmap);
        } else {
            Toast.makeText(this, "failed to get image", Toast.LENGTH_SHORT).show();
        }
    }





    /*
    For More Detail:
    https://developer.android.com/topic/performance/graphics/load-bitmap
    */
    private Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
                                                   int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}

