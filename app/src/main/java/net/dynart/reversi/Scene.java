package net.dynart.reversi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint;
import android.media.SoundPool;
import android.media.AudioManager;

import java.util.ArrayList;
import java.util.Iterator;

public class Scene extends View {

    // bitmaps
    private static Bitmap bg;
    private static Bitmap logo;
    private static Bitmap dynart_logo;
    public static Bitmap dynart_splash;
    private static Bitmap table;
    private static Bitmap message_bg;
    public static Bitmap white;
    public static Bitmap white_dot;
    public static Bitmap white_x;
    public static Bitmap black;
    public static Bitmap black_dot;
    public static Bitmap black_x;
    public static Bitmap number_bg;
    public static Bitmap highlight;

    // painters
    protected static Paint filter_paint;
    protected static Paint rect_paint;
    protected static Paint text_paint;

    // screen values
    protected static int real_width;
    protected static int real_height;
    protected static float width;
    protected static float full_width;
    protected static float height;
    protected static float pixel_ratio;

    // sounds
    protected static int sound_menu;
    protected static int sound_move;
    protected static int sound_end;

    protected static boolean sound_on;

    protected static SoundPool sound_pool;
    protected static AudioManager mgr;

    // touch events
    protected Coord down;
    protected Coord up;

    // buttons
    protected ArrayList<Button> buttons;

    // message box
    protected boolean msg_show;
    protected String msg_text;
    protected Button msg_but_ok;
    protected Button msg_but_cancel;

    // fonts
    protected Typeface typeface;

    // board
    protected static BoardNative board;


    public static void init(int w, int h)
    {
        real_width = w;
        real_height = h;

        pixel_ratio = (float)h / 480;

        width = (float)w * 480 / (float)h;
        full_width = width;
        if (width > 800) { // maximum width: 800
            width = 800;
        }
        height = 480;


        filter_paint = new Paint();
        filter_paint.setFilterBitmap(true);

        rect_paint = new Paint();

        text_paint = new Paint();
        text_paint.setTextAlign(Paint.Align.CENTER);

        text_paint.setAntiAlias(true);


        sound_pool = new SoundPool(4, AudioManager.STREAM_MUSIC, 100);

        sound_on = true;

    }

    public void convertRectF(RectF r, boolean useOffset)
    {
        r.top = r.top * pixel_ratio;
        r.bottom = r.bottom * pixel_ratio;
        r.left = (r.left + (useOffset ? getOffsetX() : 0)) * pixel_ratio;
        r.right = (r.right + (useOffset ? getOffsetX() : 0)) * pixel_ratio;
    }

    public void drawBitmap(Canvas canvas, Bitmap bmp, RectF r)
    {
        drawBitmap(canvas, bmp, r, true);
    }

    public void drawBitmap(Canvas canvas, Bitmap bmp, RectF r, boolean ratio)
    {
        if (ratio)
        {
            convertRectF(r, true);
        }

        canvas.drawBitmap(
            bmp, new Rect(0, 0, bmp.getWidth(), bmp.getHeight()), r, filter_paint
        );

    }

    public void drawRect(Canvas canvas, RectF r, boolean transparent, boolean useOffset)
    {
        convertRectF(r, useOffset);

        if (transparent)
        {
            rect_paint.setARGB(128, 0, 0, 0);
        }
        else
        {
            rect_paint.setARGB(255, 0, 0, 0);
        }

        canvas.drawRect(r, rect_paint);
    }

    public void drawRect(Canvas canvas, RectF r, boolean useOffset)
    {
        drawRect(canvas, r, true, useOffset);
    }

    public void drawText(Canvas canvas, String text, float x, float y, float size, int color)
    {
        size *= pixel_ratio;

        text_paint.setTextSize(size);
        text_paint.setARGB(255, color, color, color);

        x += getOffsetX();
        x *= pixel_ratio;
        y *= pixel_ratio;
        y += size;

        canvas.drawText(text, x, y, text_paint);
    }

