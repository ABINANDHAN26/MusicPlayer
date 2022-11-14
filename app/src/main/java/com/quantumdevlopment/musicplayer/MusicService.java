package com.quantumdevlopment.musicplayer;

import static java.lang.Thread.sleep;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.media.session.MediaSessionCompat;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Random;

public class MusicService extends Service {
    //Object Refs
    public static MediaPlayer mediaPlayer; // Object used to play media
    MainActivity mainActivity = new MainActivity(); // Used to access MainActivity class methods
    static Thread seekThread, completionThread; //seekThread is used for seekbar and time of the song, completionThread is to check media is completed or not
    Context myContext;
    static SharedPreferences sharedPreferences;
    static SharedPreferences.Editor editor;
    MediaSessionCompat mediaSessionCompat;
    static AudioManager audioManager;
    static AudioFocusRequest focusRequest;
    static AudioAttributes audioAttributes;
    static AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;
    RemoteViews customBigView, customSmallView;
    Notification notification;
    NotificationManagerCompat notificationManager;
    //Variables
    public static int position; // To Track the postion of the playinng music in SongData list
    String songLocation; // Location of the song that need to played
    static String currentSongTitle; // Saved variable that used when main activity resumes
    int lastProgress, lastPosition; // last position is used to initialize the mediaPlayer object if it got value from sharedPreferences
    public static boolean isShuffle = false;
    public static boolean isRepeat = false;
    public static boolean favourite = false; // To check shuffle / repeat is on & if fav is true means current song is in favorite list
    static int audioFocusRequest; // Value returned from AudioManger.requestAudioFocus


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //initiate the audio attributes
        audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                    start();
                    showNotification(R.drawable.pause_icon);
                } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    pause();
                    showNotification(R.drawable.play_icon);
                } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                    pause();
                    showNotification(R.drawable.play_icon);
                } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                    mediaPlayer.setVolume(30, 30);
                } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    mediaPlayer.setVolume(30, 30);
                else {
                    stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                    notificationManager.cancelAll();
                }
            }
        };

        //set the attributes for thr focus requester
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build();
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        sharedPreferences = getSharedPreferences("lastSongDetails", 0);
        editor = sharedPreferences.edit();
        lastProgress = intent.getIntExtra("lastProgress", 0);
        lastPosition = intent.getIntExtra("lastPosition", -1);
        position = intent.getIntExtra("position", -1);
        myContext = getApplicationContext();
        mediaSessionCompat = new MediaSessionCompat(myContext, "tag");
        if (lastPosition != -1) {
            createMediaPlayer(lastPosition, myContext);
            mediaPlayer.seekTo(lastProgress);
            lastPosition = -1;
        } else if (mediaPlayer == null && position != -1) {
            createMediaPlayer(position, myContext);
            start();
        } else if (mediaPlayer != null && position != -1) {
            if (mediaPlayer.isPlaying()) {
                stop();
                createMediaPlayer(position, myContext);
                start();
            } else if (lastPosition == -1) {
                createMediaPlayer(position, myContext);
                start();
            }
        }


        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                while (mediaPlayer != null) {
                    try {
                        Message msg = new Message();
                        msg.what = getCurrentPosition();
                        handler.sendMessage(msg);
                        sleep(100);
                    } catch (Exception e) {
                        mainActivity.finish();
                        stopSelf();
                    }

                }
            }
        };
        seekThread = new Thread(runnable);
        if (!seekThread.isAlive())
            seekThread.start();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
      //  notificationManager.cancelAll();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;

        }
        super.onDestroy();
    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        onDestroy();
        super.onTaskRemoved(rootIntent);
    }

    public void createMediaPlayer(int position, Context context) {
        currentSongTitle = ListAdapter.mFiles.get(position).getTitle();
        songLocation = ListAdapter.mFiles.get(position).getPath();
        mediaPlayer = MediaPlayer.create(context, Uri.parse(songLocation));
        setLooping(isRepeat);
        favourite = ListAdapter.mFiles.get(position).isFav;
        editor.putString("lastPath", ListAdapter.mFiles.get(position).getPath());
        editor.apply();
        if (MainActivity.isMainVisible) {
            MainActivity.timeSeekBar.setMax(getDuration());
            MainActivity.setViews();
        } else if (CurrentPlaying.isCurrentVisible) {
         CurrentPlaying.timeSeekBar.setMax(getDuration());
         CurrentPlaying.setViews(position);
        }
        MusicService.position = position;
        if (MainActivity.isMainVisible)
            MainActivity.setImage(position);
        else if (CurrentPlaying.isCurrentVisible)
         CurrentPlaying.setImage(position);

    }


    public void start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = audioManager.requestAudioFocus(focusRequest);
            mediaPlayer.start();
            showNotification(R.drawable.pause_icon);
        }
        if (MainActivity.isMainVisible) {
            MainActivity.setViews();
        }
        if (CurrentPlaying.isCurrentVisible) {
            CurrentPlaying.setViews(position);
        }
    }

    public void stop() {
        mediaPlayer.stop();

        if (MainActivity.isMainVisible)
            MainActivity.setViews();
        if (CurrentPlaying.isCurrentVisible)
            CurrentPlaying.setViews(position);
    }

    public void pause() {
        showNotification(R.drawable.play_icon);
        mediaPlayer.pause();
        if (MainActivity.isMainVisible)
            MainActivity.setViews();
        if (CurrentPlaying.isCurrentVisible)
            CurrentPlaying.setViews(position);
    }

    public void seekTo(int progress, Context context) {
        if (progress == getDuration()) {
            stop();
            if (!isLooping()) {
                if (isShuffle) {
                    Random random = new Random();
                    int temp;
                    while (true) {
                        temp = random.nextInt(ListAdapter.mFiles.size());
                        if (temp >= 0 && temp < ListAdapter.mFiles.size() && temp != position) {
                            position = temp;
                            createMediaPlayer(position, context);
                            start();
                            break;
                        }
                    }
                } else {
                    position = position + 1;
                    if (position != -1 && position < ListAdapter.mFiles.size()) {
                        createMediaPlayer(position, context);
                        start();
                    } else {
                        position = 0;
                        createMediaPlayer(0, context);
                        start();
                    }
                }
            } else {
                seekTo(0, context);
                start();
            }
        } else {
            mediaPlayer.seekTo(progress);
        }
    }

    public void playNext(Context context) {
        position = position + 1;
        if (position >= ListAdapter.mFiles.size())
            position = 0;
        if (mediaPlayer.isPlaying())
            stop();
        createMediaPlayer(position, context);
        if (MainActivity.isMainVisible) {
            MainActivity.setListSelection(position);
        }
        start();

    }

    public void playPrevious(Context context) {
        position = position - 1;

        if (position < 0)
            position = ListAdapter.mFiles.size() - 1;
        if (mediaPlayer.isPlaying())
            stop();
        createMediaPlayer(position, context);
        if (MainActivity.isMainVisible) {
            MainActivity.setListSelection(position);
        }
        start();
    }

    public void setLooping(boolean loop) {
        mediaPlayer.setLooping(loop);
    }

    public boolean isLooping() {
        return mediaPlayer.isLooping();
    }

    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    public static int getSession() {
        return mediaPlayer.getAudioSessionId();
    }

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    private void onCompletion() {

    }

    private void showNotification(int playPauseBtn) {
        //Creating Intent to open Main Activity on click of the notification
        Intent openMain = new Intent(MainActivity.context, MainActivity.class);
        openMain.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.context, 0, openMain, PendingIntent.FLAG_CANCEL_CURRENT);
        //Creating Intent to play pause btn
        Intent playIntent = new Intent(MainActivity.context, NotificationReceiver.class).setAction("Play");
        PendingIntent playPending = PendingIntent.getBroadcast(MainActivity.context, 0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT);//PendingIntent Flagupdatecurrent
        //Creating Intent to pre btn
        Intent preIntent = new Intent(MainActivity.context,NotificationReceiver.class).setAction("Pre");
        PendingIntent prePending = PendingIntent.getBroadcast(MainActivity.context, 0, preIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        //Creating Intent to next btn
        Intent nextIntent = new Intent(MainActivity.context,NotificationReceiver.class).setAction("Next");
        PendingIntent nextPending = PendingIntent.getBroadcast(MainActivity.context, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationManager = NotificationManagerCompat.from(MainActivity.context);
        //Initialize Notification Big View
        customBigView = new RemoteViews(MainActivity.context.getPackageName(), R.layout.notification_extend_layout);
        //Initializr Notification Small View
        customSmallView = new RemoteViews(MainActivity.context.getPackageName(), R.layout.notification_collapsed_layout);
        //Set Image and Text to the Big View
        customBigView.setTextViewText(R.id.song_title_notification, ListAdapter.mFiles.get(position).getTitle());
        customBigView.setTextViewText(R.id.artist_notification, ListAdapter.mFiles.get(position).getArtist());
        customBigView.setImageViewResource(R.id.play_icon_notification, playPauseBtn);
        customSmallView.setImageViewResource(R.id.play_icon_collapsed, playPauseBtn);
        byte[] temp = MainActivity.getAlbumArt(ListAdapter.mFiles.get(position).getPath());
        Bitmap album_art;

        if (temp != null) {
            album_art = BitmapFactory.decodeByteArray(temp, 0, temp.length);
            customBigView.setImageViewBitmap(R.id.album_art_notification, album_art);
            customSmallView.setImageViewBitmap(R.id.album_art_collapsed, album_art);
        } else {
            customBigView.setImageViewResource(R.id.album_art_notification, R.drawable.play_icon);
            customSmallView.setImageViewResource(R.id.album_art_collapsed, R.drawable.play_icon);
        }
        customBigView.setImageViewResource(R.id.play_icon_collapsed, playPauseBtn);
        customSmallView.setTextViewText(R.id.song_title_collapsed, ListAdapter.mFiles.get(position).getTitle());
        //Set OnClick Listener to the images
        customBigView.setOnClickPendingIntent(R.id.play_icon_notification, playPending);
        customBigView.setOnClickPendingIntent(R.id.pre_icon_notification, prePending);
        customBigView.setOnClickPendingIntent(R.id.next_icon_notification, nextPending);
        customSmallView.setOnClickPendingIntent(R.id.play_icon_collapsed, playPending);
        notification = new NotificationCompat.Builder(MainActivity.context, com.quantumdevlopment.arplayer.NotificationChannel.CHANNEL_ID)
                .setSmallIcon(playPauseBtn)
                .setCustomBigContentView(customBigView)
                .setCustomContentView(customSmallView)
                .setContentIntent(pendingIntent)
                .setSilent(true)
                .setOngoing(mediaPlayer.isPlaying())
                .setPriority(Notification.PRIORITY_MAX)
                .build();
        notificationManager.notify(1, notification);

    }


    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @SuppressLint("SetTextI10n")
        @Override
        public void handleMessage(@NonNull Message msg) {
            int seekBarPosition = msg.what;
            if (MainActivity.isMainVisible)
                MainActivity.setSeekProgress(seekBarPosition);
            else if (CurrentPlaying.isCurrentVisible)
               CurrentPlaying.setSeekProgress(seekBarPosition);

            editor.putInt("lastProgress", seekBarPosition);
            editor.putBoolean("isShuffleOn", isShuffle);
            editor.putBoolean("isRepeatOn", isRepeat);
            editor.apply();

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {

                    if (!isLooping()) {
                        stop();
                        if (isShuffle) {
                            Random random = new Random();
                            int temp;
                            while (true) {
                                temp = random.nextInt(ListAdapter.mFiles.size());
                                if (temp >= 0 && temp < ListAdapter.mFiles.size() && temp != position) {
                                    position = temp;
                                    createMediaPlayer(position, getApplicationContext());
                                    start();
                                    break;
                                }
                            }
                            if (MainActivity.isMainVisible) {
                                MainActivity.setListSelection(position);
                            }
                        } else {
                            playNext(getApplicationContext());

                        }
                    } else {
                        seekTo(0, getApplicationContext());
                        start();
                    }
                }
            });
        }
    };


}
