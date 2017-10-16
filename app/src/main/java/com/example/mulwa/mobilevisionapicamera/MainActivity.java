package com.example.mulwa.mobilevisionapicamera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.app.AlertDialog;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;

import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mulwa.mobilevisionapicamera.camera.CameraSource;
import com.example.mulwa.mobilevisionapicamera.camera.CameraSourcePreview;
import com.example.mulwa.mobilevisionapicamera.camera.GraphicOverlay;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.RuntimeExecutionException;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "MainActivity";
    private EditText m_display;
    private final static int RequestCameraPermissionId = 1001;
    private final static int MAKE_CALL_PERMISSION_REQUEST_CODE = 1002;
    // Intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;
    private TextView  m_provider;
    private RelativeLayout relativeLayout;
    private RelativeLayout m_flash_layout;
    private Button m_top_up, m_check_balance, m_clear;


    private CameraSourcePreview mPreview;
    private CameraSource mCameraSource;
    private GraphicOverlay<OcrGraphic> mGraphicOverlay;
    private boolean autoFocus = true;
    private boolean useFlash = false;
    private Switch m_flash_switch;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        m_display = (EditText) findViewById(R.id.tv_display);
        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay<OcrGraphic>) findViewById(R.id.graphicOverlay);
        m_provider = (TextView)  findViewById(R.id.tv_provider);
        relativeLayout= (RelativeLayout) findViewById(R.id.viewLayout);
        m_flash_layout = (RelativeLayout) findViewById(R.id.flash_layout);

        m_top_up = (Button) findViewById(R.id.btn_topUp);
        m_check_balance = (Button) findViewById(R.id.btn_check_balance);
        m_clear = (Button) findViewById(R.id.btn_clear);
        m_flash_switch =(Switch)findViewById(R.id.flash_switch);

        m_top_up.setOnClickListener(this);
        m_check_balance.setOnClickListener(this);
        m_clear.setOnClickListener(this);



        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(autoFocus, useFlash);
        } else {
            requestCameraPermission();
        }


        if(hasFlashSupport()){
            m_flash_layout.setVisibility(View.VISIBLE);
        }else {
            m_flash_layout.setVisibility(View.INVISIBLE);
        }

        m_provider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popProvider();
            }
        });

        m_flash_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean ischecked) {
                if(ischecked){
                    mCameraSource.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);

                }else {
                    mCameraSource.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);

                }
            }
        });
        if(checkPermission(Manifest.permission.CALL_PHONE)){
            m_top_up.setEnabled(true);
            m_check_balance.setEnabled(true);

        }else {
            m_top_up.setEnabled(false);
            m_check_balance.setEnabled(false);
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CALL_PHONE},MAKE_CALL_PERMISSION_REQUEST_CODE);

        }



    }
    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the ocr detector to detect small text samples
     * at long distances.
     *
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @SuppressLint("InlinedApi")
    private void createCameraSource(boolean autoFocus, boolean useFlash) {
        Context context = getApplicationContext();

        // A text recognizer is created to find text.  An associated multi-processor instance
        // is set to receive the text recognition results, track the text, and maintain
        // graphics for each text block on screen.  The factory is used by the multi-processor to
        // create a separate tracker instance for each text block.
        TextRecognizer textRecognizer = new TextRecognizer.Builder(context).build();
//        textRecognizer.setProcessor(new OcrDetectorProcessor(mGraphicOverlay,m_display));
        textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<TextBlock> detections) {
                final SparseArray<TextBlock> items = detections.getDetectedItems();
                if(items.size() != 0){
                    m_display.post(new Runnable() {
                        @Override
                        public void run() {
                            for(int i =0; i<items.size();i++){
                                TextBlock item = items.valueAt(i);
                                for (Text  line: item.getComponents()){
                                    String stringT = line.getValue().replace(" ","");
                                    if( stringT.matches("[0-9]+")  && stringT.length() ==14){
                                        m_display.setText(stringT);
                                    }
                                }


                            }
//                                this was displaying a block of text
//                                m_display.setText(stringBuilder.toString());

                        }
                    });
                }


            }
        });

        if (!textRecognizer.isOperational()) {
            // Note: The first time that an app using a Vision API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any text,
            // barcodes, or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Log.w(TAG, "Detector dependencies are not yet available.");
            longSnackBar("Detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
//                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                longSnackBar( getString( R.string.low_storage_error));
                Log.w(TAG, getString(R.string.low_storage_error));
            }
        }

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the text recognizer to detect small pieces of text.
        mCameraSource =
                new CameraSource.Builder(getApplicationContext(), textRecognizer)
                        .setFacing(CameraSource.CAMERA_FACING_BACK)
                        .setRequestedPreviewSize(1280, 1024)
                        .setRequestedFps(2.0f)
                        .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                        .setFocusMode(autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null)
                        .build();
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onStop() {
        super.onStop();
        // on stop release the camera

    }

    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mPreview != null){
            mPreview.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mPreview !=null){
            mPreview.release();
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id){
            case R.id.btn_topUp:
                if(validateProvide() && validateCode()){
                    loadCredit(m_provider.getText().toString(),m_display.getText().toString().trim());

                }
                break;
            case R.id.btn_check_balance:
                if(validateProvide()){
                    checkBalance(m_provider.getText().toString());
                }

                break;
            case R.id.btn_clear:
                clearUi();
                break;
            default:
                break;
        }

    }
    private void showToast(String msg){
        Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_LONG).show();
    }
    private void popProvider(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        final String[] category = getResources().getStringArray(R.array.carriers);
        alertDialogBuilder.setTitle("Select Provider");
        alertDialogBuilder.setItems(R.array.carriers, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String selectedProvider = category[i];
                if(selectedProvider != null){
                    m_provider.setText(selectedProvider);

                }

            }
        });
        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
