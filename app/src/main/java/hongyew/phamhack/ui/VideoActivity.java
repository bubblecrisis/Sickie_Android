package hongyew.phamhack.ui;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.twilio.video.AudioTrack;
import com.twilio.video.CameraCapturer;
import com.twilio.video.CameraCapturer.CameraSource;
import com.twilio.video.ConnectOptions;
import com.twilio.video.LocalAudioTrack;
import com.twilio.video.LocalMedia;
import com.twilio.video.LocalVideoTrack;
import com.twilio.video.Media;
import com.twilio.video.Participant;
import com.twilio.video.Room;
import com.twilio.video.RoomState;
import com.twilio.video.TwilioException;
import com.twilio.video.VideoClient;
import com.twilio.video.VideoRenderer;
import com.twilio.video.VideoTrack;
import com.twilio.video.VideoView;

import org.android1liner.ui.DialogUtils;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.App;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.math.BigDecimal;
import java.util.Map;

import hongyew.phamhack.AppPreference_;
import hongyew.phamhack.MainApplication;
import hongyew.phamhack.R;
import hongyew.phamhack.manager.ConferenceManager;
import hongyew.phamhack.manager.ConferenceManager_;
import hongyew.phamhack.model.BasketProduct;

@EActivity(R.layout.activity_video)
public class VideoActivity extends AppCompatActivity {
    private static final int CAMERA_MIC_PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "VideoActivity";
    
    /*
     * You must provide a Twilio Access Token to connect to the Video service
     */
    private static final String TWILIO_ACCESS_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImN0eSI6InR3aWxpby1mcGE7dj0xIn0.eyJqdGkiOiJTSzkxMTQ4ZmFkYjYxMzVkYzZmZTEyYTNjOWRmN2VjYmRiLTE0ODgyNzI4MzgiLCJpc3MiOiJTSzkxMTQ4ZmFkYjYxMzVkYzZmZTEyYTNjOWRmN2VjYmRiIiwic3ViIjoiQUNiNDcxOTM4MWM2NjcwOTJjMzM2N2E5MDgyYTg0OTMyYyIsImV4cCI6MTQ4ODI3NjQzOCwiZ3JhbnRzIjp7ImlkZW50aXR5IjoicGhhbWFoYWNrX2NsaWVudCIsInJ0YyI6eyJjb25maWd1cmF0aW9uX3Byb2ZpbGVfc2lkIjoiVlM3OTgwM2NkZmY4NWU3NDA2MjdmMmE2MzNiZjE1M2Q3MiJ9fX0.k0gVUHGtf12c6I29UnyiTTl_einbZ2Lzqdjqf_Je9OI";
    
    /*
     * The Video Client allows a client to connect to a room
     */
    private VideoClient videoClient;
    
    /*
     * A Room represents communication between the client and one or more participants.
     */
    private Room room;
    
    /*
     * A VideoView receives frames from a local or remote video track and renders them
     * to an associated view.
     */
    private VideoView primaryVideoView;
    private VideoView thumbnailVideoView;
    
    /*
     * Android application UI elements
     */
    private TextView videoStatusTextView;
    private CameraCapturer cameraCapturer;
    private LocalMedia localMedia;
    private LocalAudioTrack localAudioTrack;
    private LocalVideoTrack localVideoTrack;
    private FloatingActionButton connectActionFab;
    private FloatingActionButton switchCameraActionFab;
    private FloatingActionButton localVideoActionFab;
    private FloatingActionButton muteActionFab;
    private android.support.v7.app.AlertDialog alertDialog;
    private AudioManager audioManager;
    private String participantIdentity;
    
    private int previousAudioMode;
    private VideoRenderer localVideoView;
    private boolean disconnectedFromOnDestroy;
    
    @Extra
    public String twillioToken;
    
    @App
    MainApplication application;
    
    @Bean
    ConferenceManager conferenceManager;
    
    @ViewById(R.id.basket_list)
    RecyclerView basketView;
    
    @ViewById(R.id.total_value)
    TextView totalView;
    
    @Pref
    AppPreference_ pref;
    
