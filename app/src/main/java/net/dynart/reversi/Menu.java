package net.dynart.reversi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.MotionEvent;

public class Menu extends Scene {

    private Button but_single;
    private Button but_multi;
    private Button but_sound;

    public Menu(Context context)
    {
        super(context);

        but_single = new Button(new RectF(458, 120, width - 20, 190), "Single");
        but_multi = new Button(new RectF(458, 198, width - 20, 268), "Multi");
        but_sound = new Button(new RectF(458, 316, width - 20, 386), "Sound");

        buttons.add(but_single);
        buttons.add(but_multi);
        buttons.add(but_sound);
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

        if (but_single.isClicked(down, up))
        {
            playSound(sound_menu);

            Main main = (Main)getContext();
            main.setScene(main.menu_difficulty);
        }
        else if (but_multi.isClicked(down, up))
        {
            playSound(sound_menu);

            Main main = (Main)getContext();
            main.game.start(Board.PIECE_EMPTY);
            main.setScene(main.game);
        }
        else if (but_sound.isClicked(down, up))
        {
            Scene.sound_on = !Scene.sound_on;
            but_sound.setSelected(Scene.sound_on);
        }
        else if (msg_but_ok.isClicked(down, up))
        {
            playSound(sound_menu);

            Main main = (Main)getContext();
            main.finish();
        }
        else if (msg_but_cancel.isClicked(down, up))
        {
            playSound(sound_menu);

            hideMsg();
        }

        invalidate();

        return true;
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
/*
        canvas.save();
        canvas.translate(40, 100);
        canvas.rotate(45);

        Scene.drawBitmap(canvas, Scene.dynart_splash, new RectF(-128, -128, 127, 127) );
        canvas.restore();
*/

        drawMsg(canvas);
    }

    @Override
    public void onBackPressed()
    {
        playSound(sound_menu);

        if (!msg_show)
        {
            showMsg("Exit game?");

            invalidate();

            return;
        }

        Main main = (Main)getContext();
        main.finish();
    }

}
