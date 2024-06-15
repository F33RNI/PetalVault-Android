/**
 * This file is part of the PetalVault-Android password manager distribution.
 * See <https://github.com/F33RNI/PetalVault-Android>.
 * Copyright (C) 2024 Fern Lane
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, version 3.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.f3rni.petalvault;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowInsets;
import android.view.WindowMetrics;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Objects;

public class QRViewerActivity extends AppCompatActivity {
    private static final String TAG = QRViewerActivity.class.getName();

    // Approximately. Actual data may exceed this value slightly
    private static final int QR_LIMIT_BYTES = 500;

    private int qrCodeSize, dataIndex;
    private final ArrayList<String> data = new ArrayList<>();
    private TextView qrIndexTotal;
    private Button btnPrev, btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_qrviewer);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Input data
        Intent intent = getIntent();

        // Set title and description
        if (intent.hasExtra("title")) ((TextView) findViewById(R.id.qrTitle)).setText(intent.getStringExtra("title"));
        if (intent.hasExtra("description"))
            ((TextView) findViewById(R.id.qrDescription)).setText(intent.getStringExtra("description"));

        // Mnemonic QR -> split by space
        if (intent.hasExtra("mnemonic")) {
            StringBuilder mnemonic = new StringBuilder();
            for (String word : Objects.requireNonNull(intent.getStringArrayListExtra("mnemonic")))
                mnemonic.append(word).append(" ");
            data.add(mnemonic.toString().trim());
        }

        // Actions -> create multiple QRs
        if (intent.hasExtra("actions")) {
            try {
                JSONArray datasTemp = new JSONArray();

                // Convert to proper json
                int indexCurrent = 0;
                for (String action : Objects.requireNonNull(intent.getStringArrayListExtra("actions"))) {
                    // New QR code
                    if (indexCurrent >= datasTemp.length()) {
                        JSONObject dataTemp = new JSONObject();
                        dataTemp.put("i", indexCurrent);
                        dataTemp.put("acts", new JSONArray());

                        // Add sync salt of the 1st QR
                        if (intent.hasExtra("salt") && indexCurrent == 0)
                            dataTemp.put("salt", intent.getStringExtra("salt"));

                        datasTemp.put(indexCurrent, dataTemp);
                    }

                    // Convert action from string and put it to the array
                    ((JSONArray) ((JSONObject) datasTemp.get(indexCurrent)).get("acts")).put(new JSONObject(action));

                    // Convert entire data to string
                    String dataString = datasTemp.get(indexCurrent).toString().replace(" ", "").replace("\\/", "/");
                    dataString = dataString.substring(1, dataString.length() - 1);

                    // Size exceeded -> switch to the next QR
                    if (dataString.length() > QR_LIMIT_BYTES) indexCurrent++;
                }

                // Add total size and convert to strings
                int sizeTotal = datasTemp.length();
                for (int i = 0; i < sizeTotal; i++) {
                    JSONObject dataTemp = (JSONObject) datasTemp.get(i);
                    dataTemp.put("n", sizeTotal);
                    String dataString = datasTemp.get(i).toString().replace(" ", "").replace("\\/", "/");
                    dataString = dataString.substring(1, dataString.length() - 1);
                    data.add(dataString);
                }
            } catch (Exception e) {
                Log.e(TAG, "Unable to parse actions", e);
            }
        }

        // Connect views
        qrIndexTotal = findViewById(R.id.qrIndexTotal);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);

        // Connect Previous button
        btnPrev.setOnClickListener(v -> {
            if (dataIndex <= 0) return;
            dataIndex--;
            showQR();
        });

        // Connect Next button
        btnNext.setOnClickListener(v -> {
            if (dataIndex >= data.size() - 1) return;
            dataIndex++;
            showQR();
        });

        // Click on QR
        findViewById(R.id.qrViewer).setOnClickListener(v -> {
            if (dataIndex >= data.size() - 1) {
                finish();
                return;
            }
            dataIndex++;
            showQR();
        });

        // Determine QR size based on screen size
        qrCodeSize = Math.min(getScreenHeight(this), getScreenWidth(this));

        // Show first QR code
        showQR();
    }

    /**
     * Shows data by index as QR code. dataIndex must be in [0 to data.size() - 1] range
     */
    private void showQR() {
        try {
            // Show QR code
            ImageView imageViewQrCode = findViewById(R.id.qrViewer);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(data.get(dataIndex), BarcodeFormat.QR_CODE, qrCodeSize, qrCodeSize);
            imageViewQrCode.setImageBitmap(bitmap);

            // Enable or disable buttons
            btnPrev.setEnabled(dataIndex > 0);
            btnNext.setEnabled(dataIndex < data.size() - 1);

            // Show index
            if (data.size() == 1) qrIndexTotal.setText("");
            else qrIndexTotal.setText(getString(R.string.qr_index, (dataIndex + 1), data.size()));

        } catch (Exception e) {
            Log.e(TAG, "Unable to show QR code", e);
        }
    }

    /**
     * <https://stackoverflow.com/a/70350121>
     */
    public static int getScreenWidth(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics windowMetrics = activity.getWindowManager().getCurrentWindowMetrics();
            Rect bounds = windowMetrics.getBounds();
            android.graphics.Insets insets = windowMetrics.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());

            if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && activity.getResources().getConfiguration().smallestScreenWidthDp < 600) { // landscape and phone
                int navigationBarSize = insets.right + insets.left;
                return bounds.width() - navigationBarSize;
            } else { // portrait or tablet
                return bounds.width();
            }
        } else {
            DisplayMetrics outMetrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
            return outMetrics.widthPixels;
        }
    }

    /**
     * <https://stackoverflow.com/a/70350121>
     */
    public static int getScreenHeight(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics windowMetrics = activity.getWindowManager().getCurrentWindowMetrics();
            Rect bounds = windowMetrics.getBounds();
            android.graphics.Insets insets = windowMetrics.getWindowInsets().getInsetsIgnoringVisibility(androidx.core.view.WindowInsetsCompat.Type.systemBars());

            if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && activity.getResources().getConfiguration().smallestScreenWidthDp < 600) { // landscape and phone
                return bounds.height();
            } else { // portrait or tablet
                int navigationBarSize = insets.bottom;
                return bounds.height() - navigationBarSize;
            }
        } else {
            DisplayMetrics outMetrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
            return outMetrics.heightPixels;
        }
    }
}