    FirebaseRecyclerAdapter<BasketProduct, BasketProductViewHolder> adapter;
    
    @AfterViews
    void init() {
        
        primaryVideoView = (VideoView) findViewById(R.id.primary_video_view);
        thumbnailVideoView = (VideoView) findViewById(R.id.thumbnail_video_view);
        videoStatusTextView = (TextView) findViewById(R.id.video_status_textview);
        
        connectActionFab = (FloatingActionButton) findViewById(R.id.connect_action_fab);
        switchCameraActionFab = (FloatingActionButton) findViewById(R.id.switch_camera_action_fab);
        localVideoActionFab = (FloatingActionButton) findViewById(R.id.local_video_action_fab);
        muteActionFab = (FloatingActionButton) findViewById(R.id.mute_action_fab);

        /*
         * Enable changing the volume using the up/down keys during a conversation
         */
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        /*
         * Needed for setting/abandoning audio focus during call
         */
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        /*
         * Check camera and microphone permissions. Needed in Android M.
         */
        if (!checkPermissionForCameraAndMicrophone()) {
            requestPermissionForCameraAndMicrophone();
        } else {
            createLocalMedia();
            createVideoClient();
        }

        /*
         * Set the initial state of the UI
         */
        intializeUI();
        
        if (pref.appointmentKey().get() != null) {
            String roomKey = pref.appointmentKey().get();
            loadBasket(roomKey);
            connectToRoom(pref.appointmentKey().get());
        }
        else {
            DialogUtils.alert(this, "No appointment", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            });
        }
    }
    
    void loadBasket(String roomName) {
        Query q = conferenceManager.basketRef(roomName);
        adapter = new FirebaseRecyclerAdapter<BasketProduct, BasketProductViewHolder>(BasketProduct.class, R.layout.basket_item, BasketProductViewHolder.class, q.getRef()) {
            protected void populateViewHolder(BasketProductViewHolder viewHolder, BasketProduct model, int position) {
                viewHolder.nameView.setText(model.name);
                viewHolder.descriptionView.setText(model.symptoms);
                viewHolder.priceView.setText("$" + new BigDecimal(model.price).setScale(2));
                viewHolder.quantityView.setText((model.quantity == null)? null: model.quantity.toString());
                viewHolder.basketItemKey = getRef(position).getKey();
                viewHolder.check(model.buy != null && model.buy.equalsIgnoreCase("true"));
                calculateTotal();
            }
        };
        basketView.setLayoutManager(new LinearLayoutManager(this));
        basketView.setAdapter(adapter);
    }
    
    void calculateTotal() {
        int itemCount = adapter.getItemCount();
        double total = 0;
        for (int i = 0; i<itemCount; i++){
            BasketProduct product = adapter.getItem(i);
            if (product.buy != null && Boolean.TRUE.toString().equalsIgnoreCase(product.buy.toString())) {
                total+= product.price;
            }
        }
        totalView.setText("$" + new BigDecimal(total).setScale(2));
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == CAMERA_MIC_PERMISSION_REQUEST_CODE) {
            boolean cameraAndMicPermissionGranted = true;
            
            for (int grantResult : grantResults) {
                cameraAndMicPermissionGranted &= grantResult == PackageManager.PERMISSION_GRANTED;
            }
            
            if (cameraAndMicPermissionGranted) {
                createLocalMedia();
                createVideoClient();
            } else {
                Toast.makeText(this,
                    R.string.permissions_needed,
                    Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        /*
         * If the local video track was removed when the app was put in the background, add it back.
         */
        if (localMedia != null && localVideoTrack == null) {
            localVideoTrack = localMedia.addVideoTrack(true, cameraCapturer);
            localVideoTrack.addRenderer(localVideoView);
        }
    }
    
    @Override
    protected void onPause() {
        /*
         * Remove the local video track before going in the background. This ensures that the
         * camera can be used by other applications while this app is in the background.
         *
         * If this local video track is being shared in a Room, participants will be notified
         * that the track has been removed.
         */
        if (localMedia != null && localVideoTrack != null) {
            localMedia.removeVideoTrack(localVideoTrack);
            localVideoTrack = null;
        }
        super.onPause();
    }
    
    @Override
    protected void onDestroy() {
        /*
         * Always disconnect from the room before leaving the Activity to
         * ensure any memory allocated to the Room resource is freed.
         */
        if (room != null && room.getState() != RoomState.DISCONNECTED) {
            room.disconnect();
            disconnectedFromOnDestroy = true;
        }

        /*
         * Release the local media ensuring any memory allocated to audio or video is freed.
         */
        if (localMedia != null) {
            localMedia.release();
            localMedia = null;
        }
        
        super.onDestroy();
    }
    
    private boolean checkPermissionForCameraAndMicrophone() {
        int resultCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int resultMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        return resultCamera == PackageManager.PERMISSION_GRANTED &&
                   resultMic == PackageManager.PERMISSION_GRANTED;
    }
    
    private void requestPermissionForCameraAndMicrophone() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
            Toast.makeText(this,
                R.string.permissions_needed,
                Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                CAMERA_MIC_PERMISSION_REQUEST_CODE);
        }
    }
    
    private void createLocalMedia() {
        localMedia = LocalMedia.create(this);
        
        // Share your microphone
        localAudioTrack = localMedia.addAudioTrack(true);
        
        // Share your camera
        cameraCapturer = new CameraCapturer(this, CameraSource.FRONT_CAMERA);
        localVideoTrack = localMedia.addVideoTrack(true, cameraCapturer);
        primaryVideoView.setMirror(true);
        localVideoTrack.addRenderer(primaryVideoView);
        localVideoView = primaryVideoView;
    }
    
    private void createVideoClient() {
        /*
         * Create a VideoClient allowing you to connect to a Room
         */
        
        // OPTION 1- Generate an access token from the getting started portal
        // https://www.twilio.com/console/video/dev-tools/testing-tools
    
       // twillioToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImN0eSI6InR3aWxpby1mcGE7dj0xIn0.eyJqdGkiOiJTSzkxMTQ4ZmFkYjYxMzVkYzZmZTEyYTNjOWRmN2VjYmRiLTE0ODg1OTY4NTQiLCJpc3MiOiJTSzkxMTQ4ZmFkYjYxMzVkYzZmZTEyYTNjOWRmN2VjYmRiIiwic3ViIjoiQUNiNDcxOTM4MWM2NjcwOTJjMzM2N2E5MDgyYTg0OTMyYyIsImV4cCI6MTQ4ODYwMDQ1NCwiZ3JhbnRzIjp7ImlkZW50aXR5IjoiSG9uZyIsInJ0YyI6eyJjb25maWd1cmF0aW9uX3Byb2ZpbGVfc2lkIjoiVlM3OTgwM2NkZmY4NWU3NDA2MjdmMmE2MzNiZjE1M2Q3MiJ9fX0.pF3KwuQo-61pBP_v5RxcnqLDDADBCjNJwtHFF8oDX5M";
    
        videoClient = new VideoClient(VideoActivity.this, twillioToken);
        
        // OPTION 2- Retrieve an access token from your own web app
        // retrieveAccessTokenfromServer();
    }
    
    private void connectToRoom(String roomName) {
        try {
            setAudioFocus(true);
            ConnectOptions connectOptions = new ConnectOptions.Builder()
                                                .roomName(roomName)
                                                .localMedia(localMedia)
                                                .build();
            room = videoClient.connect(connectOptions, roomListener());
            setDisconnectAction();
        }
        catch (Exception e) {
            Log.e(VideoActivity.class.getSimpleName(), e.toString(), e);
            finish();
        }
    }
    
    /*
     * The initial state when there is no active conversation.
     */
    private void intializeUI() {
        connectActionFab.setImageDrawable(ContextCompat.getDrawable(this,
            R.drawable.ic_call_white_24px));
        connectActionFab.setOnClickListener(connectActionClickListener());
        switchCameraActionFab.show();
        switchCameraActionFab.setOnClickListener(switchCameraClickListener());
        localVideoActionFab.show();
        localVideoActionFab.setOnClickListener(localVideoClickListener());
        muteActionFab.show();
        muteActionFab.setOnClickListener(muteClickListener());
    }
    
    /*
     * The actions performed during disconnect.
     */
    private void setDisconnectAction() {
        connectActionFab.setImageDrawable(ContextCompat.getDrawable(this,
            R.drawable.ic_call_end_white_24px));
        connectActionFab.show();
        connectActionFab.setOnClickListener(disconnectClickListener());
    }
    
    /*
     * Creates an connect UI dialog
     */
    private void showConnectDialog() {
        EditText roomEditText = new EditText(this);
        alertDialog = Dialog.createConnectDialog(roomEditText,
            connectClickListener(roomEditText), cancelConnectDialogClickListener(), this);
        alertDialog.show();
    }
    
    /*
     * Called when participant joins the room
     */
    private void addParticipant(Participant participant) {
        /*
         * This app only displays video for one additional participant per Room
         */
        if (thumbnailVideoView.getVisibility() == View.VISIBLE) {
            Snackbar.make(connectActionFab,
                "Multiple participants are not currently support in this UI",
                Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
            return;
        }
        participantIdentity = participant.getIdentity();
        videoStatusTextView.setText("Participant " + participantIdentity + " joined");

        /*
         * Add participant renderer
         */
        if (participant.getMedia().getVideoTracks().size() > 0) {
            addParticipantVideo(participant.getMedia().getVideoTracks().get(0));
        }

        /*
         * Start listening for participant media events
         */
        participant.getMedia().setListener(mediaListener());
    }
    
    /*
     * Set primary view as renderer for participant video track
     */
    private void addParticipantVideo(VideoTrack videoTrack) {
        moveLocalVideoToThumbnailView();
        primaryVideoView.setMirror(false);
        videoTrack.addRenderer(primaryVideoView);
    }
    
    private void moveLocalVideoToThumbnailView() {
        if (thumbnailVideoView.getVisibility() == View.GONE) {
            thumbnailVideoView.setVisibility(View.VISIBLE);
            localVideoTrack.removeRenderer(primaryVideoView);
            localVideoTrack.addRenderer(thumbnailVideoView);
            localVideoView = thumbnailVideoView;
            thumbnailVideoView.setMirror(cameraCapturer.getCameraSource() ==
                                             CameraSource.FRONT_CAMERA);
        }
    }
    
    /*
     * Called when participant leaves the room
     */
    private void removeParticipant(Participant participant) {
        videoStatusTextView.setText("Participant " + participant.getIdentity() + " left.");
        if (!participant.getIdentity().equals(participantIdentity)) {
            return;
        }

        /*
         * Remove participant renderer
         */
        if (participant.getMedia().getVideoTracks().size() > 0) {
            removeParticipantVideo(participant.getMedia().getVideoTracks().get(0));
        }
        participant.getMedia().setListener(null);
        moveLocalVideoToPrimaryView();
    }
    
    private void removeParticipantVideo(VideoTrack videoTrack) {
        videoTrack.removeRenderer(primaryVideoView);
    }
    
    private void moveLocalVideoToPrimaryView() {
        if (thumbnailVideoView.getVisibility() == View.VISIBLE) {
            localVideoTrack.removeRenderer(thumbnailVideoView);
            thumbnailVideoView.setVisibility(View.GONE);
            localVideoTrack.addRenderer(primaryVideoView);
            localVideoView = primaryVideoView;
            primaryVideoView.setMirror(cameraCapturer.getCameraSource() ==
                                           CameraSource.FRONT_CAMERA);
        }
    }
    
    /*
     * Room events listener
     */
    private Room.Listener roomListener() {
        return new Room.Listener() {
            @Override
            public void onConnected(Room room) {
                videoStatusTextView.setText("Connected to " + room.getName());
                setTitle(room.getName());
                
                for (Map.Entry<String, Participant> entry : room.getParticipants().entrySet()) {
                    addParticipant(entry.getValue());
                    break;
                }
            }
            
            @Override
            public void onConnectFailure(Room room, TwilioException e) {
                videoStatusTextView.setText("Failed to connect");
            }
            
            @Override
            public void onDisconnected(Room room, TwilioException e) {
                videoStatusTextView.setText("Disconnected from " + room.getName());
                VideoActivity.this.room = null;
                // Only reinitialize the UI if disconnect was not called from onDestroy()
                if (!disconnectedFromOnDestroy) {
                    setAudioFocus(false);
                    intializeUI();
                    moveLocalVideoToPrimaryView();
                }
            }
            
            @Override
            public void onParticipantConnected(Room room, Participant participant) {
                addParticipant(participant);
                
            }
            
            @Override
            public void onParticipantDisconnected(Room room, Participant participant) {
                removeParticipant(participant);
            }
            
            @Override
            public void onRecordingStarted(Room room) {
                /*
                 * Indicates when media shared to a Room is being recorded. Note that
                 * recording is only available in our Group Rooms developer preview.
                 */
                Log.d(TAG, "onRecordingStarted");
            }
            
            @Override
            public void onRecordingStopped(Room room) {
                /*
                 * Indicates when media shared to a Room is no longer being recorded. Note that
                 * recording is only available in our Group Rooms developer preview.
                 */
                Log.d(TAG, "onRecordingStopped");
            }
        };
    }
    
    private Media.Listener mediaListener() {
        return new Media.Listener() {
            
            @Override
            public void onAudioTrackAdded(Media media, AudioTrack audioTrack) {
                //videoStatusTextView.setText("onAudioTrackAdded");
            }
            
            @Override
            public void onAudioTrackRemoved(Media media, AudioTrack audioTrack) {
                //videoStatusTextView.setText("onAudioTrackRemoved");
            }
            
            @Override
            public void onVideoTrackAdded(Media media, VideoTrack videoTrack) {
                //videoStatusTextView.setText("onVideoTrackAdded");
                addParticipantVideo(videoTrack);
            }
            
            @Override
            public void onVideoTrackRemoved(Media media, VideoTrack videoTrack) {
                //videoStatusTextView.setText("onVideoTrackRemoved");
                removeParticipantVideo(videoTrack);
            }
            
            @Override
            public void onAudioTrackEnabled(Media media, AudioTrack audioTrack) {
                
            }
            
            @Override
            public void onAudioTrackDisabled(Media media, AudioTrack audioTrack) {
                
            }
            
            @Override
            public void onVideoTrackEnabled(Media media, VideoTrack videoTrack) {
                
            }
            
            @Override
            public void onVideoTrackDisabled(Media media, VideoTrack videoTrack) {
                
            }
        };
    }
    
    private DialogInterface.OnClickListener connectClickListener(final EditText roomEditText) {
        return new DialogInterface.OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
                /*
                 * Connect to room
                 */
                connectToRoom(roomEditText.getText().toString());
            }
        };
    }
    
    private View.OnClickListener disconnectClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * Disconnect from room
                 */
                if (room != null) {
                    room.disconnect();
                }
                intializeUI();
            }
        };
    }
    
    private View.OnClickListener connectActionClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showConnectDialog();
            }
        };
    }
    
    private DialogInterface.OnClickListener cancelConnectDialogClickListener() {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                intializeUI();
                alertDialog.dismiss();
            }
        };
    }
    
    private View.OnClickListener switchCameraClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cameraCapturer != null) {
                    CameraSource cameraSource = cameraCapturer.getCameraSource();
                    cameraCapturer.switchCamera();
                    if (thumbnailVideoView.getVisibility() == View.VISIBLE) {
                        thumbnailVideoView.setMirror(cameraSource == CameraSource.BACK_CAMERA);
                    } else {
                        primaryVideoView.setMirror(cameraSource == CameraSource.BACK_CAMERA);
                    }
                }
            }
        };
    }
    
    private View.OnClickListener localVideoClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * Enable/disable the local video track
                 */
                if (localVideoTrack != null) {
                    boolean enable = !localVideoTrack.isEnabled();
                    localVideoTrack.enable(enable);
                    int icon;
                    if (enable) {
                        icon = R.drawable.ic_videocam_green_24px;
                        switchCameraActionFab.show();
                    } else {
                        icon = R.drawable.ic_videocam_off_red_24px;
                        switchCameraActionFab.hide();
                    }
                    localVideoActionFab.setImageDrawable(
                        ContextCompat.getDrawable(VideoActivity.this, icon));
                }
            }
        };
    }
    
    private View.OnClickListener muteClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * Enable/disable the local audio track. The results of this operation are
                 * signaled to other Participants in the same Room. When an audio track is
                 * disabled, the audio is muted.
                 */
                if (localAudioTrack != null) {
                    boolean enable = !localAudioTrack.isEnabled();
                    localAudioTrack.enable(enable);
                    int icon = enable ?
                                   R.drawable.ic_mic_green_24px : R.drawable.ic_mic_off_red_24px;
                    muteActionFab.setImageDrawable(ContextCompat.getDrawable(
                        VideoActivity.this, icon));
                }
            }
        };
    }
    
    private void retrieveAccessTokenfromServer() {
        Ion.with(this)
            .load("http://localhost:8000/token.php")
            .asJsonObject()
            .setCallback(new FutureCallback<JsonObject>() {
                @Override
                public void onCompleted(Exception e, JsonObject result) {
                    if (e == null) {
                        String accessToken = result.get("token").getAsString();
                        
                        videoClient = new VideoClient(VideoActivity.this, accessToken);
                    } else {
                        Toast.makeText(VideoActivity.this,
                            R.string.error_retrieving_access_token, Toast.LENGTH_LONG)
                            .show();
                    }
                }
            });
    }
    
    private void setAudioFocus(boolean focus) {
        if (focus) {
            previousAudioMode = audioManager.getMode();
            // Request audio focus before making any device switch.
            audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            /*
             * Use MODE_IN_COMMUNICATION as the default audio mode. It is required
             * to be in this mode when playout and/or recording starts for the best
             * possible VoIP performance. Some devices have difficulties with
             * speaker mode if this is not set.
             */
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        } else {
            audioManager.setMode(previousAudioMode);
            audioManager.abandonAudioFocus(null);
        }
    }
    
    @Click(R.id.checkout_button)
    public void checkout() {
        BuyCompleteActivity_.intent(this)
            .total(totalView.getText().toString())
            .appointmentKey(pref.appointmentKey().get())
            .start();
        finish();
    }
    
    public static class BasketProductViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;
        public TextView nameView;
        public TextView descriptionView;
        public TextView quantityView;
        public TextView priceView;
        public AppCompatCheckBox checkbox;
        public String basketItemKey;
        private CompoundButton.OnCheckedChangeListener checkListener;
        
        public BasketProductViewHolder(final View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.basket_item_image);
            nameView = (TextView) itemView.findViewById(R.id.basket_item_name);
            descriptionView = (TextView) itemView.findViewById(R.id.basket_item_description);
            quantityView = (TextView) itemView.findViewById(R.id.basket_item_quantity);
            priceView = (TextView) itemView.findViewById(R.id.basket_item_price);
            checkbox = (AppCompatCheckBox) itemView.findViewById(R.id.basket_checkbox);
    
            checkListener = new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    AppPreference_ pref = new AppPreference_(itemView.getContext());
                    ConferenceManager conferenceManager = ConferenceManager_.getInstance_(itemView.getContext());
                    String room = pref.appointmentKey().get();
                    if (room != null) {
                        DatabaseReference ref = conferenceManager.basketRef(room).child(basketItemKey);
                        if (b) {
                            ref.child("buy").setValue("true");
                        }
                        else {
                            ref.child("buy").setValue("false");
                        }
                    }
                }
            };
    
            checkbox.setOnCheckedChangeListener(checkListener);
        }
        
        public void check(boolean b) {
            checkbox.setOnCheckedChangeListener(null);
            checkbox.setChecked(b);
            checkbox.setOnCheckedChangeListener(checkListener);
        }
    }
}
