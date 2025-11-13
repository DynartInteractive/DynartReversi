package net.dynart.reversi;

import android.content.Context;
import android.graphics.RectF;
import android.view.MotionEvent;

public class MenuDifficulty extends Scene {

    private Button but_easy;
    private Button but_medium;
    private Button but_hard;

    public MenuDifficulty(Context context)
    {
        super(context);

        but_easy = new Button(new RectF(458, 140, width - 20, 210), "Easy");
        but_medium = new Button(new RectF(458, 218, width - 20, 288), "Medium");
        but_hard = new Button(new RectF(458, 296, width - 20, 366), "Hard");

        buttons.add(but_easy);
        buttons.add(but_medium);
        buttons.add(but_hard);
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

        boolean clicked = false;

        if (but_easy.isClicked(down, up))
        {
            Board.maxRunDepth = 1;
            clicked = true;
        }
        else if (but_medium.isClicked(down, up))
        {
            Board.maxRunDepth = 3;
            clicked = true;
        }
        else if (but_hard.isClicked(down, up))
        {
            Board.maxRunDepth = 5;
            clicked = true;
        }

        if (clicked)
        {
            playSound(sound_menu);

            Main main = (Main)getContext();
            main.setScene(main.menu_color);
        }

        invalidate();

        return true;
    }

    @Override
    public void onBackPressed()
    {
        playSound(sound_menu);

        Main main = (Main)getContext();
        main.setScene(main.menu);
    }


}
