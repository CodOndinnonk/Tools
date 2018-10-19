package com.sezam.gbsfo.sezam.Helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

/**
 * Class for image modifications
 */
public class ImageHelper {

    /**
     * Perform cut img to round corners and set to the set imageView
     *
     * @param context
     * @param imageView      view to set modified img
     * @param drawableId     id of img res
     * @param roundCornersDp dp of rounding corners (as big as round)
     */
    public static void setCroppedWithCornersBitmap(
            @NonNull final Context context,
            @NonNull final ImageView imageView,
            final int drawableId,
            final int roundCornersDp) {
        if (context == null) {
            LogHelper.e("Context is NULL");
            return;
        }
        if (imageView == null) {
            LogHelper.e("imageView is NULL. It has to be set, as sing to host modified img");
            return;
        }
        if (roundCornersDp <= 0) {
            LogHelper.w("roundCornersDp has to be >= 1. roundCornersDp = " + roundCornersDp);
        }
        ViewTreeObserver vto = imageView.getViewTreeObserver();

        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                imageView.getViewTreeObserver().removeOnPreDrawListener(this);
                imageView.setImageBitmap(
                        ImageHelper.getRoundedCornerBitmap(
                                context,
                                ImageHelper.getCenterCropBitmap(
                                        ImageHelper.convertToBitmap(
                                                context.getDrawable(drawableId)
                                        ),
                                        imageView.getMeasuredWidth(),
                                        imageView.getMeasuredHeight()
                                ),
                                roundCornersDp
                        )
                );

                return true;
            }
        });
    }

    /**
     * Perform converting to Bitmap object
     *
     * @param drawable object to convert
     * @return -> Bitmap of object
     * <p>-> NULL, if any error
     */
    public static Bitmap convertToBitmap(@NonNull Drawable drawable) {
        if (drawable == null) {
            LogHelper.e("drawable is NULL. Can't perform any action with NULL object");
            return null;
        }
        return ((BitmapDrawable) drawable).getBitmap();
    }

    /**
     * Perform cutting img from center
     *
     * @param srcBmp img to modify
     * @param width  of output img
     * @param height of output img
     * @return -> Bitmap with modified dimensions
     * <p>-> income Bitmap (no modifications), if width or height <= income Bitmap dimensions
     * <p>-> NULL, if any error
     */
    public static Bitmap getCenterCropBitmap(@NonNull Bitmap srcBmp, int width, int height) {
        if (srcBmp == null) {
            LogHelper.e("srcBmp is NULL. Can't perform any action with NULL object");
            return null;
        }
        if (width < 1 || height < 1) {
            LogHelper.w("Dimensions has to be >= 1. width = " + width + " height = " + height);
        }
        if (width <= srcBmp.getWidth() && height <= srcBmp.getHeight()) {
            return Bitmap.createBitmap(
                    srcBmp,
                    0,
                    0,
                    width,
                    height
            );
        }

        return srcBmp;
    }

    /**
     * Perform rounding img
     *
     * @param context
     * @param bitmap  img to modify
     * @param roundDp to round corners (as big as round)
     * @return -> modified Bitmap object
     * <p>-> NULL, if any error
     */
    public static Bitmap getRoundedCornerBitmap(@NonNull Context context, @NonNull Bitmap bitmap, int roundDp) {
        if (context == null) {
            LogHelper.e("Context is NULL");
            return null;
        }
        if (bitmap == null) {
            LogHelper.e("bitmap is NULL. Can't perform any action with NULL object");
            return null;
        }
        if (roundDp <= 0) {
            LogHelper.w("roundCornersDp has to be >= 1. roundDp = " + roundDp);
        }
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        //maybe in future fix color, and set as parameter
        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = ScreenHelper.convertDpToPx(context, roundDp);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }


    /**
     * Perform resize of bitmap. Scale proportional, so img save it's shape. Resize only square img, without getHeight (other shape will have outSize as biggest side). Img size will multiply with a coefficient of each side.
     * Coefficient is calculating as min({@code outSize}-{@code incomeImg.getWidth};{@code outSize}-{@code incomeImg.getHeight})
     *
     * @param incomeImg bitmap for transformation
     * @param outSize   size of output bitmap
     * @return -> transformed bitmap to {@code outSize} size
     * <p>-> income bitmap, if {@code outSize} = income bitmap size (width or height)
     * <p>-> NULL, if any error
     */
    public static Bitmap resizeImageProportional(@NonNull Bitmap incomeImg, float outSize) {
        if (incomeImg == null) {
            LogHelper.e("incomeImg is NULL, can't operate with NULL object.");
            return null;
        }

        if (outSize == incomeImg.getWidth() || outSize == incomeImg.getHeight()) {
            LogHelper.i("No transformation performed. outSize is the same as income img. outSize = " + outSize + " incomeImg.getWidth() = " + incomeImg.getWidth() + " incomeImg.getHeight() = " + incomeImg.getHeight());
            return incomeImg;
        }

        float ratio = Math.min(
                outSize / incomeImg.getWidth(),
                outSize / incomeImg.getHeight());
        int width = Math.round(ratio * incomeImg.getWidth());
        int height = Math.round(ratio * incomeImg.getHeight());

        Bitmap newBitmap = Bitmap.createScaledBitmap(incomeImg, width,
                height, true);

        if (newBitmap == null) {
            LogHelper.e("Resized bitmap is NULL, please check.");
        }

        return newBitmap;
    }

    /**
     * Perform hard resize of bitmap with deformation. Output img will have provided dimensions.
     *
     * @param incomeImg  bitmap for transformation, doesn't matter shape and size
     * @param heightSize height of output img in pixels
     * @param widthSize  width of output img in pixels
     * @return transformed img. Can be deformed (stretched)
     */
    public static Bitmap resizeImageHard(@NonNull Bitmap incomeImg, float heightSize, float widthSize) {
        if (incomeImg == null) {
            LogHelper.e("incomeImg is NULL, can't operate with NULL object.");
            return null;
        }

        if (widthSize == incomeImg.getWidth() || heightSize == incomeImg.getHeight()) {
            LogHelper.i("No transformation performed. Output dimens are the same as income img. widthSize = " + widthSize + " incomeImg.getWidth() = " + incomeImg.getWidth() + " heightSize = " + heightSize + " incomeImg.getHeight() = " + incomeImg.getHeight());
            return incomeImg;
        }

        int width = Math.round(widthSize / incomeImg.getWidth() * incomeImg.getWidth());
        int height = Math.round(heightSize / incomeImg.getHeight() * incomeImg.getHeight());

        Bitmap newBitmap = Bitmap.createScaledBitmap(incomeImg, width,
                height, true);

        if (newBitmap == null) {
            LogHelper.e("Resized bitmap is NULL, please check.");
        }

        return newBitmap;
    }

}