    public Scene(Context context)
    {
        super(context);

        typeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/Enigma__2.TTF");
        text_paint.setTypeface(typeface);

        if (bg == null) bg = BitmapFactory.decodeResource(getResources(), R.drawable.bg);
        if (logo == null) logo = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
        if (dynart_logo == null) dynart_logo = BitmapFactory.decodeResource(getResources(), R.drawable.dynart_logo);
        if (dynart_splash == null) dynart_splash = BitmapFactory.decodeResource(getResources(), R.drawable.dynart_splash);
        if (table == null) table = BitmapFactory.decodeResource(getResources(), R.drawable.table);
        if (message_bg == null) message_bg = BitmapFactory.decodeResource(getResources(), R.drawable.message_bg);
        if (white == null) white = BitmapFactory.decodeResource(getResources(), R.drawable.white);
        if (white_dot == null) white_dot = BitmapFactory.decodeResource(getResources(), R.drawable.white_dot);
        if (white_x == null) white_x = BitmapFactory.decodeResource(getResources(), R.drawable.white_x);
        if (black == null) black = BitmapFactory.decodeResource(getResources(), R.drawable.black);
        if (black_dot == null) black_dot = BitmapFactory.decodeResource(getResources(), R.drawable.black_dot);
        if (black_x == null) black_x = BitmapFactory.decodeResource(getResources(), R.drawable.black_x);
        if (number_bg == null) number_bg = BitmapFactory.decodeResource(getResources(), R.drawable.number_bg);
        if (highlight == null) highlight = BitmapFactory.decodeResource(getResources(), R.drawable.highlight);

        sound_menu = sound_pool.load(getContext(), R.raw.menu, 1);
        sound_move = sound_pool.load(getContext(), R.raw.move, 1);
        sound_end  = sound_pool.load(getContext(), R.raw.end, 1);

        mgr = (AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);

        down = new Coord(0, 0);
        up = new Coord(0, 0);

        buttons = new ArrayList<Button>();

        msg_show = false;
        msg_text = "";


        float x = (width-519) / 2;

        msg_but_ok = new Button(new RectF(x+59, 261, x+259, 317), "OK");
        msg_but_ok.setLayer(1);
        msg_but_ok.setEnabled(false);

        msg_but_cancel = new Button(new RectF(x+261, 261, x+461, 317), "Cancel");
        msg_but_cancel.setLayer(1);
        msg_but_cancel.setEnabled(false);

        buttons.add(msg_but_ok);
        buttons.add(msg_but_cancel);

        board = new BoardNative();
        board.setStartPosition();

    }

    public static void playSound(int sound)
    {
        if (!sound_on) return;

        float streamVolumeCurrent = mgr.getStreamVolume(AudioManager.STREAM_MUSIC);
        float streamVolumeMax = mgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        float volume = streamVolumeCurrent / streamVolumeMax;

        sound_pool.play(sound, volume, volume, 1, 0, 1f);
    }

    public float getOffsetX() {
        return (full_width - width) / 2;
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        drawBitmap(canvas, bg, new RectF(0, 0, real_width, real_height), false);
        drawBitmap(canvas, table, new RectF(15, 27, 440, 452));
        drawBitmap(canvas, logo, new RectF(456, 19, 637, 62));
        drawBitmap(canvas, dynart_logo, new RectF(width - 145, height - 57, width - 20, height - 20));

        drawBoard(canvas);

        buttonsDrawLayer(canvas, 0);
    }

    protected RectF getDiscRectF(int i, int j)
    {
        return new RectF(16 + i*53, 28 + j*53, 16 + i*53 + 53, 28 + j*53 + 53);
    }

    public void drawBoard(Canvas canvas)
    {
        int i, j, piece;

        for (j = 0; j < 8; j++)
        {
            for (i = 0; i < 8; i++)
            {
                piece = board.getPiece(i,  j);

                if (piece == BoardNative.PIECE_EMPTY) continue;

                drawBitmap(canvas, piece == BoardNative.PIECE_LIGHT ? Scene.white : Scene.black, getDiscRectF(i, j));
            }
        }
    }

    protected void showMsg(String text)
    {
        msg_show = true;
        msg_text = text;
        buttonsSetEnabled(false, true);
    }

    protected void hideMsg()
    {
        msg_show = false;
        buttonsSetEnabled(true, false);
    }

    protected boolean drawMsg(Canvas canvas)
    {
        if (!msg_show) return false;

        float x = (width-519) / 2;

        drawRect(canvas, new RectF(0, 0, full_width, 480), true, false);
        drawBitmap(canvas, message_bg, new RectF(x, 132, x+519, 347));
        drawText(canvas, msg_text, x + 519/2, 170, 45, 0);
        drawText(canvas, msg_text, x + 519/2, 167, 45, 255);

        buttonsDrawLayer(canvas, 1);

        return true;
    }

    private void buttonsDrawLayer(Canvas canvas, int layer)
    {
        Button button;
        Iterator<Button> it = buttons.iterator();
        while (it.hasNext())
        {
            button = it.next();

            if (button.getLayer() == layer)
            {
                button.draw(canvas, this);
            }
        }
    }

    private void buttonsSetEnabled(boolean l0, boolean l1)
    {
        Button button;
        Iterator<Button> it = buttons.iterator();
        while (it.hasNext())
        {
            button = it.next();

            if (button.getLayer() == 0)
            {
                button.setEnabled(l0);
            }
            else
            {
                button.setEnabled(l1);
            }
        }

    }

    private void buttonsTransparent()
    {
        Iterator<Button> it = buttons.iterator();
        while (it.hasNext())
        {
            it.next().transparent();
        }
    }

    private void buttonsSetTransparent()
    {
        Iterator<Button> it = buttons.iterator();
        while (it.hasNext())
        {
            it.next().setTransparent(down);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {

        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            down.x = (int)(event.getRawX() * 1/pixel_ratio) - (int)getOffsetX();
            down.y = (int)(event.getRawY() * 1/pixel_ratio);

            buttonsSetTransparent();
        }
        else if (event.getAction() == MotionEvent.ACTION_UP)
        {
            up.x = (int)(event.getRawX() * 1/pixel_ratio) - (int)getOffsetX();
            up.y = (int)(event.getRawY() * 1/pixel_ratio);

            buttonsTransparent();
        }

        return true;
    }

    public void onBackPressed()
    {
    }

}
