package com.example.reflexgame.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.example.reflexgame.R;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Reflexview extends View {
    private static final String HIGH_SCORE = "HIGH_SCORE";
    private SharedPreferences preferences;

    private int spotsTouched;
    private int score;
    private int level;
    private int viewWidth;
    private int viewHeight;
    private int highScore;
    private long animationTime;
    private boolean gameOver, gamePaused, dialogDisplayed;

    //collections types for our circle/spots
    private final Queue<ImageView> spots = new ConcurrentLinkedDeque<>();
    private final Queue<Animator> animators = new ConcurrentLinkedDeque<>();

    private TextView highScoreTextView, currentScoreTextView, levelTextView;
    private LinearLayout livesLinearLayout;
    private RelativeLayout relativeLayout;
    private Resources resources;
    private LayoutInflater layoutInflater;

    public static final int INITIAL_ANIMATION_DURATION = 6000; //6 SEC
    public static final Random random = new Random();
    public static final float SCALE_X = 0.25f;
    public static final float SCALE_Y = 0.25f;
    public static final int SPOT_DIAMETER = 100;
    public static final int INITIAL_SPOTS = 5;
    public static final int SPOT_DELAY = 500;
    public static final int LIVES = 3;
    public static final int MAX_LIVES = 7;
    public static final int NEW_LEVEL = 10;
    private Handler spothandler;

    public static final int HIT_SOUND_ID = 1;
    public static final int MISS_SOUND_ID = 2;
    public static final int DISAPPEAR_SOUND_ID = 3;
    public static final int SOUND_PRIORITY = 1;
    public static final int SOUND_QUALITY = 100;
    public static final int MAX_STREAMS = 4;

    private SoundPool soundPool;
    private int volume;
    private Map<Integer, Integer> soundMap;

    public Reflexview(Context context, SharedPreferences sharedPreferences, RelativeLayout parentLayout) {
        super(context);
        preferences = sharedPreferences;
        highScore = preferences.getInt(HIGH_SCORE, 0);
        //save resources for loading external values
        resources = context.getResources();
        //save LAyoutInflater
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        //setup UI components
        relativeLayout = parentLayout;
        livesLinearLayout = relativeLayout.findViewById(R.id.lifeLinearLayout);
        highScoreTextView = relativeLayout.findViewById(R.id.highScoreTextView);
        currentScoreTextView = relativeLayout.findViewById(R.id.scoreTextView);
        levelTextView = relativeLayout.findViewById(R.id.levelTextView);

        spothandler = new Handler();
    }
//checked
    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
        viewWidth = width;
        viewHeight = height;
    }

   //checked
    public void pause() {
        gamePaused = true;
        soundPool.release();
        soundPool = null;
        cancelAnimation();
    }
    private void cancelAnimation() {
        for (Animator animator : animators)
            animator.cancel();
        for (ImageView view : spots)
            relativeLayout.removeView(view);

        spothandler.removeCallbacks(addSpotRunnable);
        animators.clear();
        spots.clear();
    }
//c
    public void resume(Context context) {
        gamePaused = false;
        initializeSoundEffects(context);

        if(!dialogDisplayed)
            resetGame();
    }
//c
    public void resetGame() {
        spots.clear();
        animators.clear();
        livesLinearLayout.removeAllViews();
        animationTime = INITIAL_ANIMATION_DURATION;
        spotsTouched = 0;
        score = 0;
        level = 1;
        gameOver = false;
        displayScores();
        //add Lives
        for (int i = 0; i < LIVES; i++) {
            //add life indicator to screen
            livesLinearLayout.addView(
                    (ImageView) layoutInflater.inflate(R.layout.life, null));
        }
        for (int i = 1; i <= INITIAL_SPOTS; ++i)
            spothandler.postDelayed(addSpotRunnable, i * SPOT_DELAY);

    }
