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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();

    public static final String CONFIG_FILENAME = "config.json";
    public static final String VAULTS_DIR = "vaults";
    public static final String DATA_VERSION = "2.0.0";
    private static final short STAGE_NAME = 0, STAGE_ENABLE_PASSWORD = 1, STAGE_ASK_PASSWORD = 2, STAGE_CONFIRM_PASSWORD = 3, STAGE_NEW_OR_IMPORT = 4;

    private ConfigManager configManager;
    private VaultUtils vaultUtils;

    private LinearLayout vaultsLayout;
    private TextInputEditText alertEditText;
    private boolean newVault;
    private String vaultName;
    private String vaultPassword1;
    private String[] vaultMnemonic;
    private short newOrImportStage = STAGE_NAME;
    private VaultUtils.VaultNamePath vaultNamePathTemp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Read wordlist
        String[] mnemonicWordlist = getResources().getStringArray(R.array.mnemonic_words);

        // Initialize config manager instance
        configManager = new ConfigManager(new File(getFilesDir(), CONFIG_FILENAME), DATA_VERSION);

        // Initialize vault instance
        vaultUtils = new VaultUtils(new File(getFilesDir(), VAULTS_DIR), configManager, mnemonicWordlist);

        // Connect elements
        vaultsLayout = findViewById(R.id.vaultsLayout);
        findViewById(R.id.btnNewVault).setOnClickListener(v -> newOrImport(true));
        findViewById(R.id.btnImportVault).setOnClickListener(v -> newOrImport(false));

        // Logo -> Show about
        findViewById(R.id.petalVaultLogo).setOnClickListener(v -> about());
    }

    /**
     * Refresh available vaults on resume
     */
    @Override
    protected void onResume() {
        super.onResume();
        configManager.refresh();
        refreshVaults();
    }

    /**
     * Refreshes available vaults
     */
    private void refreshVaults() {
        // Clear current vaults
        vaultsLayout.removeAllViews();
        System.gc();

        // Parse
        vaultUtils.refreshAvailable();

        // Add each vault as TextView
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < vaultUtils.getVaultNamePaths().size(); i++) {
            final VaultUtils.VaultNamePath vaultNamePath = vaultUtils.getVaultNamePaths().get(i);

            LinearLayout rowLayout = (LinearLayout) inflater.inflate(R.layout.vaults_row, vaultsLayout, false);
            TextView vaultName = rowLayout.findViewById(R.id.vaultName);
            vaultName.setText(vaultNamePath.name);

            // Ask for password / mnemonic on click
            vaultName.setOnClickListener(v -> openVaultAskPassword(vaultNamePath));

            vaultsLayout.addView(rowLayout);
        }

        // Add "No vaults" entry
        if (vaultUtils.getVaultNamePaths().isEmpty()) {
            LinearLayout rowLayout = (LinearLayout) inflater.inflate(R.layout.vaults_row, vaultsLayout, false);
            TextView vaultName = rowLayout.findViewById(R.id.vaultName);
            vaultName.setText(getString(R.string.no_vaults));
            vaultsLayout.addView(rowLayout);
        }
    }

    /**
     * Asks for password or mnemonic to open the vault
     */
    private void openVaultAskPassword(final VaultUtils.VaultNamePath vaultNamePath) {

        // Read
        JSONObject vault = vaultUtils.read(vaultNamePath.relPath);
        if (vault == null) {
            Toast.makeText(this, R.string.vault_open_error, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if vault has mnemonic encrypted
        if (vault.has("mnemonic_encrypted")) {
            // Build container
            byte[] mnemonicEncryptedBytes, salt1, salt2;
            try {
                mnemonicEncryptedBytes = CryptoUtils.base64Decode(vault.getString("mnemonic_encrypted"));
                salt1 = CryptoUtils.base64Decode(vault.getString("mnemonic_salt_1"));
                salt2 = CryptoUtils.base64Decode(vault.getString("mnemonic_salt_2"));
            } catch (JSONException e) {
                Log.e(TAG, "Error opening vault", e);
                Toast.makeText(this, R.string.vault_open_error, Toast.LENGTH_SHORT).show();
                return;
            }
            final CryptoUtils.MnemonicEncrypted mnemonicEncrypted = new CryptoUtils.MnemonicEncrypted(mnemonicEncryptedBytes, salt1, salt2);

            // Ask for password
            AlertDialog.Builder passwordDialog = getAlertBuilder(true, true, (short) -1);
            passwordDialog.setTitle(R.string.vault_open_ask_password_title);
            passwordDialog.setMessage(R.string.vault_open_ask_password_description);
            alertEditText.setText("");

            // Ok button -> decrypt mnemonic and load activity or openVault() again in case of error
            passwordDialog.setPositiveButton(R.string.ok, (dialog, which) -> {
                try {
                    String[] mnemonic = CryptoUtils.decryptMnemonic(mnemonicEncrypted, String.valueOf(alertEditText.getText()));
                    openVaultActivity(vaultNamePath, mnemonic, false);
                    return;
                } catch (Exception e) {
                    Log.e(TAG, "Error decrypting mnemonic", e);
                }

                // Wrong password?
                Toast.makeText(this, R.string.vault_mnemonic_decrypt_error, Toast.LENGTH_SHORT).show();
                openVaultAskPassword(vaultNamePath);
            });

            // Use mnemonic instead of master password
            passwordDialog.setNeutralButton(R.string.use_mnemonic_instead, (dialog, which) -> openVaultAskMnemonic(vaultNamePath));
            passwordDialog.show();
        }

        // Mnemonic only
        else openVaultAskMnemonic(vaultNamePath);
    }

    /**
     * Asks for mnemonic to open the vault
     */
    private void openVaultAskMnemonic(final VaultUtils.VaultNamePath vaultNamePath) {
        // Build mnemonic activity intent
        Intent intent = new Intent(MainActivity.this, MnemonicActivity.class);
        intent.putExtra("title", getString(R.string.mnemonic_viewer_title));
        intent.putExtra("description", getString(R.string.mnemonic_viewer_description_open));
        intent.putExtra("editable", true);
        intent.putExtra("randomDisabled", true);

        // Launch it
        vaultNamePathTemp = vaultNamePath;
        mnemonicLauncherOpenVault.launch(intent);
    }

    /**
     * Start VaultActivity and passes vaultNamePath and data for vault utils to it
     */
    private void openVaultActivity(final VaultUtils.VaultNamePath vaultNamePath, String[] vaultMnemonic, boolean importAfter) {
        Intent intent = new Intent(MainActivity.this, VaultActivity.class);
        intent.putExtra("name", vaultNamePath.name);
        intent.putExtra("relPath", vaultNamePath.relPath);
        intent.putExtra("vaultMnemonic", vaultMnemonic);
        if (importAfter) intent.putExtra("import", true);
        startActivity(intent);

        System.gc();
    }

    private void newOrImport(boolean new_) {
        newVault = new_;
        newOrImport(false, false, false);
    }

    private void newOrImport(boolean fromDialogue, boolean dialogueCanceled, boolean dialogueResult) {
        // Canceled
        if (fromDialogue && dialogueCanceled) {
            Toast.makeText(this, R.string.canceled, Toast.LENGTH_SHORT).show();
            newOrImportStage = STAGE_NAME;
            return;
        }

        AlertDialog.Builder vaultAlert;

        // Only vault name dialogue can be called
        if (!fromDialogue) {
            newOrImportStage = STAGE_NAME;
            vaultAlert = getAlertBuilder(true, false, STAGE_NAME);

            // Ok / Cancel buttons
            vaultAlert.setPositiveButton(R.string.ok, (dialog, whichButton) -> newOrImport(true, false, true));
            vaultAlert.setNegativeButton(R.string.cancel, (dialog, whichButton) -> newOrImport(true, true, false));
            vaultAlert.setOnCancelListener(dialog -> newOrImport(true, true, false));
            vaultAlert.show();
        }

        // From dialogue
        else {
            switch (newOrImportStage) {
                // Name -> mnemonic
                case STAGE_NAME:
                    // Get name
                    vaultName = String.valueOf(alertEditText.getText()).trim();

                    // Name not provided -> cancel
                    if (vaultName.isEmpty()) {
                        newOrImport(true, true, false);
                        return;
                    }

                    // Check if already exists
                    if (vaultUtils.getNamePath(vaultName, null) != null) {
                        Toast.makeText(this, getString(R.string.vault_create_already_exists, vaultName), Toast.LENGTH_SHORT).show();

                        // Ask for name again
                        newOrImport(false, false, false);
                        return;
                    }

                    // Build mnemonic activity intent
                    Intent intent = new Intent(MainActivity.this, MnemonicActivity.class);
                    intent.putExtra("title", getString(R.string.mnemonic_viewer_title));
                    intent.putExtra("description", getString(R.string.mnemonic_viewer_description_create));
                    intent.putExtra("editable", true);
                    intent.putExtra("random", true);

                    // Next stage - use password (yes / no)
                    newOrImportStage = STAGE_ENABLE_PASSWORD;
                    mnemonicLauncherNewVault.launch(intent);
                    break;

                // Mnemonic -> use password (yes / no)
                case STAGE_ENABLE_PASSWORD:
                    vaultAlert = getAlertBuilder(false, false, STAGE_ENABLE_PASSWORD);

                    // Next stage - ask for password
                    newOrImportStage = STAGE_ASK_PASSWORD;

                    // Yes / No buttons
                    vaultAlert.setPositiveButton(R.string.yes, (dialog, whichButton) -> newOrImport(true, false, true));
                    vaultAlert.setNegativeButton(R.string.no, (dialog, whichButton) -> newOrImport(true, false, false));
                    vaultAlert.setOnCancelListener(dialog -> newOrImport(true, true, false));
                    vaultAlert.show();
                    break;

                // Use password (yes / no) -> ask for password or create vault
                case STAGE_ASK_PASSWORD:
                    // Don't use master password
                    if (!dialogueResult) {
                        newOrImportStage = STAGE_NEW_OR_IMPORT;
                        vaultPassword1 = "";
                        newOrImport(true, false, false);
                        break;
                    }

                    vaultAlert = getAlertBuilder(true, true, STAGE_ASK_PASSWORD);

                    // Next stage - ask for password confirmation (or create vault)
                    alertEditText.setText("");
                    newOrImportStage = STAGE_CONFIRM_PASSWORD;

                    // OK button only
                    vaultAlert.setPositiveButton(R.string.ok, (dialog, whichButton) -> newOrImport(true, false, true));
                    vaultAlert.setOnCancelListener(dialog -> newOrImport(true, true, false));
                    vaultAlert.show();
                    break;

                // Ask for password -> Ask for password confirmation or create vault
                case STAGE_CONFIRM_PASSWORD:
                    vaultPassword1 = String.valueOf(alertEditText.getText());

                    // No password provided -> don't use master password
                    if (!dialogueResult || vaultPassword1.isEmpty()) {
                        newOrImportStage = STAGE_NEW_OR_IMPORT;
                        vaultPassword1 = "";
                        newOrImport(true, false, false);
                        break;
                    }

                    vaultAlert = getAlertBuilder(true, true, STAGE_CONFIRM_PASSWORD);

                    // Next stage - create vault
                    alertEditText.setText("");
                    newOrImportStage = STAGE_NEW_OR_IMPORT;

                    // OK button only
                    vaultAlert.setPositiveButton(R.string.ok, (dialog, whichButton) -> newOrImport(true, false, true));
                    vaultAlert.setOnCancelListener(dialog -> newOrImport(true, true, false));
                    vaultAlert.show();

                    break;

                // Check if passwords are match and finally create or import vault
                case STAGE_NEW_OR_IMPORT:
                    // Use password -> check if vaultPassword1 and vaultPassword2 are equal
                    if (vaultPassword1 != null && !vaultPassword1.isEmpty()) {
                        String vaultPassword2 = String.valueOf(alertEditText.getText());

                        // Passwords don't match -> reset and ask for password again
                        if (!vaultPassword1.equals(vaultPassword2)) {
                            Toast.makeText(this, R.string.vault_ask_password_not_match, Toast.LENGTH_SHORT).show();
                            vaultPassword1 = "";
                            newOrImportStage = STAGE_ASK_PASSWORD;
                            newOrImport(true, false, true);
                            break;

                        }
                    }

                    // Create vault and open it
                    try {
                        if (vaultUtils.create(vaultName, vaultMnemonic, vaultPassword1)) {
                            refreshVaults();
                            openVaultActivity(vaultUtils.getNamePath(vaultName, null), vaultMnemonic, !newVault);
                        } else Toast.makeText(this, R.string.vault_create_error, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error creating vault", e);
                        Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
                    }

                    break;
            }
        }
    }

    /**
     * Build plain alert dialog or with editText element
     *
     * @param withAlertEditText add EditText element
     * @param password          hide typed text
     * @param titleFromStage    set title and description from STAGE_... or -1 to ignore it
     * @return alert dialog
     */
    @NonNull
    private AlertDialog.Builder getAlertBuilder(boolean withAlertEditText, boolean password, short titleFromStage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Add editable text
        if (withAlertEditText) {
            LayoutInflater inflater = LayoutInflater.from(this);

            int layoutID = password ? R.layout.text_input_dialog_password : R.layout.text_input_dialog;
            LinearLayout dialogueInputLayout = (LinearLayout) inflater.inflate(layoutID, null, false);
            alertEditText = dialogueInputLayout.findViewById(R.id.inputView);

            // Hint for vault name or password
            if (titleFromStage == STAGE_NAME) alertEditText.setHint(R.string.vault_name_hint);
            else if (titleFromStage == STAGE_ASK_PASSWORD || titleFromStage == STAGE_CONFIRM_PASSWORD)
                alertEditText.setHint(R.string.password);
            else alertEditText.setHint(null);

            builder.setView(dialogueInputLayout);
        }

        // Alert title
        if (titleFromStage >= 0) {
            if (titleFromStage == STAGE_NAME) {
                if (newVault) builder.setTitle(R.string.vault_ask_name_title_new);
                else builder.setTitle(R.string.vault_ask_name_title_import);
                builder.setMessage(R.string.vault_ask_name);
            } else if (titleFromStage == STAGE_ENABLE_PASSWORD) {
                builder.setTitle(R.string.vault_ask_encrypt_password_title);
                builder.setMessage(R.string.vault_ask_encrypt_password_text);
            } else if (titleFromStage == STAGE_ASK_PASSWORD) builder.setTitle(R.string.vault_ask_password);
            else if (titleFromStage == STAGE_CONFIRM_PASSWORD) builder.setTitle(R.string.vault_ask_password_confirm);
        }

        return builder;
    }

    /**
     * Shows about message with github link
     */
    private void about() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.logo);
        builder.setTitle(R.string.app_name);
        builder.setMessage(R.string.about);

        // Show GitHub page
        builder.setNeutralButton(R.string.github, (dialog, which) -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://github.com/F33RNI/PetalVault-Android"));
            startActivity(intent);
        });

        builder.setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    /**
     * New vault mnemonic callback
     */
    private final ActivityResultLauncher<Intent> mnemonicLauncherNewVault = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        // Mnemonic provided ?
        if (result.getResultCode() == RESULT_OK) {
            Intent data = result.getData();

            // Mnemonic not provided
            if (data == null || !data.hasExtra("mnemonic")) {
                newOrImport(true, true, false);
                return;
            }
            String mnemonicString = data.getStringExtra("mnemonic");
            if (mnemonicString == null || mnemonicString.isEmpty()) {
                newOrImport(true, true, false);
                return;
            }

            vaultMnemonic = mnemonicString.split(" ");
            newOrImport(true, false, true);
        }

        // Canceled
        else newOrImport(true, true, false);
    });

    /**
     * Open vault mnemonic callback
     */
    private final ActivityResultLauncher<Intent> mnemonicLauncherOpenVault = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        // Mnemonic provided ?
        if (result.getResultCode() == RESULT_OK) {
            Intent data = result.getData();

            // Mnemonic not provided
            if (data == null || !data.hasExtra("mnemonic")) return;
            String mnemonicString = data.getStringExtra("mnemonic");
            if (mnemonicString == null || mnemonicString.isEmpty()) return;

            // Mnemonic provided -> launch activity
            openVaultActivity(vaultNamePathTemp, mnemonicString.split(" "), false);
        }
    });
}
