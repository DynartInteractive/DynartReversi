package net.dynart.reversi;

import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Main extends AppCompatActivity {


    public Game game;
    public Menu menu;
    public MenuDifficulty menu_difficulty;
    public MenuColor menu_color;
    public Logo logo;

    private Scene active_scene;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        // media volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // init drawing
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        Scene.init(metrics.widthPixels, metrics.heightPixels);

        // start
        game = new Game(this);
        game.start(BoardNative.PIECE_EMPTY);

        menu = new Menu(this);
        menu_difficulty = new MenuDifficulty(this);
        menu_color = new MenuColor(this);

        //logo = new Logo(this);

        EdgeToEdge.enable(this);

        setScene(menu);
    }

    public void setScene(Scene scene)
    {
        active_scene = scene;
        setContentView(scene);

    }
    @Override
    public void onBackPressed() {

        active_scene.onBackPressed();
        return;
    }
}