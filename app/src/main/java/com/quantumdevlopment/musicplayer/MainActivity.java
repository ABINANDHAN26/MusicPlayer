package com.quantumdevlopment.musicplayer;

import static com.quantumdevlopment.musicplayer.MusicService.currentSongTitle;
import static com.quantumdevlopment.musicplayer.MusicService.mediaPlayer;
import static com.quantumdevlopment.musicplayer.MusicService.position;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MotionEventCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.quantumdevlopment.musicplayer.ui.main.SectionsPagerAdapter;
import com.quantumdevlopment.musicplayer.databinding.ActivityMainBinding;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    //Widgets
    RecyclerView recyclerView;
    RelativeLayout bottomLayout;
    static ImageView playPauseBtn, nextBtn, songImg, settingsImg;
    static TextView songTitle,favTitle;
    static SeekBar timeSeekBar;
    EditText searchBar;
    ImageView sortBtn, favListBtn, playlistBtn;
    Thread favThread; // Thread for getting fav songs from database
    //Object Refs
    static ListAdapter listAdapter;
    static MusicService musicService;
    static SharedPreferences sharedPreferences;
    static SharedPreferences.Editor editor;
    static Context context;
    static com.quantumdevlopment.arplayer.DBHelper dbHelper;
    //Variables
    static ArrayList<SongData> songFiles, searchFiles, favFiles;
    static boolean isMainVisible = true; //TO Check the activity is foreground or not
    int lastProgress, lastMax, lastPosition = -1;
    String lastPath;
    boolean isShuffleOn = false, isRepeatOn = false;
    final static int PERMISSION_REQUEST = 1;
    static String sortOrder = null;
    static boolean isFavOpen = false;
    static String TAG = "chk";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        findViews();
        binding = ActivityMainBinding.inflate(getLayoutInflater());
//        setContentView(binding.getRoot());
setContentView(R.layout.activity_main);
//        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
//        ViewPager viewPager = binding.viewPager;
//        viewPager.setAdapter(sectionsPagerAdapter);
//        TabLayout tabs = binding.tabs;
//        tabs.setupWithViewPager(viewPager);
        sharedPreferences = getSharedPreferences("lastSongDetails", 0);
        editor = sharedPreferences.edit();

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            songFiles = scanMusic(MainActivity.this);
            setSongList(0);
        } else {
            runTimePermission();
        }
        getSharedValues();
        MusicService.isRepeat = isRepeatOn;
        MusicService.isShuffle = isShuffleOn;
        if (lastPath != null)
            lastPosition = getPosition(lastPath);
        musicService = new MusicService();
        Runnable favRun = new Runnable() {
            @Override
            public void run() {
                if (favFiles == null) {
                    dbHelper = new com.quantumdevlopment.arplayer.DBHelper(MainActivity.this);
                    favFiles = new ArrayList<>();
                    ArrayList<String> songList = dbHelper.getFav();
                    if (songList != null) {
                        for (String favSongTitle : songList) {
                            for (SongData song : songFiles) {
                                if (song.getTitle().equals(favSongTitle)) {
                                    favFiles.add(song);
                                }
                            }
                        }
                    }
                }
            }
        };
        favThread = new Thread(favRun);
        if (!(favThread.isAlive())) favThread.start();

    }

    @Override
    protected void onStart() {
        super.onStart();
        isMainVisible = true;
        context = MainActivity.this;
        if (lastPosition != -1) setListSelection(lastPosition);

        Intent intent = new Intent(MainActivity.this, MusicService.class);
        if (lastPosition != -1 && isServiceRunning()) {
            intent.putExtra("lastProgress", lastProgress);
            intent.putExtra("lastPosition", lastPosition);
            startService(intent);
            recyclerView.getLayoutManager().scrollToPosition(lastPosition);
        }
        bottomLayout.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                Intent intent1 = new Intent(MainActivity.this, CurrentPlaying.class);
                intent1.putExtra("position", MusicService.position);
                startActivity(intent1);
                overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_top);
            }
        });
        playPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isServiceRunning()) {
                    intent.putExtra("position", position);
                    startService(intent);
                }
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        musicService.pause();
                        playPauseBtn.setImageResource(R.drawable.play_icon);
                    } else {
                        musicService.start();


                    }
                }

            }
        });
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer != null)
                    musicService.playNext(MainActivity.this);

            }

        });
        bottomLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = MotionEventCompat.getActionMasked(event);
                if (action == MotionEvent.ACTION_UP) {
                    if (mediaPlayer != null) {
                        Intent intent = new Intent(MainActivity.this, CurrentPlaying.class);
                        intent.putExtra("position", MusicService.position);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_top);
                    }
                }
                return true;
            }
        });
        sortBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(MainActivity.this, sortBtn);
                popupMenu.getMenuInflater().inflate(R.menu.sort_menu, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (!(item.getTitle().toString().equalsIgnoreCase(sortOrder))) {
                            switch (item.getTitle().toString()) {
                                case "By Name":
                                    sortOrder = "by Name";
                                    editor.putString("sortOrder", "by Name");
                                    editor.apply();
                                    songFiles = scanMusic(MainActivity.this);
                                    setSongList(0);
                                    break;
                                case "By Size":
                                    sortOrder = "by Size";
                                    editor.putString("sortOrder", "by Size");
                                    editor.apply();
                                    songFiles = scanMusic(MainActivity.this);
                                    setSongList(0);
                                    break;
                                case "By Date":
                                    sortOrder = "by Date";
                                    editor.putString("sortOrder", "by Date");
                                    editor.apply();
                                    songFiles = scanMusic(MainActivity.this);
                                    setSongList(0);
                                    break;
                            }
                            if (mediaPlayer != null) {
                                String path = sharedPreferences.getString("lastPath", null);
                                setListSelection(getPosition(path));
                            }
                        }
                        return true;
                    }
                });
                popupMenu.show();
            }
        });
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String input = s.toString().toLowerCase();
                setSearchSong(input);

            }

            @Override
            public void afterTextChanged(Editable s) {
                position = getPosition(sharedPreferences.getString("lastPath", null));
            }

        });
        favListBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isFavOpen && favFiles != null) {
                    sortBtn.setVisibility(View.INVISIBLE);
                    favListBtn.setVisibility(View.INVISIBLE);
                    playlistBtn.setVisibility(View.INVISIBLE);
                    favTitle.setVisibility(View.VISIBLE);
                    ListAdapter.mFiles = favFiles;
                    setSongList(2);
                    for (SongData song : favFiles) {
                        if (song.getTitle().equals(currentSongTitle)) {
                            position = getPosition(song.getPath());
                            setListSelection(position);
                        } else {
                            setListSelection(-1);
                        }
                    }
                    isFavOpen = true;

                } else
                    Toast.makeText(MainActivity.this, "No Favourite Songs", Toast.LENGTH_SHORT).show();
            }
        });
