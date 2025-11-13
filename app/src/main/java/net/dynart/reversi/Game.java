package net.dynart.reversi;

import net.dynart.reversi.Board.GameResult;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.os.Bundle;

public class Game extends Scene {

    private int draw_count;
    private Button but_restart;
    private Button but_menu;
    private boolean msg_restart;
    private Coord last_move;
    private int cpu;
    private boolean end;

    public Game(Context context)
    {
        super(context);

        draw_count = 0;

        but_restart = new Button(new RectF(458, 236, width - 20, 306), "Restart");
        but_menu = new Button(new RectF(458, 314, width - 20, 384), "Menu");

        buttons.add(but_restart);
        buttons.add(but_menu);

        msg_restart = false;

        last_move = new Coord(0, 0);

        setKeepScreenOn(true);
    }

    public void start(int cpu)
    {
        board.setStartPosition();

        last_move.x = -1;

        this.cpu = cpu;

        end = false;
    }

    private RectF getHighlightRectF(Coord c)
    {
        return new RectF(9 + c.x*53, 21 + c.y*53, 9 + c.x*53 + 67, 21 + c.y*53 + 67);
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        Coord[] moves = board.getMoves();

        for (int i = 0; i < moves.length; i++)
        {
            drawBitmap(
                    canvas, Scene.highlight, getHighlightRectF(moves[i])
            );
        }

        if (last_move.x != -1)
        {
            if (board.isDark())
            {
                drawBitmap(canvas, Scene.white_x, getDiscRectF(last_move.x, last_move.y));
            }
            else
            {
                drawBitmap(canvas, Scene.black_x, getDiscRectF(last_move.x, last_move.y));
            }
        }

        Integer count;

        drawBitmap(canvas, Scene.white, new RectF(451, 80, 451 + 53, 80 + 53));
        if (!board.isDark()) drawBitmap(canvas, Scene.white_dot, new RectF(451, 80, 451 + 53, 80 + 53));

        drawBitmap(canvas, Scene.number_bg, new RectF(512, 90, 512 + 110, 90 + 33));
        count = new Integer(board.getLightPiecesCount());
        drawText(canvas, count.toString(), 512 + 55, 80 + 13, 20, 255);

        drawBitmap(canvas, Scene.black, new RectF(451, 140, 451 + 53, 140 + 53));
        if (board.isDark()) drawBitmap(canvas, Scene.black_dot, new RectF(451, 140, 451 + 53, 140 + 53));

        drawBitmap(canvas, Scene.number_bg, new RectF(512, 150, 512 + 110, 150 + 33));
        count = new Integer(board.getDarkPiecesCount());
        drawText(canvas, count.toString(), 512 + 55, 140 + 13, 20, 255);

        if (board.getGameResult() == GameResult.DRAW)
        {
            drawBoardMsg(canvas, "Draw");
        }
        else if (board.getGameResult() == GameResult.DARK_WINS)
        {
            drawBoardMsg(canvas, "Black wins!");
        }
        else if (board.getGameResult() == GameResult.LIGHT_WINS)
        {
            drawBoardMsg(canvas, "White wins!");
        }

        if (board.getGameResult() != GameResult.UNKNOWN)
        {
            if (!end)
            {
                playSound(sound_end);
            }

            end = true;
        }

        if (drawMsg(canvas)) return;

        // update (silly, but works :)

        if (((!board.isDark() && cpu == Board.PIECE_LIGHT) || (board.isDark() && cpu == Board.PIECE_DARK)) && board.getGameResult() == GameResult.UNKNOWN)
        {
            drawBoardMsg(canvas, "Thinking...");

            if (draw_count == 2)
            {
                Coord move = board.run();
                board.makeMove(move);

                last_move.x = move.x;
                last_move.y = move.y;

                draw_count = 0;

                playSound(sound_move);

                invalidate();
            }

            if (draw_count < 2)
            {
                draw_count++;
                invalidate();
            }
        }

    }

    public void drawBoardMsg(Canvas canvas, String text)
    {
        drawRect(canvas, new RectF(25, 220, 430, 276), true);
        drawText(canvas, text, 228, 223, 40, 0);
        drawText(canvas, text, 228, 220, 40, 255);
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

        // board events
        if (!msg_show && up.x > 15 && up.y > 27 && up.x < 440 && up.y < 452 &&
                down.x > 15 && down.y > 27 && down.x < 440 && down.y < 452 &&
                ((board.isDark() && cpu == Board.PIECE_LIGHT) || (!board.isDark() && cpu == Board.PIECE_DARK) || cpu == Board.PIECE_EMPTY))
        {
            up.x -= 15;
            up.y -= 27;

            up.x /= 53;
            up.y /= 53;

            if (board.makeMove(up))
            {
                last_move.x = up.x;
                last_move.y = up.y;

                playSound(sound_move);
            }
        }

        // button events
        if (but_restart.isClicked(down, up))
        {
            playSound(sound_menu);

            showMsg("Restart game?");
            msg_restart = true;
        }
        else if (but_menu.isClicked(down, up))
        {
            playSound(sound_menu);

            showMsg("Back to menu?");
            msg_restart = false;
        }
        else if (msg_but_cancel.isClicked(down, up))
        {
            playSound(sound_menu);

            hideMsg();
        }
        else if (msg_but_ok.isClicked(down, up))
        {
            if (msg_restart)
            {
                playSound(sound_menu);

                start(cpu);
                hideMsg();
            }
            else
            {
                playSound(sound_menu);

                hideMsg();

                Main main = (Main)getContext();
                main.setScene(main.menu);
            }
        }

        invalidate();

        return true;
    }

    @Override
    public void onBackPressed()
    {
        playSound(sound_menu);

        if (!msg_show)
        {
            showMsg("Back to menu?");
            msg_restart = false;

            invalidate();

            return;
        }

        if (msg_restart)
        {
            hideMsg();
            invalidate();
        }
        else
        {
            hideMsg();

            Main main = (Main)getContext();
            main.setScene(main.menu);
        }
    }

    public void saveState(Bundle bundle)
    {

    }


    public void restoreState(Bundle bundle)
    {

    }

}
