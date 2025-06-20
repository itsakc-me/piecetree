package io.itsakc.demo;

import android.Manifest;
import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import io.itsakc.demo.databinding.ActivityMainBinding;
import io.itsakc.piecetree.PieceTree;
import io.itsakc.piecetree.common.FindMatch;
import io.itsakc.piecetree.common.Position;

public class MainActivity extends AppCompatActivity {

    private static final int MANAGE_EXTERNAL_STORAGE_REQUEST_CODE = 101;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private ActivityMainBinding binding;
    private TextView textView;
    private EditText editText;
    private Button button;

    private PieceTree pieceTree;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate and get instance of binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        textView = binding.textview;
        editText = binding.edittext;
        button = binding.button;

        // set content view to binding's root
        setContentView(binding.getRoot());

        // INFO: All these codes below are for testing purpose of PieceTree only, this is not part of the demo.
        // INFO: If you also want to debug and test PieceTree feel free to uncomment or do your stuff.

//        if (checkPermissions()) {
//            long startTimeMillis = System.currentTimeMillis();
//            loadContent();
//            long endTimeMillis = System.currentTimeMillis();
//            textView.setText("Time taken to load the file : " + (endTimeMillis - startTimeMillis) / 1000.0);
//        } else {
//            requestPermissions();
//        }
//
//        long startTimeMillis = System.currentTimeMillis();
//        FindMatch match = pieceTree.findNext("надкалывать/27,32,30,30,52,87,92,106,53,88,93,107,28,33,31,31,54,89,94,108", 0, false, true, null, false);
//        int pieceTreeLength = pieceTree.length();
//        int pieceTreeLineCount = pieceTree.lineCount();
//        String pieceTreeLineContentAt5 = pieceTree.lineContent(5);
//        int pieceTreeOffsetAt510 = pieceTree.offsetAt(5, 10);
//        Position pieceTreePositionAt5 = pieceTree.positionAt(5);
//        String pieceTreeTextRange010 = pieceTree.textRange(0, 10);
//        long endTimeMillis = System.currentTimeMillis();
//        textView.append("\nPieceTree match : " + match.toString());
//        textView.append("\nPieceTree length : " + pieceTreeLength);
//        textView.append("\nPieceTree line count : " + pieceTreeLineCount);
//        textView.append("\nPieceTree line content 5 : " + pieceTreeLineContentAt5);
//        textView.append("\nPieceTree offset at line 5, column 10 : " + pieceTreeOffsetAt510);
//        textView.append("\nPieceTree position at offset 5 : " + pieceTreePositionAt5);
//        textView.append("\nPieceTree text range from start offset 0, end offset 10 : " + pieceTreeTextRange010);
//        textView.append("\nTime taken to perform all these operations : " + (endTimeMillis - startTimeMillis) / 1000.0);
//
//        long dStartTimeMillis = System.currentTimeMillis();
//        pieceTree.delete(0, pieceTree.length() - 1);
//        long dEndTimeMillis = System.currentTimeMillis();
//        textView.append("\nTime taken to delete, leaving last char only : " + (dEndTimeMillis - dStartTimeMillis) / 1000.0);
//
//        editText.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                pieceTree.append(s.toString());
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {
//                textView.setText(pieceTree.text());
//            }
//        });
//
//        button.setOnClickListener(v -> {
//            pieceTree.delete(5, 10);
//            textView.setText(pieceTree.text());
//        });
    }

    /**
     * NOTE: This method is for testing purpose, so it assumes a file named test.txt
     * that is located in the external storage directory.
     */
    private void loadContent() {
        pieceTree = new PieceTree();
        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/test.txt");

        if (file.exists() && file.canRead()) {
            pieceTree.initialize(file);
            Toast.makeText(this, "Content loaded successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "File not found or cannot be read", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            int result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, MANAGE_EXTERNAL_STORAGE_REQUEST_CODE);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, MANAGE_EXTERNAL_STORAGE_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadContent();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MANAGE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    loadContent();
                } else {
                    Toast.makeText(this, "Manage External Storage Permission Denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.binding = null;
    }
}