//c
    private void initializeSoundEffects(Context context) {

        soundPool= new SoundPool(MAX_STREAMS,AudioManager.STREAM_MUSIC,SOUND_QUALITY);
        //set sound effect volume
        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        volume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
        //create a sound map
        soundMap = new HashMap<>();
        soundMap.put(HIT_SOUND_ID, soundPool.load(context, R.raw.hit, SOUND_PRIORITY));
        soundMap.put(MISS_SOUND_ID, soundPool.load(context, R.raw.miss, SOUND_PRIORITY));
        soundMap.put(DISAPPEAR_SOUND_ID, soundPool.load(context, R.raw.disappear, SOUND_PRIORITY));
    }
//c
    private void displayScores()
    {
        highScoreTextView.setText(resources.getString
                (R.string.high_score) + " " + highScore);
        levelTextView.setText(resources.getString
                (R.string.level) + " " + level);
        currentScoreTextView.setText(resources.getString
                (R.string.score) + " " + score);
    }
//c
    private Runnable addSpotRunnable = new Runnable() {
        @Override
        public void run() {
            addNewSpot();
        }
    };


//c
    public void addNewSpot() {
        int x = random.nextInt(viewWidth- SPOT_DIAMETER);
        int y = random.nextInt(viewHeight- SPOT_DIAMETER);
        int x2 = random.nextInt(viewWidth- SPOT_DIAMETER);
        int y2 = random.nextInt(viewHeight- SPOT_DIAMETER);
        //CREATE ACTUAL SPOT/CIRCLE
        final ImageView spot = (ImageView) layoutInflater.inflate
                (R.layout.untouched, null);

        spots.add(spot);
        spot.setLayoutParams(new RelativeLayout.LayoutParams(SPOT_DIAMETER, SPOT_DIAMETER));
        spot.setImageResource(random.nextInt(2) == 0 ? R.drawable.greenspot : R.drawable.redspot);
        spot.setX(x);
        spot.setY(y);

        spot.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                touchedSpot(spot);
            }
        });
        relativeLayout.addView(spot); //adding circle to screen

        //add spot animations
        spot.animate().x(x2).y(y2).scaleX(SCALE_X).scaleY(SCALE_Y)
                .setDuration(animationTime).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                animators.add(animation); //save for later time
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                animators.remove(animation);
                if (!gamePaused && spots.contains(spot)) {
                    missedSpot(spot);
                }
            }
        });

    }
    //c
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (soundPool != null)
            soundPool.play(HIT_SOUND_ID, volume, volume, SOUND_PRIORITY, 0, 1.0F);
        score -= 15 * level;
        score = Math.max(score, 0); //do not let score go below zero
        displayScores();
        return true;
    }
    private void touchedSpot(ImageView spot) {
        relativeLayout.removeView(spot);
        spots.remove(spot);

        ++spotsTouched; //increment the number of spots touched
        score += 10 * level;

        if(soundPool!=null)
            soundPool.play(HIT_SOUND_ID,volume,volume,SOUND_PRIORITY,0,1f);

        if (spotsTouched % NEW_LEVEL == 0) {
            ++level;
            animationTime *= 0.95;
            if (livesLinearLayout.getChildCount() < MAX_LIVES) {
                ImageView life = (ImageView) layoutInflater.inflate(R.layout.life, null);
                livesLinearLayout.addView(life);
            }
        }
        displayScores();
        if (!gameOver)
            addNewSpot();

    }
//c
    private void missedSpot(ImageView spot) {
        spots.remove(spot);
        relativeLayout.removeView(spot);
        if (gameOver)
            return;
        if (soundPool != null)
            soundPool.play(DISAPPEAR_SOUND_ID, volume, volume, SOUND_PRIORITY, 0, 1f);
        if (livesLinearLayout.getChildCount() == 0) { //we lost
            gameOver = true;
            if (score > highScore) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt(HIGH_SCORE, score);
                editor.apply(); //can also use commit func
                highScore = score;
            }

            cancelAnimation();
            AlertDialog.Builder builder= new AlertDialog.Builder(getContext());
            builder.setTitle("GAME OVER");
            builder.setMessage("SCORE:"+score);
            builder.setPositiveButton("RESET", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    displayScores();
                    dialogDisplayed=false;
                    resetGame();
                }
            });
            dialogDisplayed=true;
            builder.show();
        }
        else{
            livesLinearLayout.removeViewAt(livesLinearLayout.getChildCount()-1);
            addNewSpot();
        }
    }


}
