package com.scanner_barcodeqr;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.single.PermissionListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    CameraView cameraView;
    Button btnScanAgain;
    boolean isDetected = false;

    FirebaseVisionBarcodeDetectorOptions barcodeDetectorOptions;
    FirebaseVisionBarcodeDetector firebaseVisionBarcodeDetector;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Dexter.withActivity(this).
                withPermissions(new String[]{Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO})
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {

                        setUpCamera();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {


                    }
                }).check();
    }

    private void setUpCamera() {
       btnScanAgain = findViewById(R.id.scanAgain);
       btnScanAgain.setEnabled(isDetected);
       btnScanAgain.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
              isDetected = !isDetected;
           }
       });

       cameraView = findViewById(R.id.camera_view);
       cameraView.setLifecycleOwner((LifecycleOwner) MainActivity.this);
       cameraView.addFrameProcessor(new FrameProcessor() {
           @Override
           public void process(@NonNull Frame frame) {
               processImg(getVisionImgFrame(frame));
           }
       });

       barcodeDetectorOptions = new FirebaseVisionBarcodeDetectorOptions.Builder().setBarcodeFormats(FirebaseVisionBarcode.FORMAT_QR_CODE)
               .build();
       firebaseVisionBarcodeDetector = FirebaseVision.getInstance().getVisionBarcodeDetector(barcodeDetectorOptions);
    }

    private void processImg(FirebaseVisionImage visionImgFrame) {

        if (!isDetected){
            firebaseVisionBarcodeDetector.detectInImage(visionImgFrame)
                    .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionBarcode>>() {
                        @Override
                        public void onSuccess(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {

                            processResult(firebaseVisionBarcodes);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                   Toast.makeText(MainActivity.this,""+ e.getMessage(),Toast.LENGTH_LONG).show();
                }
            });
        }
    }


    private void processResult(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {

        if (firebaseVisionBarcodes.size() > 0){
            btnScanAgain.setEnabled(isDetected);
//need to replace for loop..detects multiple times
        for (FirebaseVisionBarcode visionBarcode: firebaseVisionBarcodes){
            int value_type = visionBarcode.getValueType();

            switch (value_type){
                case FirebaseVisionBarcode.TYPE_TEXT:
                {
                    showDialog(visionBarcode.getRawValue());
                }
                break;

                case FirebaseVisionBarcode.TYPE_URL:
                {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(visionBarcode.getRawValue())));
                }
                break;

                case FirebaseVisionBarcode.TYPE_CONTACT_INFO:
                {

                    String info = new StringBuilder("Name: ")
                            .append(visionBarcode.getContactInfo().getName().getFormattedName())
                            .append("\n")
                            .append("Address: ")
                            .append(visionBarcode.getContactInfo().getAddresses().get(0).getAddressLines()[0])
                            .append("\n")
                            .append("Email: ")
                            .append(visionBarcode.getContactInfo().getEmails().get(0).getAddress())
                            .append("\n")
                            .append("Phone Number: ")
                            .append(visionBarcode.getContactInfo().getPhones().get(0).getNumber())
                            .toString();
                    showDialog(info);
                }
                break;

                case FirebaseVisionBarcode.TYPE_PRODUCT:
                {
                    String infoProduct = new StringBuilder("Name: ")
                            .append(visionBarcode.getContactInfo().getTitle())
                            .append(visionBarcode.getContactInfo().getOrganization())
                            .toString();
                    showDialog(infoProduct);
                }
                break;

                default:
                    break;
            }

        }
        }
    }

    private void showDialog(String rawValue) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(rawValue)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        dialogInterface.dismiss();;
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private FirebaseVisionImage getVisionImgFrame(Frame frame){
        byte [] data = frame.getData();
        FirebaseVisionImageMetadata metadata = new FirebaseVisionImageMetadata.Builder()
                .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                .setHeight(frame.getSize().getHeight())
                .setWidth(frame.getSize().getWidth())
                .build();
        return FirebaseVisionImage.fromByteArray(data,metadata);
    }
}
