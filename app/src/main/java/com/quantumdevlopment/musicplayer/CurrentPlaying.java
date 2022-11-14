package com.quantumdevlopment.musicplayer;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MotionEventCompat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.quantumdevlopment.musicplayer.ListAdapter;
import com.quantumdevlopment.musicplayer.MainActivity;
import com.quantumdevlopment.musicplayer.MusicService;
import com.quantumdevlopment.musicplayer.R;
import com.quantumdevlopment.arplayer.DBHelper;
import java.util.ArrayList;


import static com.quantumdevlopment.musicplayer.MusicService.isRepeat;
import static com.quantumdevlopment.musicplayer.MusicService.isShuffle;
import static com.quantumdevlopment.musicplayer.MusicService.mediaPlayer;

public class CurrentPlaying extends AppCompatActivity implements View.OnTouchListener, GestureDetector.OnGestureListener {
    static ImageView playPauseBtn, preBtn, nextBtn, repeatBtn, shuffleBtn, songImg, favouriteImg,playlistImg,backBtn;
    static SeekBar timeSeekBar;
    static TextView songTitle, elapsedTime, remainingTime;
    static int position;
    String song_title;
    static boolean isCurrentVisible = false;
    static MusicService musicService;
    private static Context mContext;
    GestureDetector gestureDetector;
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_playing);
        findViews();
        mContext = this;
        musicService = new MusicService();
        gestureDetector = new GestureDetector(this, this);
        position = getIntent().getIntExtra("position", -1);
        if (position != -1) {
            song_title = ListAdapter.mFiles.get(position).getTitle();
            songTitle.setText(song_title);
            timeSeekBar.setMax(musicService.getDuration());
            timeSeekBar.setProgress(musicService.getCurrentPosition());
            String elapsed_time = createTime(musicService.getCurrentPosition());
            String remaining_time = createTime(musicService.getDuration() - musicService.getCurrentPosition());
            elapsedTime.setText(elapsed_time);
            remainingTime.setText(remaining_time);
            setViews(position);
            setImage(position);
        }


    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onStart() {
        super.onStart();
        isCurrentVisible = true;
        songImg.setOnTouchListener(this);
        playPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        musicService.pause();
                        playPauseBtn.setImageResource(R.drawable.play_icon);
                    } else if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                        musicService.start();
                        playPauseBtn.setImageResource(R.drawable.pause_icon);
                    }
                }
            }
        });
        timeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                musicService.seekTo(seekBar.getProgress(), CurrentPlaying.this);

            }
        });
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                musicService.playNext(CurrentPlaying.this);
            }
        });
        preBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                musicService.playPrevious(CurrentPlaying.this);
            }
        });
        shuffleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isShuffle) {
                    shuffleBtn.setColorFilter(getResources().getColor(R.color.icon_color));
                    isShuffle = true;
                } else {
                    shuffleBtn.setColorFilter(getResources().getColor(R.color.icon_disabled_color));
                    isShuffle = false;

                }
            }
        });
        repeatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRepeat) {
                    repeatBtn.setImageResource(R.drawable.repeat_one_icon);
                    isRepeat = true;
                    musicService.setLooping(true);
                } else {
                    repeatBtn.setImageResource(R.drawable.repeat_icon_disabled);
                    isRepeat = false;
                    musicService.setLooping(false);
                }
            }
        });

        favouriteImg.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                DBHelper dbHelper = new DBHelper(CurrentPlaying.this);
                if (ListAdapter.mFiles.get(MusicService.position).isFav) {
                    favouriteImg.setColorFilter(CurrentPlaying.mContext().getResources().getColor(R.color.icon_disabled_color));
                    MusicService.favourite = false;
                    dbHelper.delFav(MusicService.position);
                    ListAdapter.mFiles.get(MusicService.position).isFav = false;
                  //  MainActivity.updateFavFile(MusicService.position,false);
                } else {
                    favouriteImg.setColorFilter(CurrentPlaying.mContext().getResources().getColor(R.color.fav_heart));
                    MusicService.favourite = true;
                    ListAdapter.mFiles.get(MusicService.position).isFav = true;
                    dbHelper.addFav(MusicService.position);
                    MainActivity.updateFavFile(MusicService.position,true);
                }
            }
        });
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_down);
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isCurrentVisible = false;
    }

    //set seek bar progress while music plays
    public static void setSeekProgress(int progress) {
        timeSeekBar.setProgress(progress);
        String elapsed_time = createTime(progress);
        String remaining_time = createTime(mediaPlayer.getDuration() - progress);
        elapsedTime.setText(elapsed_time);
        remainingTime.setText(remaining_time);
    }

    public static void setImage(int position) {
      /*  byte[] art = MainActivity.getAlbumArt(ListAdapter.mFiles.get(position).getPath());
        if (art != null) {
            Glide.with(MainActivity.context.getApplicationContext()).load(art).into(songImg);
        } else {
            Glide.with(MainActivity.context.getApplicationContext()).load(R.drawable.play_icon).into(songImg);
        }*/
    }

    public static void setViews(int position) {
        songTitle.setText(ListAdapter.mFiles.get(position).getTitle());
        if (isShuffle) {
            shuffleBtn.setColorFilter(CurrentPlaying.mContext().getResources().getColor(R.color.icon_color));
        } else {
            shuffleBtn.setColorFilter(CurrentPlaying.mContext().getResources().getColor(R.color.icon_disabled_color));
        }
        if (isRepeat) {
            repeatBtn.setImageResource(R.drawable.repeat_one_icon);
        } else {
            repeatBtn.setImageResource(R.drawable.repeat_icon_disabled);
        }
        if (mediaPlayer.isPlaying()) {
            playPauseBtn.setImageResource(R.drawable.pause_icon);
        } else {
            playPauseBtn.setImageResource(R.drawable.play_icon);
        }
        if (ListAdapter.mFiles.get(position).isFav) {
            favouriteImg.setColorFilter(CurrentPlaying.mContext().getResources().getColor(R.color.fav_heart));
            MusicService.favourite = true;
        } else {
            favouriteImg.setColorFilter(CurrentPlaying.mContext().getResources().getColor(R.color.icon_disabled_color));
            MusicService.favourite = false;
        }

    }

    //TO INITIALIZE THE VIEWS
    private void findViews() {
        playPauseBtn = findViewById(R.id.play_pause_current_playing);
        preBtn = findViewById(R.id.pre_song_btn);
        nextBtn = findViewById(R.id.next_song_btn);
        repeatBtn = findViewById(R.id.repeat_btn);
        shuffleBtn = findViewById(R.id.shuffle_btn);
        timeSeekBar = findViewById(R.id.time_seek_bar_current_song);
        songTitle = findViewById(R.id.song_title_current_playing);
        elapsedTime = findViewById(R.id.elapsed_time_text);
        remainingTime = findViewById(R.id.remaining_time_text);
        songImg = findViewById(R.id.song_img_current_playing);
        favouriteImg = findViewById(R.id.favourite);
        playlistImg = findViewById(R.id.playlist);
        backBtn = findViewById(R.id.arrow_back);
    }

    private static Context mContext() {
        return mContext;
    }

    private static String createTime(int milliSeconds) {
        int min = milliSeconds / 1000 / 60;
        int sec = milliSeconds / 1000 % 60;
        String time;
        time = min + ":";
        if (sec < 10) time += "0";
        time += sec;
        return time;

    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        boolean result = false;
        try {
            float diffY = e2.getY() - e1.getY();
            float diffX = e2.getX() - e1.getX();
            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        //    onSwipeRight();
                        musicService.playPrevious(mContext);
                    } else {
                        //  onSwipeLeft();
                        musicService.playNext(mContext);
                    }
                    result = true;
                }
            } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffY > 0) {
                    ///  onSwipeBottom();
                } else {
                    //  onSwipeTop();
                }
                result = true;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return result;

    }
}