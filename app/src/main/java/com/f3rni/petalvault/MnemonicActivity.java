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

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.Objects;

public class MnemonicActivity extends AppCompatActivity {
    private static final String TAG = MnemonicActivity.class.getName();
    private static final int[] MNEMONIC_WORD_IDS = {R.id.mnemonicWord1, R.id.mnemonicWord2, R.id.mnemonicWord3, R.id.mnemonicWord4, R.id.mnemonicWord5, R.id.mnemonicWord6, R.id.mnemonicWord7, R.id.mnemonicWord8, R.id.mnemonicWord9, R.id.mnemonicWord10, R.id.mnemonicWord11, R.id.mnemonicWord12};

    private final Intent returnIntent = new Intent();

    private String[] mnemonicWords;
    private Mnemonic mnemonic;
    private boolean textChangedFromCode = false;
    private boolean editable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mnemonic);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize wordlist and main mnemonic class instance
        mnemonicWords = getResources().getStringArray(R.array.mnemonic_words);
        mnemonic = new Mnemonic(mnemonicWords);

        // Autocomplete words adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mnemonicWords);

        // Connect random button
        findViewById(R.id.btnRandom).setOnClickListener(v -> generateRandom());

        // Connect Show QR button
        findViewById(R.id.btnShow).setOnClickListener(v -> showQR());

        // Connect Scan QR button
        findViewById(R.id.btnScan).setOnClickListener(v -> scanQR());

        // Connect Copy button
        findViewById(R.id.btnCopy).setOnClickListener(v -> copyToClipboard());

        // Connect Paste button
        findViewById(R.id.btnPaste).setOnClickListener(v -> pasteFromClipboard());

        // Connect OK button
        findViewById(R.id.btnOk).setOnClickListener(v -> ok());

        // Connect adapters and edit listeners
        for (int mnemonicWordID : MNEMONIC_WORD_IDS) {
            AutoCompleteTextView autoCompleteTextView = findViewById(mnemonicWordID);
            autoCompleteTextView.setAdapter(adapter);
            autoCompleteTextView.addTextChangedListener(wordWatcher);
        }

        // Disable elements (default mode is non-editable)
        editable = false;

        findViewById(R.id.btnRandom).setEnabled(false);
        findViewById(R.id.btnScan).setEnabled(false);
        findViewById(R.id.btnPaste).setEnabled(false);

        for (int mnemonicWordID : MNEMONIC_WORD_IDS)
            findViewById(mnemonicWordID).setEnabled(false);

        // Input data
        Intent intent = getIntent();

        // Set title and description
        if (intent.hasExtra("title"))
            ((TextView) findViewById(R.id.mnemonicTitle)).setText(intent.getStringExtra("title"));
        if (intent.hasExtra("description"))
            ((TextView) findViewById(R.id.mnemonicDescription)).setText(intent.getStringExtra("description"));

        // Mnemonic provided -> split by space and build
        if (intent.hasExtra("mnemonic")) {
            try {
                setMnemonic(Objects.requireNonNull(intent.getStringArrayListExtra("mnemonic")).toArray(new String[0]));
                mnemonicToTextViews();
            } catch (Exception e) {
                Log.e(TAG, "Unable to build mnemonic", e);
                Toast.makeText(this, R.string.qr_scanner_mnemonic_error, Toast.LENGTH_SHORT).show();
            }
        }

        // Generate random mnemonic
        if (intent.hasExtra("random") && intent.getBooleanExtra("random", false)) generateRandom();

        // Editable mode
        if (intent.hasExtra("editable") && intent.getBooleanExtra("editable", false)) {
            editable = true;

            // Enable buttons
            findViewById(R.id.btnRandom).setEnabled(true);
            findViewById(R.id.btnScan).setEnabled(true);
            findViewById(R.id.btnPaste).setEnabled(true);

            // Enable word inputs
            for (int mnemonicWordID : MNEMONIC_WORD_IDS)
                findViewById(mnemonicWordID).setEnabled(true);
        }

        // Disable random
        if (intent.hasExtra("randomDisabled") && intent.getBooleanExtra("randomDisabled", false))
            findViewById(R.id.btnRandom).setEnabled(false);
    }

    /**
     * Shows mnemonic as QR code
     */
    private void showQR() {
        if (mnemonic.getMnemonic().isEmpty()) {
            Toast.makeText(this, R.string.no_mnemonic, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(MnemonicActivity.this, QRViewerActivity.class);
        intent.putExtra("title", getString(R.string.qr_viewer_mnemonic_title));
        intent.putExtra("description", getString(R.string.qr_viewer_mnemonic_description));
        intent.putExtra("mnemonic", mnemonic.getMnemonic());
        startActivity(intent);
    }

    /**
     * Scans QR code containing mnemonic phrase
     */
    private void scanQR() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt(getString(R.string.qr_scanner_mnemonic_title));
        options.setOrientationLocked(false);
        options.setBeepEnabled(false);
        options.setBarcodeImageEnabled(false);
        barcodeLauncher.launch(options);
    }

    /**
     * Copies mnemonic phrase to clipboard
     */
    private void copyToClipboard() {
        String mnemonicString = mnemonic.toString();
        if (mnemonicString.isEmpty()) return;

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) return;

        ClipData clip = ClipData.newPlainText(getString(R.string.mnemonic_clip_data), mnemonicString);
        clipboard.setPrimaryClip(clip);
    }

    /**
     * Uses mnemonic from clipboard
     */
    private void pasteFromClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard == null) {
            Toast.makeText(this, R.string.mnemonic_viewer_paste_error, Toast.LENGTH_SHORT).show();
            return;
        }

        ClipDescription clipDescription = clipboard.getPrimaryClipDescription();
        if (clipDescription == null || !clipDescription.hasMimeType(MIMETYPE_TEXT_PLAIN)) {
            Toast.makeText(this, R.string.mnemonic_viewer_paste_error, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String mnemonicString = Objects.requireNonNull(clipboard.getPrimaryClip()).getItemAt(0).getText().toString();
            if (mnemonicString.isEmpty()) {
                Toast.makeText(this, R.string.mnemonic_viewer_paste_error, Toast.LENGTH_SHORT).show();
                return;
            }
            setMnemonic(mnemonicString);
        } catch (Exception e) {
            Toast.makeText(this, R.string.mnemonic_viewer_paste_error, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Unable to paste mnemonic", e);
        }

        mnemonicToTextViews();
    }


    /**
     * Generates random mnemonic and updates views
     */
    private void generateRandom() {
        mnemonic.generateRandom();
        setMnemonic((String) null);
        mnemonicToTextViews();
    }

    /**
     * Ok button callback. Tries to build mnemonic and finishes if succeeded
     */
    private void ok() {
        if (!editable) {
            finish();
            return;
        }
        if (parseFromViews()) {
            setResult(Activity.RESULT_OK, returnIntent);
            finish();
            return;
        }
        Toast.makeText(this, R.string.mnemonic_viewer_exit_error, Toast.LENGTH_SHORT).show();
    }

    /**
     * QR scanner callback
     */
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
        // Scan cancelled
        if (result.getContents() == null) {
            Toast.makeText(this, R.string.qr_scanner_canceled, Toast.LENGTH_SHORT).show();
            return;
        }

        // Try to build mnemonic from it
        try {
            Mnemonic mnemonicTemp = new Mnemonic(mnemonicWords);
            mnemonicTemp.fromMnemonic(result.getContents());

            // Update
            setMnemonic(mnemonicTemp.getMnemonic().toArray(new String[0]));
            mnemonicToTextViews();
        } catch (Exception e) {
            Log.e(TAG, "Unable to build mnemonic", e);
            Toast.makeText(this, R.string.qr_scanner_mnemonic_error, Toast.LENGTH_SHORT).show();
        }
    });

    /**
     * Sets mnemonic words to textViews
     */
    private void mnemonicToTextViews() {
        if (mnemonic.getMnemonic().isEmpty()) return;
        ArrayList<String> mnemonicList = mnemonic.getMnemonic();
        textChangedFromCode = true;
        for (short i = 0; i < MNEMONIC_WORD_IDS.length; i++)
            ((AutoCompleteTextView) findViewById(MNEMONIC_WORD_IDS[i])).setText(mnemonicList.get(i));
        textChangedFromCode = false;
    }

    /**
     * Text edit callback. Tries to build mnemonic on "afterTextChanged"
     */
    private final TextWatcher wordWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            // Accept only from user
            if (textChangedFromCode) return;

            parseFromViews();
        }
    };

    /**
     * Tries to "build" mnemonic
     *
     * @return true if mnemonic is correct, false if not
     */
    private boolean parseFromViews() {
        // Read words
        String[] words = new String[MNEMONIC_WORD_IDS.length];
        for (short i = 0; i < MNEMONIC_WORD_IDS.length; i++)
            words[i] = String.valueOf(((AutoCompleteTextView) findViewById(MNEMONIC_WORD_IDS[i])).getText());

        // Try to build mnemonic from it
        try {
            Mnemonic mnemonicTemp = new Mnemonic(mnemonicWords);
            mnemonicTemp.fromMnemonic(words);
            setMnemonic(mnemonicTemp.getMnemonic().toArray(new String[0]));
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Unable to build mnemonic", e);
        }

        return false;
    }

    /**
     * Updates current mnemonic and returnIntent
     *
     * @param mnemonicWords array of words or null to ignore
     */
    private void setMnemonic(String[] mnemonicWords) {
        if (mnemonicWords != null) mnemonic.fromMnemonic(mnemonicWords);
        returnIntent.putExtra("mnemonic", mnemonic.toString());
    }

    /**
     * Updates current mnemonic and returnIntent
     *
     * @param mnemonicString mnemonic phrase or null to ignore
     */
    private void setMnemonic(String mnemonicString) {
        if (mnemonicString != null) mnemonic.fromMnemonic(mnemonicString);
        returnIntent.putExtra("mnemonic", mnemonic.toString());
    }
}