//               what to execute  when user selects cancel button

            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();


    }
    private void checkBalance(String provider){
        int code = 141 ;
        switch (provider){
            case "Safaricom":
                code = 144;
                break;
            case "Airtel":
                code = 131;
                break;
            case "Orange":
                code = 131;
                break;
            case "Yu":
                code =131;
                break;
            case "Equitel":
                code = 131;
                break;
            default:
                break;
        }
//        String dial = "tel:" + code;
        String dial = "tel:" +Uri.encode("*"+String.valueOf(code)+"#");
        Log.d("dial",dial);
       if(checkPermission(Manifest.permission.CALL_PHONE)){
           startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
       }else {
           showToast("Grant call permission");
       }
    }
    private void loadCredit(String provider,String pin){
        int code ;
        switch (provider){
            case "Safaricom":
                code = 141;
                break;
            case "Airtel":
                code = 130;
                break;
            case "Orange":
                code = 132;
                break;
            case "Yu":
                code =131;
                break;
            case "Equitel":
                code = 131;
                break;
            default:
                code = 141;
                break;
        }
//        String dial = "tel:" + code;
        String dial = "tel:" +Uri.encode("*"+String.valueOf(code)+"*"+pin+"#");
        Log.d("topup",dial);
        if(checkPermission(Manifest.permission.CALL_PHONE)){
            startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
        }else {
//            showToast("Grant call permission");
            shortSnackBar("Grant call permission");
        }
    }
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RequestCameraPermissionId);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RequestCameraPermissionId);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    private void clearUi(){
        if(!TextUtils.isEmpty(m_display.getText().toString())){
            m_display.setText("");
        }

    }
    private boolean  checkPermission(String permission){
        return ContextCompat.checkSelfPermission(this, permission)== PackageManager.PERMISSION_GRANTED;
    }
    private boolean hasFlashSupport(){
        return getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }
    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() throws SecurityException {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }



    private boolean validateProvide(){
        String m_pervider = m_provider.getText().toString().trim();
        if(m_pervider.contains("Select Provider")){
//            showToast("Select Your Service Provider");
            shortSnackBar("Select Your Service Provider");
            return false;
        }
        return true;
    }
    private boolean validateCode(){
        if(TextUtils.isEmpty(m_display.getText().toString())){
//            showToast("Please Scan Credit code");
            shortSnackBar("Please Scan Credit code");
            return false;

        }
        return true;
    }
    private void longSnackBar(String msg){
        Snackbar snackbar = Snackbar
                .make(relativeLayout, msg, Snackbar.LENGTH_INDEFINITE)
                .setAction("Ok", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                    }
                });

// Changing message text color
        snackbar.setActionTextColor(Color.RED);

// Changing action button text color
        View sbView = snackbar.getView();
        TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(Color.YELLOW);
        snackbar.show();

    }
    private void shortSnackBar(String msg){
        Snackbar snackbar = Snackbar
                .make(relativeLayout, msg, Snackbar.LENGTH_LONG);

        snackbar.show();

    }


}
