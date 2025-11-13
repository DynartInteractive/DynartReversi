package net.dynart.reversi;

import android.content.Context;
import android.graphics.RectF;
import android.view.MotionEvent;

public class MenuColor extends Scene {

    private Button but_white;
    private Button but_black;

    public MenuColor(Context context)
    {
        super(context);

        but_white = new Button(new RectF(458, 170, width - 20, 240), "White");
        but_black = new Button(new RectF(458, 248, width - 20, 318), "Black");

        buttons.add(but_white);
        buttons.add(but_black);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        super.onTouchEvent(event);

        if (event.getAction() != MotionEvent.ACTION_UP)
        {
            invalidate();
            return true;
        }

        int cpu = 0;

        if (but_black.isClicked(down, up))
        {
            cpu = BoardNative.PIECE_LIGHT;
        }
        else if (but_white.isClicked(down, up))
        {
            cpu = BoardNative.PIECE_DARK;
        }

        if (cpu != 0)
        {
            playSound(sound_menu);

            Main main = (Main)getContext();
            main.game.start(cpu);
            main.setScene(main.game);
        }

        invalidate();

        return true;
    }

    @Override
    public void onBackPressed()
    {
        playSound(sound_menu);

        Main main = (Main)getContext();
        main.setScene(main.menu_difficulty);
    }
}