//        settingsImg.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                PopupMenu popupMenu = new PopupMenu(MainActivity.this, sortBtn);
//                popupMenu.getMenuInflater().inflate(R.menu.setting_menu, popupMenu.getMenu());
//                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
//                    @Override
//                    public boolean onMenuItemClick(MenuItem item) {
//                        String title = item.getTitle().toString();
//                        switch (title) {
//                            case "Create Playlist":
//                                Toast.makeText(MainActivity.this, "Cant create playlist", Toast.LENGTH_SHORT).show();
//                                break;
//                        }
//                        return true;
//                    }
//                });
//
//            }
//        });

    }

    @Override
    public void onBackPressed() {
        if (isFavOpen) {
            sortBtn.setVisibility(View.VISIBLE);
            favListBtn.setVisibility(View.VISIBLE);
            playlistBtn.setVisibility(View.VISIBLE);
            favTitle.setVisibility(View.INVISIBLE);
            ListAdapter.mFiles = songFiles;
            setSongList(0);
            isFavOpen = false;
            position = getPosition(sharedPreferences.getString("lastPath", null));
            setListSelection(position);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isMainVisible = false;

    }


    @Override
    protected void onPostResume() {
        super.onPostResume();
        isMainVisible = true;
        if (isFavOpen) {
            ListAdapter.mFiles = favFiles;
        } else
            ListAdapter.mFiles = songFiles;
        // TO SET THE TITLE IN BOTTOM LAYOUT AFTER ACTIVITY DESTROYED
        // IF ACIVITY DESTROYED IT AND MUSIC STILL PLAYS AND IF THE APP OPENS U NEED INITIALIZE THE GET THE SONG DETAILS CURRENTLY PLAYING
        if (mediaPlayer != null) {
            timeSeekBar.setMax(musicService.getDuration());
            if (mediaPlayer.isPlaying()) {
                String temp = currentSongTitle;
                int pos = -1;
                for (int i = 0; i < songFiles.size(); i++) {
                    if (temp.equalsIgnoreCase(songFiles.get(i).getTitle())) {
                        pos = i;
                    }
                }
                Log.i(TAG, "onPostResume: " + pos);
                setViews();
                //  setImage(pos);
                // setListSelection(pos);
            }
            String input = searchBar.getText().toString().toLowerCase();
            if (!(input.isEmpty())) {
                setSearchSong(input);
            }
        }
    }


    public static void setImage(int position) {
        Log.i(TAG, "setImage: " + position);
        songTitle.setText(ListAdapter.mFiles.get(position).getTitle());
        byte[] art = getAlbumArt(ListAdapter.mFiles.get(position).getPath());
        if (art != null) {
            Glide.with(context.getApplicationContext()).load(art).into(songImg);
        } else {
            Glide.with(context.getApplicationContext()).load(R.drawable.play_icon).into(songImg);
        }
    }

    //Change the song title and play button when music play and pause
    public static void setViews() {
        if (mediaPlayer.isPlaying()) {
            playPauseBtn.setImageResource(R.drawable.pause_icon);
        } else {
            playPauseBtn.setImageResource(R.drawable.play_icon);
        }
    }

    //set seek bar progress while music plays
    public static void setSeekProgress(int progress) {
        timeSeekBar.setProgress(progress);
    }

    //scan storage for music files
    public ArrayList<SongData> scanMusic(Context context) {
        long id = 0;
        sortOrder = sharedPreferences.getString("sortOrder", "by Name");
        String order = null;
        ArrayList<SongData> tempList = new ArrayList<>();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        switch (sortOrder) {
            case "by Name":
                order = MediaStore.MediaColumns.TITLE + " ASC";
                break;
            case "by Date":
                order = MediaStore.MediaColumns.DATE_ADDED + " ASC";
                break;
            case "by Size":
                order = MediaStore.MediaColumns.SIZE + " DESC";
                break;
        }
        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST,

        };
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, order);
        if (cursor != null) {
            boolean isFav = false;
            while (cursor.moveToNext()) {
                String title = cursor.getString(0);
                String path = cursor.getString(1);
                String album = cursor.getString(2);
                String artist = cursor.getString(3);

                isFav = false;

                SongData songData = new SongData(title, path, album, artist, id, isFav);
                tempList.add(songData);
                id++;

            }
            cursor.close();
        }

        return tempList;

    }


    //Get Permission to access device's storage
    public void runTimePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST);
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                songFiles = scanMusic(MainActivity.this);
                setSongList(0);
            } else {
                Toast.makeText(MainActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }


    //TO SET THE LIST OF SONGS
    private void setSongList(int value) {
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        if (!(songFiles.size() < 1)) {
            if (value == 0)
                listAdapter = new ListAdapter(getApplicationContext(), songFiles);
            else if (value == 1)
                listAdapter = new ListAdapter(getApplicationContext(), searchFiles);
            else
                listAdapter = new ListAdapter(getApplicationContext(), favFiles);
            listAdapter.setHasStableIds(true);
            recyclerView.setAdapter(listAdapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext(), RecyclerView.VERTICAL, false));
        }
    }

    // TO CHECK WHETHER THE SERVICE IS RUNNING OR NOT
    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MusicService.class.getName().equals(service.service.getClassName())) {
                return false;
            }
        }
        return true;
    }

    // TO GET LAST POSITION OF THE SONG IN THE ARRAYLIST
    private int getPosition(String lastPath) {
        String temp;
        for (int i = 0; i < ListAdapter.mFiles.size(); i++) {
            temp = ListAdapter.mFiles.get(i).getPath();
            if (temp.equalsIgnoreCase(lastPath)) {
                return i;
            }
        }
        return -1;
    }

    private void getSharedValues() {

        lastPath = sharedPreferences.getString("lastPath", null);
        lastProgress = sharedPreferences.getInt("lastProgress", 0);
        lastMax = sharedPreferences.getInt("lastMax", 0);
        isShuffleOn = sharedPreferences.getBoolean("isShuffleOn", false);
        isRepeatOn = sharedPreferences.getBoolean("isRepeatOn", false);
    }

    public static byte[] getAlbumArt(String uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri);
        byte[] art = retriever.getEmbeddedPicture();
        retriever.release();
        return art;
    }

    public static void setListSelection(int position) {
        if (ListAdapter.preSelectedPos == -1) {
            ListAdapter.preSelectedPos = position;
            listAdapter.notifyItemChanged(position);
        } else
            ListAdapter.preSelectedPos = ListAdapter.selectedPosition;
        ListAdapter.selectedPosition = position;
        listAdapter.notifyItemChanged(ListAdapter.preSelectedPos);
        listAdapter.notifyItemChanged(ListAdapter.selectedPosition);

    }

    private void setSearchSong(String input) {
        searchFiles = new ArrayList<>();
        for (SongData song : songFiles) {
            if (song.getTitle().toLowerCase().contains(input)) {
                searchFiles.add(song);
            }
        }
        setSongList(1);
        if (mediaPlayer != null) {
            String path = sharedPreferences.getString("lastPath", null);
            setListSelection(getPosition(path));
        }
    }

    public boolean chkIsFav(String title) {
        dbHelper = new com.quantumdevlopment.arplayer.DBHelper(MainActivity.this);
        return dbHelper.isFav(title);

    }

    //Update the  favourite list if fav added or  removed
    public static void updateFavFile(int position, boolean what) {
        if (what) {
            favFiles.add(songFiles.get(position));
        } else {
            favFiles.remove(songFiles.get(position));
        }
    }

    //Initialize the views
    private void findViews() {
        songFiles = new ArrayList<>();
        recyclerView = findViewById(R.id.song_list_view);
        bottomLayout = findViewById(R.id.btm_layout);
        songTitle = findViewById(R.id.song_title);
        playPauseBtn = findViewById(R.id.play_pause);
        timeSeekBar = findViewById(R.id.time_seek_bar_current_song);
        nextBtn = findViewById(R.id.next_btn);
        songImg = findViewById(R.id.current_song_img);
        sortBtn = findViewById(R.id.sort_icon);
        searchBar = findViewById(R.id.search_bar);
        favListBtn = findViewById(R.id.fav_icon);
        playlistBtn = findViewById(R.id.playlist_icon);
        favTitle = findViewById(R.id.fav_title);
    }

}