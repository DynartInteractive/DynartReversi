package net.dynart.reversi;

import android.graphics.RectF;
import android.graphics.Canvas;

public class Button {

    private final RectF r;
    private final String text;
    private boolean transp;
    private boolean enabled;
    private boolean selected;
    private int layer;

    public Button(RectF r, String text)
    {
        this.r = r;
        this.text = text;

        transp = true;
        enabled = true;
        selected = true;
        layer = 0;
    }

    public void setSelected(boolean s)
    {
        selected = s;
    }

    public void setLayer(int l)
    {
        layer = l;
    }

    public int getLayer()
    {
        return layer;
    }

    public void draw(Canvas canvas, Scene scene)
    {
        scene.drawRect(canvas, new RectF(r), transp, true);
        scene.drawText(canvas, text, r.left + (r.right - r.left) / 2, (r.bottom - r.top) / 2 + r.top - 20, 30, selected ? 183 : 80);
    }

    public void setTransparent(Coord down)
    {
        if (!enabled)
        {
            transp = true;
            return;
        }

        transp = down.x < r.left || down.y < r.top || down.x > r.right || down.y > r.bottom;
    }

    public void transparent()
    {
        transp = true;
    }

    public void setEnabled(boolean e)
    {
        enabled = e;
    }

    public boolean isClicked(Coord down, Coord up)
    {
        if (enabled && up.x > r.left && up.y > r.top && up.x < r.right && up.y < r.bottom &&
                down.x > r.left && down.y > r.top && down.x < r.right && down.y < r.bottom)
        {
            return true;
        }

        return false;
    }

}

