package com.friendinneed.ua.friendinneed;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;

/**
 * Created by mymac on 9/25/16.
 */

public class Utils {


    public static Bitmap getCroppedBitmap(Bitmap bitmap) {


//        Bitmap bitmap = Bitmap.createScaledBitmap(bitmapInput, bitmapInput.getWidth()*2, bitmapInput.getHeight()*2, false);
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);

//            Bitmap output = Bitmap.createBitmap(150,
//                    150, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        // canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                100 / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
//        Bitmap _bmp = Bitmap.createScaledBitmap(output, bitmap.getHeight(), bitmap.getHeight(), false);
        return output;
    }
}
