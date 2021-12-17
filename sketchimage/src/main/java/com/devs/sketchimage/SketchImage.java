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
    private final Context context;
    private final Bitmap bitmap;

    private Bitmap bmGray, bmInvert, bmBlur, bmBlend;

    private SketchImage(final Builder builder){
        this.context = builder.context;
        this.bitmap = builder.bitmap;
    }

    /**
     * @param type ORIGINAL_TO_GRAY or ORIGINAL_TO_SKETCH and many more..
     * @param value 0 to 100 to control effect
     * @return Processed Bitmap
     */
    //invert:转化 blur:模糊 blend:混合
    public Bitmap getImageAs(final int type, final int value) {

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
        private final Context context;
        private final Bitmap bitmap;
        // optional

        public Builder(final Context context, final Bitmap bitmap){
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
    private Bitmap toGrayscale(final Bitmap bmpOriginal, final float saturation) {
        final int width;
        final int height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        final Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final Canvas c = new Canvas(bmpGrayscale);
        final Paint paint = new Paint();
        final ColorMatrix cm = new ColorMatrix();

        cm.setSaturation(saturation / 100);
        final ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    private Bitmap toInverted(final Bitmap src, final float i) {
        final ColorMatrix colorMatrix_Inverted =
                new ColorMatrix(new float[]{
                        -1, 0, 0, 0, 255,
                        0, -1, 0, 0, 255,
                        0, 0, -1, 0, 255,
                        0, 0, 0, i / 100, 0});

        final ColorFilter colorFilter = new ColorMatrixColorFilter(
                colorMatrix_Inverted);

        final Bitmap bitmap = Bitmap.createBitmap(src.getWidth(), src.getHeight(),
                Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);

        final Paint paint = new Paint();
        paint.setColorFilter(colorFilter);
        canvas.drawBitmap(src, 0, 0, paint);

        return bitmap;
    }

    private Bitmap toBlur(final Bitmap input, final float i) {
        try {
            final RenderScript rsScript = RenderScript.create(context);
            final Allocation alloc = Allocation.createFromBitmap(rsScript, input);

            final ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rsScript, Element.U8_4(rsScript));
            blur.setRadius((i * 25) / 100);
            blur.setInput(alloc);

            final Bitmap result = Bitmap.createBitmap(input.getWidth(), input.getHeight(), Bitmap.Config.ARGB_8888);
            final Allocation outAlloc = Allocation.createFromBitmap(rsScript, result);

            blur.forEach(outAlloc);
            outAlloc.copyTo(result);

            rsScript.destroy();
            return result;
        } catch (final Exception e) {
            // TODO: handle exception
            return input;
        }
    }

    /**
     * Blends 2 bitmaps to one and adds the color dodge blend mode to it.
     */
    public Bitmap colorDodgeBlend(final Bitmap source, final Bitmap layer, final float i) {
        final Bitmap base = source.copy(Bitmap.Config.ARGB_8888, true);
        final Bitmap blend = layer.copy(Bitmap.Config.ARGB_8888, false);

        final IntBuffer buffBase = IntBuffer.allocate(base.getWidth() * base.getHeight());
        base.copyPixelsToBuffer(buffBase);
        buffBase.rewind();

        final IntBuffer buffBlend = IntBuffer.allocate(blend.getWidth() * blend.getHeight());
        blend.copyPixelsToBuffer(buffBlend);
        buffBlend.rewind();

        final IntBuffer buffOut = IntBuffer.allocate(base.getWidth() * base.getHeight());
        buffOut.rewind();

        while (buffOut.position() < buffOut.limit()) {
            final int filterInt = buffBlend.get();
            final int srcInt = buffBase.get();

            final int redValueFilter = Color.red(filterInt);
            final int greenValueFilter = Color.green(filterInt);
            final int blueValueFilter = Color.blue(filterInt);

            final int redValueSrc = Color.red(srcInt);
            final int greenValueSrc = Color.green(srcInt);
            final int blueValueSrc = Color.blue(srcInt);

            final int redValueFinal = colordodge(redValueFilter, redValueSrc, i);
            final int greenValueFinal = colordodge(greenValueFilter, greenValueSrc, i);
            final int blueValueFinal = colordodge(blueValueFilter, blueValueSrc, i);

            final int pixel = Color.argb((int) (i * 255) / 100, redValueFinal, greenValueFinal, blueValueFinal);
            buffOut.put(pixel);
        }

        buffOut.rewind();

        base.copyPixelsFromBuffer(buffOut);
        blend.recycle();

        return base;
    }

    private int colordodge(final int in1, final int in2, final float i) {
        final float image = (float) in2;
        final float mask = (float) in1;
        return ((int) ((image == 255) ? image : Math.min(255, (((long) mask << (int) (i * 8) / 100) / (255 - image)))));
    }

}
