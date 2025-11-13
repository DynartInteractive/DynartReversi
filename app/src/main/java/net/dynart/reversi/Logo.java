package net.dynart.reversi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.os.SystemClock;

public class Logo extends Scene {

    private final long time;

    public Logo(Context context)
    {
        super(context);

        time = SystemClock.elapsedRealtime();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        super.onTouchEvent(event);

        if (event.getAction() != MotionEvent.ACTION_UP) return true;

        Main main = (Main)getContext();
        main.setScene(main.menu);

        return true;
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        drawRect(canvas, new RectF(0, 0, full_width, 480), false, false);
        drawBitmap(canvas, Scene.dynart_splash, new RectF((int)(width / 2) - 128, 112, (int)(width / 2) + 128, 367));

        if (time + 3000 > SystemClock.elapsedRealtime())
        {
            invalidate();
        }
        else
        {
            Main main = (Main)getContext();
            main.setScene(main.menu);
        }
    }

}
