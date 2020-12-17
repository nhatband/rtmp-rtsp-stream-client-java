package com.pedro.rtpstreamer;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.datatransport.BuildConfig;
import com.pedro.rtpstreamer.displayexample.DisplayActivity;
import com.pedro.rtpstreamer.displayexample.DisplayService;
import com.pedro.rtpstreamer.utils.ActivityLink;
import com.pedro.rtpstreamer.utils.ImageAdapter;

import net.ossrs.rtmp.ConnectCheckerRtmp;

import java.util.ArrayList;
import java.util.List;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

public class MainActivity extends AppCompatActivity implements ConnectCheckerRtmp, View.OnClickListener{

    //  private GridView list;
//  private List<ActivityLink> activities;
    private Button button;
    private EditText etUrl;
    private final int REQUEST_CODE_STREAM = 179; //random num
    private final int REQUEST_CODE_RECORD = 180; //random num
    private NotificationManager notificationManager;
    private final String[] PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @RequiresApi(api = LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        overridePendingTransition(R.transition.slide_in, R.transition.slide_out);
        TextView tvVersion = findViewById(R.id.tv_version);
        tvVersion.setText(getString(R.string.version, BuildConfig.VERSION_NAME));
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        button = findViewById(R.id.b_start_stop);
        button.setOnClickListener(this);
        etUrl = findViewById(R.id.et_rtp_url);
        String DeviceID = Settings.Secure.getString(getBaseContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);
        etUrl.setText("rtmp://192.168.100.248:11935/live/"+DeviceID);
        getInstance();
        if (DisplayService.Companion.isStreaming()) {
            button.setText(R.string.stop_button);
        } else {
            button.setText(R.string.start_button);
        }
//    list = findViewById(R.id.list);
//    createList();
//    setListAdapter(activities);

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, 1);
        }
    }

//  private void createList() {
//    activities = new ArrayList<>();
//    activities.add(new ActivityLink(new Intent(this, DisplayActivity.class),
//        getString(R.string.display_rtmp), LOLLIPOP));
//
//  }

//  private void setListAdapter(List<ActivityLink> activities) {
//    list.setAdapter(new ImageAdapter(activities));
//    list.setOnItemClickListener(this);
//  }

//  @Override
//  public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//    if (hasPermissions(this, PERMISSIONS)) {
//      ActivityLink link = activities.get(i);
//      int minSdk = link.getMinSdk();
//      if (Build.VERSION.SDK_INT >= minSdk) {
//        startActivity(link.getIntent());
//        overridePendingTransition(R.transition.slide_in, R.transition.slide_out);
//      } else {
//        showMinSdkError(minSdk);
//      }
//    } else {
//      showPermissionsErrorAndRequest();
//    }
//  }
private void initNotification() {
    Notification.Builder notificationBuilder =
            new Notification.Builder(this).setSmallIcon(R.drawable.notification_anim)
                    .setContentTitle("Streaming")
                    .setContentText("Display mode stream")
                    .setTicker("Stream in progress");
    notificationBuilder.setAutoCancel(true);
    if (notificationManager != null) notificationManager.notify(12345, notificationBuilder.build());
}
    private void stopNotification() {
        if (notificationManager != null) {
            notificationManager.cancel(12345);
        }
    }

@RequiresApi(api = LOLLIPOP)
private void getInstance() {
    DisplayService.Companion.init(this);
}
    private void showMinSdkError(int minSdk) {
        String named;
        switch (minSdk) {
            case JELLY_BEAN_MR2:
                named = "JELLY_BEAN_MR2";
                break;
            case LOLLIPOP:
                named = "LOLLIPOP";
                break;
            default:
                named = "JELLY_BEAN";
                break;
        }
        Toast.makeText(this, "You need min Android " + named + " (API " + minSdk + " )",
                Toast.LENGTH_SHORT).show();
    }

    private void showPermissionsErrorAndRequest() {
        Toast.makeText(this, "You need permissions before", Toast.LENGTH_SHORT).show();
        ActivityCompat.requestPermissions(this, PERMISSIONS, 1);
    }

    private boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @RequiresApi(api = LOLLIPOP)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.b_start_stop:
                if (!DisplayService.Companion.isStreaming()) {
                    button.setText(R.string.stop_button);
                    startActivityForResult(DisplayService.Companion.sendIntent(), REQUEST_CODE_STREAM);
                } else {
                    button.setText(R.string.start_button);
                    stopService(new Intent(MainActivity.this, DisplayService.class));
                }
                if (!DisplayService.Companion.isStreaming() && !DisplayService.Companion.isRecording()) {
                    stopNotification();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onConnectionSuccessRtmp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Connection success", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onConnectionFailedRtmp(@NonNull final String reason) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Connection failed. " + reason, Toast.LENGTH_SHORT)
                        .show();
                stopNotification();
                stopService(new Intent(MainActivity.this, DisplayService.class));
                button.setText(R.string.start_button);
            }
        });
    }

    @Override
    public void onNewBitrateRtmp(long bitrate) {

    }

    @Override
    public void onDisconnectRtmp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAuthErrorRtmp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Auth error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAuthSuccessRtmp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Auth success", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @RequiresApi(api = LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && (requestCode == REQUEST_CODE_STREAM
                || requestCode == REQUEST_CODE_RECORD && resultCode == Activity.RESULT_OK)) {
            initNotification();
            DisplayService.Companion.setData(resultCode, data);
            Intent intent = new Intent(this, DisplayService.class);
            intent.putExtra("endpoint", etUrl.getText().toString());
            startService(intent);
        } else {
            Toast.makeText(this, "No permissions available", Toast.LENGTH_SHORT).show();
            button.setText(R.string.start_button);
        }
    }
}