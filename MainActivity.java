package com.maqz.videoplayer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.ArrayList;

public class MainActivity extends Activity {
    private static final int PICK_VIDEO = 100;

    private LibVLC libVLC;
    private MediaPlayer mediaPlayer;
    private VLCVideoLayout videoLayout;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createPlayer();
        createInterface();
        mediaPlayer.attachViews(videoLayout, null, false, false);
    }

    private void createPlayer() {
        ArrayList<String> options = new ArrayList<>();
        options.add("--no-video-title-show");
        libVLC = new LibVLC(this, options);
        mediaPlayer = new MediaPlayer(libVLC);
    }

    private void createInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(this);
        title.setText("MAQZ VIDEO PLAYER");
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        videoLayout = new VLCVideoLayout(this);
        root.addView(videoLayout, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        status = new TextView(this);
        status.setText("Select a video to begin");
        status.setGravity(Gravity.CENTER);
        root.addView(status);

        Button select = new Button(this);
        select.setText("Open Video");
        select.setOnClickListener(v -> openVideo());
        root.addView(select);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);

        Button play = new Button(this);
        play.setText("Play");
        play.setOnClickListener(v -> {
            if (mediaPlayer.getMedia() != null) {
                mediaPlayer.play();
            } else {
                openVideo();
            }
        });

        Button pause = new Button(this);
        pause.setText("Pause");
        pause.setOnClickListener(v -> mediaPlayer.pause());

        Button stop = new Button(this);
        stop.setText("Stop");
        stop.setOnClickListener(v -> {
            mediaPlayer.stop();
            status.setText("Stopped");
        });

        controls.addView(play, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        controls.addView(pause, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        controls.addView(stop, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(controls);
        setContentView(root);
    }

    private void openVideo() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_VIDEO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_VIDEO && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) return;

            try {
                getContentResolver().takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) {
                // The provider may not support persistable permissions.
            }
            playVideo(uri);
        }
    }

    private void playVideo(Uri uri) {
        Media media = null;
        try {
            media = new Media(libVLC, uri);
            mediaPlayer.setMedia(media);
            mediaPlayer.play();
            status.setText("Playing video");
        } catch (RuntimeException e) {
            Toast.makeText(this, "Unable to play video: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        } finally {
            if (media != null) media.release();
        }
    }

    @Override
    protected void onStop() {
        if (mediaPlayer != null) mediaPlayer.pause();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.detachViews();
            mediaPlayer.release();
        }
        if (libVLC != null) libVLC.release();
        super.onDestroy();
    }
}
