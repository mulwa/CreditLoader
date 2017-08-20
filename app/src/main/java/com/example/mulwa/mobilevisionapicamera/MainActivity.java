package com.example.mulwa.mobilevisionapicamera;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.app.AlertDialog;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private SurfaceView m_surface_view;
    private EditText m_display;
    private CameraSource cameraSource;
    private final static int RequestCameraPermissionId = 1001;
    private final static int MAKE_CALL_PERMISSION_REQUEST_CODE = 1002;
    private TextView  m_provider;
    private RelativeLayout relativeLayout;
    private Button m_top_up, m_check_balance, m_clear;
    private Switch m_flash_switch;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case RequestCameraPermissionId:
            {
                if(grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                        return;
                    }
                    try {
                        cameraSource.start(m_surface_view.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        m_display = (EditText) findViewById(R.id.tv_display);
        m_surface_view = (SurfaceView) findViewById(R.id.surface_view);
        m_provider = (TextView)  findViewById(R.id.tv_provider);
        relativeLayout= (RelativeLayout) findViewById(R.id.viewLayout);

        m_top_up = (Button) findViewById(R.id.btn_topUp);
        m_check_balance = (Button) findViewById(R.id.btn_check_balance);
        m_clear = (Button) findViewById(R.id.btn_clear);
        m_flash_switch =(Switch)findViewById(R.id.flash_switch);

        m_top_up.setOnClickListener(this);
        m_check_balance.setOnClickListener(this);
        m_clear.setOnClickListener(this);

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
                    showToast("turn Flashlight on");
                }else {
                    showToast("turn flashlight off");
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


        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if (!textRecognizer.isOperational()) {
            showToast("Detector Dependencies not ready for now");
        } else {
            cameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    .setRequestedFps(2.0f)
                    .setAutoFocusEnabled(true)
                    .build();
            m_surface_view.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder surfaceHolder) {
                    try {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.CAMERA},
                                    RequestCameraPermissionId);

                            return;
                        }
                        cameraSource.start(m_surface_view.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

                }

                @Override
                public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                    cameraSource.stop();

                }
            });

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
                                String lines="";
                                for(int i =0; i<items.size();i++){
                                    TextBlock item = items.valueAt(i);
                                    for (Text  line: item.getComponents()){
                                        lines = line.getValue().trim();
                                    }
                                    m_display.setText(lines);

                                }
//                                this was displaying a block of text
//                                m_display.setText(stringBuilder.toString());

                            }
                        });
                    }


                }
            });


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
            showToast("Grant call permission");
        }
    }

    private void clearUi(){
        if(!TextUtils.isEmpty(m_display.getText().toString())){
            m_display.setText("");
        }

    }
    private boolean  checkPermission(String permission){
        return ContextCompat.checkSelfPermission(this, permission)== PackageManager.PERMISSION_GRANTED;
    }
    private boolean validateProvide(){
        String m_pervider = m_provider.getText().toString().trim();
        if(m_pervider.contains("Select Provider")){
            showToast("Select Your Service Provider");
            return false;
        }
        return true;
    }
    private boolean validateCode(){
        if(TextUtils.isEmpty(m_display.getText().toString())){
            showToast("Please Scan Credit code");
            return false;

        }
        return true;
    }

}
