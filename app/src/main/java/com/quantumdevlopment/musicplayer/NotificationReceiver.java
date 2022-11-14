package com.quantumdevlopment.musicplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String actionName = intent.getAction();
        MusicService musicService = new MusicService();
        if(actionName != null){
            switch (actionName){
                case "Play":
                    if(MusicService.mediaPlayer.isPlaying()){
                        musicService.pause();
                    }else{
                        musicService.start();
                    }
                    break;
                case "Pre":
                    musicService.playPrevious(context);
                    break;
                case "Next":
                    musicService.playNext(context);
                    break;
            }
        }
    }
}
