package org.fk.miuiotafinder;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.util.Objects;

public class MainActivity extends Activity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = findViewById(android.R.id.button1);
        button.setOnClickListener(this);
        button = findViewById(android.R.id.button2);
        button.setOnClickListener(this);
    }

    private String findOTA() {
        if (hasPermissions()) {
            try {
                File folder = new File(Environment.getExternalStorageDirectory().getPath() + "/Download/downloaded_rom");
                if(!folder.exists() || !folder.isDirectory() || Objects.requireNonNull(folder.list()).length < 1) {
                    Toast.makeText(this, "OTA directory not found or empty", Toast.LENGTH_LONG).show();
                    return null;
                }
                return Objects.requireNonNull(folder.list())[0];
            } catch (Throwable t) {
                Toast.makeText(this, "An error occurred while getting OTA file", Toast.LENGTH_LONG).show();
                finish();
            }
        }
        return null;
    }

    private void scanForOTAFile() {
        String otaFile = findOTA();
        if (otaFile != null) {
            shareOTA(parseOTA(otaFile));
        }
    }

    private void deleteOTAFile() {
        String otaFile = findOTA();
        if (otaFile != null) {
            try {
                File file = new File(Environment.getExternalStorageDirectory().getPath() + "/Download/downloaded_rom/" + otaFile);
                file.delete();
                if (!file.exists())
                    Toast.makeText(this, "OTA file "+ otaFile + " deleted successfully!", Toast.LENGTH_LONG).show();
            } catch (Throwable t) {
                Toast.makeText(this, "An error occurred while deleting OTA file", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void shareOTA(OTA ota) {
        StringBuilder sb = new StringBuilder();
        sb.append("MIUI Version: ");
        sb.append(ota.version);
        sb.append("\nVariant: ");
        sb.append(ota.variant);
        sb.append("\nOTA Type: ");
        sb.append(ota.otaType);
        sb.append("\nAndroid Version: ");
        sb.append(ota.android);
        sb.append("\nLink: ");
        sb.append(ota.link);
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        sendIntent.setType("text/plain");
        Intent shareIntent = Intent.createChooser(sendIntent, null);
        startActivity(shareIntent);
    }

    /*
    https://bigota.d.miui.com/V12.5.1.0.QCNMIXM/miui-blockota-olive_global-V12.0.3.0.QCNMIXM-V12.5.1.0.QCNMIXM-1dace3xxxx-10.0.zip
    miui-blockota-olive_global-V12.0.3.0.QCNMIXM-V12.5.1.0.QCNMIXM-1dace3xxxx-10.0.zip

    https://bigota.d.miui.com/V12.5.1.0.QCNMIXM/miui_OLIVEGlobal_V12.5.1.0.QCNMIXM_ab0f9f2a68_10.0.zip
    miui_OLIVEGlobal_V12.5.1.0.QCNMIXM_ab0f9f2a68_10.0.zip
     */
    public OTA parseOTA(String fileName) {
        if (fileName.startsWith("miui_")) {
            String[] parts = fileName.split("_");
            String device = parts[1];
            String verVar = parts[2];
            String variant = verVar.substring(verVar.lastIndexOf(".")+1);
            String version = verVar.substring(1, verVar.lastIndexOf("."));
            String android = parts[parts.length-1].replace(".zip", "");
            String otaLink = "https://bigota.d.miui.com/" + verVar + "/" + fileName;
            return new OTA(device, OTA.OTA_TYPE_FULL, version, variant, android, otaLink);
        }
        String[] parts = fileName.split("-");
        String otaType = parts[1];
        String device = parts[2];
        String verVar1 = parts[3];
        String verVar2 = parts[4];
        String variant = verVar1.substring(verVar1.lastIndexOf(".")+1);
        String version = verVar1.substring(1, verVar1.lastIndexOf(".")) + " -> " +  verVar2.substring(1, verVar2.lastIndexOf("."));
        String android = parts[parts.length-1].replace(".zip", "");
        String otaLink = "https://bigota.d.miui.com/" + verVar2 + "/" + fileName;
        return new OTA(device, otaType, version, variant, android, otaLink);
    }

    private boolean hasPermissions() {
        String perm = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        if(checkCallingOrSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{perm});
            return false;
        }
        return true;
    }

    public void requestPermissions(String[] permissions) {
        Toast.makeText(this, "We need to storage permission for checking OTA files", Toast.LENGTH_LONG).show();
        requestPermissions(permissions, 1);
    }

    int retryCount = 0;
    int actionId = -1;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onClick(findViewById(actionId));
        } else if (retryCount++ < 5) {
            requestPermissions(permissions);
        }

    }

    @Override
    public void onClick(View view) {
        actionId = view.getId();
        switch (actionId) {
            case android.R.id.button1:
                scanForOTAFile();
                break;
            case android.R.id.button2:
                deleteOTAFile();
                break;
        }
    }

    private static class OTA {
        private static final String OTA_TYPE_FULL = "full";

        final String version;
        final String otaType;
        final String variant;
        final String android;
        final String link;
        final String device;

        private OTA(String device, String otaType, String version, String variant, String android, String link) {
            this.device = device;
            this.otaType = otaType;
            this.version = version;
            this.variant = variant;
            this.android = android;
            this.link = link;
        }
    }
}