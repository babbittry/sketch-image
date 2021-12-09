package com.devs.sketchimage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import java.nio.IntBuffer;


public class SketchImage {

    public static final int ORIGINAL_TO_GRAY = 0;
    public static final int ORIGINAL_TO_SKETCH = 1;
    public static final int ORIGINAL_TO_COLORED_SKETCH = 2;


    // required
    private Context context;
    private Bitmap bitmap;

    private Bitmap bmGray, bmInvert, bmBlur, bmBlend;

    private SketchImage(Builder builder){
        this.context = builder.context;
        this.bitmap = builder.bitmap;
    }

    /**
     * @param type ORIGINAL_TO_GRAY or ORIGINAL_TO_SKETCH and many more..
     * @param value 0 to 100 to control effect
     * @return Processed Bitmap
     */
    //invert:转化 blur:模糊 blend:混合
    public Bitmap getImageAs(int type, int value) {

        switch (type){
            case ORIGINAL_TO_GRAY:
                 bmGray = toGrayscale(bitmap, 101-value); //101-i
                 bmInvert = toInverted(bmGray, 1); //i
                 bmBlur = toBlur(bmInvert, 1); //i
                 bmBlend = colorDodgeBlend(bmBlur, bmGray, 100);
                return bmBlend;

            case ORIGINAL_TO_SKETCH:
                bmGray = toGrayscale(bitmap, 101-value); //101-i
                bmInvert = toInverted(bmGray, value); //i
                bmBlur = toBlur(bmInvert, 100); //i
                bmBlend = colorDodgeBlend(bmBlur, bmGray, 100);
                return bmBlend;

            case ORIGINAL_TO_COLORED_SKETCH:
                bmGray = toGrayscale(bitmap, 100); //101-i
                bmInvert = toInverted(bmGray, value); //i
                bmBlur = toBlur(bmInvert, value); //i
                bmBlend = colorDodgeBlend(bmBlur, bmGray, 100);
                return bmBlend;
        }
        return bitmap;
    }

    // 构造器
    public static class Builder {
        // required
        private Context context;
        private Bitmap bitmap;
        // optional

        public Builder(Context context, Bitmap bitmap){
            this.context = context;
            this.bitmap = bitmap;
        }

        public SketchImage build(){
            return new SketchImage(this);
        }

    }

    /**
     * 转换为灰度图
     * @param bmpOriginal 原始图像
     * @param saturation 饱和度
     * @return 转换后的图像
     */
    private Bitmap toGrayscale(Bitmap bmpOriginal, float saturation) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();

        cm.setSaturation(saturation / 100);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    private Bitmap toInverted(Bitmap src, float i) {
        ColorMatrix colorMatrix_Inverted =
                new ColorMatrix(new float[]{
                        -1, 0, 0, 0, 255,
                        0, -1, 0, 0, 255,
                        0, 0, -1, 0, 255,
                        0, 0, 0, i / 100, 0});

        ColorFilter colorFilter = new ColorMatrixColorFilter(
                colorMatrix_Inverted);

        Bitmap bitmap = Bitmap.createBitmap(src.getWidth(), src.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setColorFilter(colorFilter);
        canvas.drawBitmap(src, 0, 0, paint);

        return bitmap;
    }

    private Bitmap toBlur(Bitmap input, float i) {
        try {
            RenderScript rsScript = RenderScript.create(context);
            Allocation alloc = Allocation.createFromBitmap(rsScript, input);

            ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rsScript, Element.U8_4(rsScript));
            blur.setRadius((i * 25) / 100);
            blur.setInput(alloc);

            Bitmap result = Bitmap.createBitmap(input.getWidth(), input.getHeight(), Bitmap.Config.ARGB_8888);
            Allocation outAlloc = Allocation.createFromBitmap(rsScript, result);

            blur.forEach(outAlloc);
            outAlloc.copyTo(result);

            rsScript.destroy();
            return result;
        } catch (Exception e) {
            // TODO: handle exception
            return input;
        }
    }

    /**
     * Blends 2 bitmaps to one and adds the color dodge blend mode to it.
     */
    public Bitmap colorDodgeBlend(Bitmap source, Bitmap layer, float i) {
        Bitmap base = source.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap blend = layer.copy(Bitmap.Config.ARGB_8888, false);

        IntBuffer buffBase = IntBuffer.allocate(base.getWidth() * base.getHeight());
        base.copyPixelsToBuffer(buffBase);
        buffBase.rewind();

        IntBuffer buffBlend = IntBuffer.allocate(blend.getWidth() * blend.getHeight());
        blend.copyPixelsToBuffer(buffBlend);
        buffBlend.rewind();

        IntBuffer buffOut = IntBuffer.allocate(base.getWidth() * base.getHeight());
        buffOut.rewind();

        while (buffOut.position() < buffOut.limit()) {
            int filterInt = buffBlend.get();
            int srcInt = buffBase.get();

            int redValueFilter = Color.red(filterInt);
            int greenValueFilter = Color.green(filterInt);
            int blueValueFilter = Color.blue(filterInt);

            int redValueSrc = Color.red(srcInt);
            int greenValueSrc = Color.green(srcInt);
            int blueValueSrc = Color.blue(srcInt);

            int redValueFinal = colordodge(redValueFilter, redValueSrc, i);
            int greenValueFinal = colordodge(greenValueFilter, greenValueSrc, i);
            int blueValueFinal = colordodge(blueValueFilter, blueValueSrc, i);

            int pixel = Color.argb((int) (i * 255) / 100, redValueFinal, greenValueFinal, blueValueFinal);
            buffOut.put(pixel);
        }

        buffOut.rewind();

        base.copyPixelsFromBuffer(buffOut);
        blend.recycle();

        return base;
    }

    private int colordodge(int in1, int in2, float i) {
        float image = (float) in2;
        float mask = (float) in1;
        return ((int) ((image == 255) ? image : Math.min(255, (((long) mask << (int) (i * 8) / 100) / (255 - image)))));
    }

}
