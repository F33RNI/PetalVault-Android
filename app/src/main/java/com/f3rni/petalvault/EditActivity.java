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
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class EditActivity extends AppCompatActivity {
    private TextInputLayout inputSite;
    private TextInputLayout inputUser;
    private TextInputLayout inputPassword;
    private TextInputLayout inputNotes;
    private String id;

    private String inputSiteStart, inputUserStart, inputPasswordStart, inputNotesStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Input data
        Intent intent = getIntent();

        // Unique ID
        if (intent.hasExtra("id")) id = intent.getStringExtra("id");

        // Set title
        TextView title = findViewById(R.id.editTitle);
        if (intent.hasExtra("title")) title.setText(intent.getStringExtra("title"));

        // Enable or disable delete button
        if (intent.hasExtra("deletable") && intent.getBooleanExtra("deletable", false))
            findViewById(R.id.btnDeleteEntry).setEnabled(true);
        else findViewById(R.id.btnDeleteEntry).setEnabled(false);

        // Connect fields
        inputSite = findViewById(R.id.inputSite);
        inputUser = findViewById(R.id.inputUser);
        inputPassword = findViewById(R.id.inputPassword);
        inputNotes = findViewById(R.id.inputNotes);

        // Set text
        if (intent.hasExtra("site"))
            Objects.requireNonNull(inputSite.getEditText()).setText(intent.getStringExtra("site"));
        if (intent.hasExtra("user"))
            Objects.requireNonNull(inputUser.getEditText()).setText(intent.getStringExtra("user"));
        if (intent.hasExtra("password"))
            Objects.requireNonNull(inputPassword.getEditText()).setText(intent.getStringExtra("password"));
        if (intent.hasExtra("notes"))
            Objects.requireNonNull(inputNotes.getEditText()).setText(intent.getStringExtra("notes"));

        // Connect copy buttons
        inputSite.setStartIconOnClickListener(v -> copyFromInput(inputSite));
        inputUser.setStartIconOnClickListener(v -> copyFromInput(inputUser));
        inputPassword.setStartIconOnClickListener(v -> copyFromInput(inputPassword));
        inputNotes.setStartIconOnClickListener(v -> copyFromInput(inputNotes));

        // Connect site to title
        Objects.requireNonNull(inputSite.getEditText()).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                title.setText(s);
            }
        });

        // Connect exit buttons
        findViewById(R.id.btnEditCancel).setOnClickListener(v -> finish());
        findViewById(R.id.btnEditDone).setOnClickListener(v -> done());
        findViewById(R.id.btnDeleteEntry).setOnClickListener(v -> delete(false));

        // Assign initial text
        inputSiteStart = String.valueOf(Objects.requireNonNull(inputSite.getEditText()).getText());
        inputUserStart = String.valueOf(Objects.requireNonNull(inputUser.getEditText()).getText());
        inputPasswordStart = String.valueOf(Objects.requireNonNull(inputPassword.getEditText()).getText());
        inputNotesStart = String.valueOf(Objects.requireNonNull(inputNotes.getEditText()).getText());
    }

    /**
     * Puts text values and "changed" flag into intent and finishes activity
     */
    private void done() {
        Intent returnIntent = new Intent();

        if (id != null && !id.isEmpty()) returnIntent.putExtra("id", id);

        String inputSiteNew = String.valueOf(Objects.requireNonNull(inputSite.getEditText()).getText());
        String inputUserNew = String.valueOf(Objects.requireNonNull(inputUser.getEditText()).getText());
        String inputPasswordNew = String.valueOf(Objects.requireNonNull(inputPassword.getEditText()).getText());
        String inputNotesNew = String.valueOf(Objects.requireNonNull(inputNotes.getEditText()).getText());

        returnIntent.putExtra("site", inputSiteNew);
        returnIntent.putExtra("user", inputUserNew);
        returnIntent.putExtra("password", inputPasswordNew);
        returnIntent.putExtra("notes", inputNotesNew);

        returnIntent.putExtra("changed", !inputSiteNew.equals(inputSiteStart) || !inputUserNew.equals(inputUserStart) || !inputPasswordNew.equals(inputPasswordStart) || !inputNotesNew.equals(inputNotesStart));

        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    /**
     * Asks for user confirmation to delete entry and puts delete boolean into intent and finishes activity
     */
    private void delete(boolean confirmed) {
        if (!confirmed) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.entry_delete_confirm_title);
            builder.setMessage(R.string.entry_delete_confirm_description);
            builder.setPositiveButton(R.string.no, null);
            builder.setNegativeButton(R.string.yes, (dialog, which) -> delete(true));
            builder.show();
            return;
        }

        Intent returnIntent = new Intent();

        if (id != null && !id.isEmpty()) returnIntent.putExtra("id", id);

        returnIntent.putExtra("delete", true);

        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    /**
     * Copies TextInputLayout' data to clipboard
     */
    private void copyFromInput(TextInputLayout textInputLayout) {
        String text = String.valueOf(Objects.requireNonNull(textInputLayout.getEditText()).getText());
        if (text.isEmpty()) return;

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) return;

        ClipData clip = ClipData.newPlainText(String.valueOf(textInputLayout.getHint()), text);
        clipboard.setPrimaryClip(clip);
    }
}