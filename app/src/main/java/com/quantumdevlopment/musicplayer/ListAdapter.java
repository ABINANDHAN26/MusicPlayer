
package com.quantumdevlopment.musicplayer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.quantumdevlopment.arplayer.DBHelper;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.MyViewHolder> {
    private final Context myContext;
    public static ArrayList<SongData> mFiles;
    static public int selectedPosition = -1;
    static public int preSelectedPos = -1;

    ListAdapter(Context myContext, ArrayList<SongData> mFiles) {
        ListAdapter.mFiles = mFiles;
        this.myContext = myContext;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(myContext).inflate(R.layout.list_items, parent, false);
        return new MyViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        if (position == selectedPosition) {

            holder.itemView.setBackgroundResource(R.drawable.btm_background);
            holder.songArtist.setTextColor(Color.BLACK);
            holder.songTitle.setTextColor(Color.BLACK);
            holder.listItemLayout.setElevation(10);


        } else {
            holder.itemView.setBackgroundColor(Color.WHITE);
            holder.songTitle.setTextColor(Color.BLACK);
            holder.songArtist.setTextColor(Color.parseColor("#777474"));
            holder.listItemLayout.setElevation(0);


        }
        holder.songTitle.setText(mFiles.get(position).getTitle());
        holder.songArtist.setText(mFiles.get(position).getArtist());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (preSelectedPos == -1)
                    preSelectedPos = position;
                else
                    preSelectedPos = selectedPosition;
                selectedPosition = position;
                notifyItemChanged(preSelectedPos);
                notifyItemChanged(position);
                Intent intent = new Intent(myContext, MusicService.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("position", position);
                intent.putExtra("isFirst", true);

                myContext.startService(intent);


            }
        });
        if (MainActivity.isFavOpen) {
            holder.menuBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popupMenu = new PopupMenu(MainActivity.context, holder.menuBtn);
                    popupMenu.getMenuInflater().inflate(R.menu.popup, popupMenu.getMenu());
                    Menu menu = popupMenu.getMenu();
                    menu.getItem(0).setTitle("Remove Favourite");
                    menu.getItem(1).setVisible(false);
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            String title = item.getTitle().toString();
                            switch (title) {
                                case "Remove Favourite":
                                    Toast.makeText(MainActivity.context,"Removed",Toast.LENGTH_SHORT).show();
                                    DBHelper dbHelper = new DBHelper(myContext);
                                    MusicService.favourite = false;
                                    dbHelper.delFav(position);
                                    ListAdapter.mFiles.get(position).isFav = false;
                                    MainActivity.updateFavFile(position, false);
                                    notifyDataSetChanged();
                                    break;
                            }
                            return true;
                        }
                    });
                    popupMenu.show();
                }
            });
        } else {
            holder.menuBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popupMenu = new PopupMenu(MainActivity.context, holder.menuBtn);
                    popupMenu.getMenuInflater().inflate(R.menu.popup, popupMenu.getMenu());
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            String title = item.getTitle().toString();
                            switch (title) {
                                case "Add Favourites":
                                    if (mFiles.get(position).isFav)
                                        Toast.makeText(MainActivity.context, "Already in Favourites", Toast.LENGTH_SHORT).show();
                                    else {
                                        DBHelper dbHelper = new DBHelper(myContext);
                                        MusicService.favourite = true;
                                        mFiles.get(position).isFav = true;
                                        dbHelper.addFav(position);
                                        MainActivity.updateFavFile(position, true);
                                        Toast.makeText(MainActivity.context, "Added to favourites", Toast.LENGTH_SHORT).show();
                                    }
                                    break;
                                case "Add to Playlist":
                                    Toast.makeText(MainActivity.context, "Added to Playlist", Toast.LENGTH_SHORT).show();
                                    break;
                            }
                            return true;
                        }
                    });
                    popupMenu.show();

                }
            });
        }


    }

    @Override
    public int getItemCount() {
        return mFiles.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView songTitle, songArtist;
        ImageView  menuBtn;
        RelativeLayout listItemLayout;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            songTitle = itemView.findViewById(R.id.song_title_text);

            songArtist = itemView.findViewById(R.id.song_artist_text);
            menuBtn = itemView.findViewById(R.id.menu);
            listItemLayout = itemView.findViewById(R.id.list_item_layout);
        }

    }

    @Override
    public long getItemId(int position) {
        return mFiles.get(position).getId();
    }


}
