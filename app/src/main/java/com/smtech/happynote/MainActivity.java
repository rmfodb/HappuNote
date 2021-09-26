package com.smtech.happynote;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.DialogPreference;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    FirebaseRemoteConfig mFirebaseRemoteConfig;
    long newAppVersion   = 0;
    long toolbarImgCount = 0;

    List<File> toolbarImageList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Firebase using
        getRemoteConfig();

    }

    private void getRemoteConfig() {

        //Firebase using
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);

        //서버에 있는 데이타 받아오기
        mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(this, new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {

                        newAppVersion   = mFirebaseRemoteConfig.getLong("new_app_version");
                        toolbarImgCount = mFirebaseRemoteConfig.getLong("toolbar_img_count");
                        //버전확인
                        checkVersion();
                    }
                });

    }

    private void checkVersion() {


        //FireBase에 정의된 변수의 내용을 가져와서 버전을 확인하는 내용
        try {

            //패키지 확인
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);

            //설치 버전을 비교
            long appVersion;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                appVersion = pi.getLongVersionCode();
            } else {
                appVersion = pi.versionCode;
            }

            //버전이 변경이 되었으면
            if (appVersion < newAppVersion) {
                //다이얼로그 창을 만들고 디스플레이 한다.
                updateDialog();
                return;
            }

            //Ctrl+Alt+Shift+S키를 누르면 Project Struct화면 Open
            //Moudles -> Default Config -> Version, Version Name을 변경할 수 있다.


            //2021-09-26 툴바 이미지 다운로드 메소드 정의
            checkToolbarImages();



        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        toolbarImageList.clear();
        toolbarImageList = null;
    }

    private void checkToolbarImages() {
        //디렉토링에 이미지 존재여부를 확인
        File file = getExternalFilesDir(Environment.DIRECTORY_PICTURES + "/toobar_images");
        if (!file.isDirectory()) {
            //디렉토리 생성
            file.mkdir();
        }

        toolbarImageList.addAll(new ArrayList<>(Arrays.asList(file.listFiles())));

        if (toolbarImageList.size() < toolbarImgCount) {
            //파일을 다운로드
            FirebaseStorage  storage            = FirebaseStorage.getInstance();
            StorageReference storageReference   = storage.getReference();

            downloadToolbarImg(storageReference);

        }

    }

    private void downloadToolbarImg(StorageReference storageReference) {

        if (toolbarImageList == null || toolbarImageList.size() >= toolbarImgCount) {
            return;
        }

        String fileName     = "toolbar_" + toolbarImageList.size() + ".jpg";
        File   fileDir      = getExternalFilesDir(Environment.DIRECTORY_PICTURES + "/toobar_images");
        File   downloadFile = new File(fileDir, fileName);

        //스토리지에 있는 이미지 파일을 설정
        StorageReference downloadRef = storageReference.child("toolbar_images/" + "toolbar_" + toolbarImageList.size() + ".jpg");

        downloadRef.getFile(downloadFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                toolbarImageList.add(downloadFile);
                if (toolbarImageList.size() < toolbarImgCount) {

                    //재귀호출처리
                    downloadToolbarImg(storageReference);
                }

                Log.e("onSuccess", downloadFile.getName());

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
            }
        });

    }

    private void updateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("업데이트 알림");
        builder.setMessage("최신버전이 등록되었습니다.\n업데이트를 하세요")
                .setCancelable(false)
                .setPositiveButton("업데이트", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Intent intent = new Intent(Intent.ACTION_VIEW);
                        //intent.setData(Uri.parse("market://details?id=com.smtech.happynote"));
                        //startActivity(intent);
                        Toast.makeText(getApplicationContext(), "업데이트 버튼클릭됨", Toast.LENGTH_SHORT).show();
                        dialog.cancel();
                    }
                });

        //다이얼로그의 디스플레이
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

}
