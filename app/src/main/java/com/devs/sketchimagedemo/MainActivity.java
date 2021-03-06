package com.devs.sketchimagedemo;

import android.content.res.Resources;
import android.graphics.Matrix;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.google.android.material.tabs.TabLayout;
import androidx.constraintlayout.widget.ConstraintLayout;
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
//    ??????????????????
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.about:
                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                dialog.setTitle("????????????");
                dialog.setMessage("??????????????????????????????\n" +
                        "???????????????????????? ????????? ?????? ?????????\n" +
                        "????????????????????????\n" +
                        "???????????????h821021@126.com");
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

        // ??????
        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // ??????File???????????????????????????????????????
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
                // ??????????????????
                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(intent, TAKE_PHOTO);
            }
        });
        // ??????
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




        // ??????????????????????????????
        Bitmap bmOriginal = BitmapFactory.decodeResource(getResources(), R.drawable.usr);
        picture.setImageBitmap(bmOriginal);
        target.setImageBitmap(bmOriginal);

        sketchImage = new SketchImage.Builder(this, bmOriginal).build();

        final SeekBar seek = (SeekBar) findViewById(R.id.simpleSeekBar);
        final TextView tvPB = (TextView) findViewById(R.id.TextView_ProgressBar);

        tvPB.setText(String.format("%d %%", MAX_PROGRESS));
        seek.setMax(MAX_PROGRESS);
        seek.setProgress(MAX_PROGRESS);
        target.setImageBitmap(sketchImage.getImageAs(effectType, MAX_PROGRESS));

        final TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab().setText("?????????"));
        tabLayout.addTab(tabLayout.newTab().setText("?????????"));
        tabLayout.addTab(tabLayout.newTab().setText("???????????????"));
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

           }

           @Override
           public void onStopTrackingTouch(SeekBar seekBar) {
               target.setImageBitmap(sketchImage.getImageAs(effectType,
                       seekBar.getProgress()));
           }
       });

    }





    private void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent, CHOOSE_PHOTO); // ????????????
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
                        // ??????????????????????????????
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));


                        // ?????????????????????.
                        int width = bitmap.getWidth();
                        int height = bitmap.getHeight();
                        float newWidth = 300;
                        float newHeight = 400;
                        // ??????????????????.
                        float scaleWidth = ((float) newWidth) / width;
                        float scaleHeight = ((float) newHeight) / height;
                        // ?????????????????????matrix??????.
                        Matrix matrix = new Matrix();
                        matrix.postScale(scaleWidth, scaleHeight);
                        // ??????????????????.
                        Bitmap bmOriginal = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
                        //Bitmap bmOriginal = bitmap;




                        target = (ImageView) findViewById(R.id.iv_target);
                        target.setImageBitmap(bmOriginal);

                        sketchImage = new SketchImage.Builder(this, bmOriginal).build();

                        final SeekBar seek = (SeekBar) findViewById(R.id.simpleSeekBar);
                        final TextView tvPB = (TextView) findViewById(R.id.TextView_ProgressBar);

                        tvPB.setText(String.format("%d %%", MAX_PROGRESS));
                        seek.setMax(MAX_PROGRESS);
                        seek.setProgress(MAX_PROGRESS);
                        target.setImageBitmap(sketchImage.getImageAs(effectType, MAX_PROGRESS));


                        final TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
                        tabLayout.addTab(tabLayout.newTab().setText("?????????"));
                        tabLayout.addTab(tabLayout.newTab().setText("?????????"));
                        tabLayout.addTab(tabLayout.newTab().setText("???????????????"));
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

                            }

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar) {
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
                    // ???????????????????????????
                    if (Build.VERSION.SDK_INT >= 19) {
                        // 4.4?????????????????????????????????????????????
                        handleImageOnKitKat(data);
                    } else {
                        // 4.4??????????????????????????????????????????
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
            // ?????????document?????????Uri????????????document id??????
            String docId = DocumentsContract.getDocumentId(uri);
            if("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1]; // ????????????????????????id
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // ?????????content?????????Uri??????????????????????????????
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            // ?????????file?????????Uri?????????????????????????????????
            imagePath = uri.getPath();
        }
        displayImage(imagePath); // ??????????????????????????????
    }

    private void handleImageBeforeKitKat(Intent data) {
        Uri uri = data.getData();
        String imagePath = getImagePath(uri, null);
        displayImage(imagePath);
    }

    private String getImagePath(Uri uri, String selection) {
        String path = null;
        // ??????Uri???selection??????????????????????????????
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
            }
            cursor.close();
        }
        return path;
    }

    private void displayImage(String imagePath) {
        if (imagePath != null) {
            //???????????????
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            Bitmap bmOriginal = bitmap;


            target = (ImageView) findViewById(R.id.iv_target);
            target.setImageBitmap(bmOriginal);

            sketchImage = new SketchImage.Builder(this, bmOriginal).build();

            final SeekBar seek = (SeekBar) findViewById(R.id.simpleSeekBar);
            final TextView tvPB = (TextView) findViewById(R.id.TextView_ProgressBar);

            tvPB.setText(String.format("%d %%", MAX_PROGRESS));
            seek.setMax(MAX_PROGRESS);
            seek.setProgress(MAX_PROGRESS);
            target.setImageBitmap(sketchImage.getImageAs(effectType, MAX_PROGRESS));

            final TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
            tabLayout.addTab(tabLayout.newTab().setText("?????????"));
            tabLayout.addTab(tabLayout.newTab().setText("?????????"));
            tabLayout.addTab(tabLayout.newTab().setText("???????????????"));
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

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
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

