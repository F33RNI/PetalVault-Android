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

import static com.f3rni.petalvault.MainActivity.CONFIG_FILENAME;
import static com.f3rni.petalvault.MainActivity.DATA_VERSION;
import static com.f3rni.petalvault.MainActivity.VAULTS_DIR;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VaultActivity extends AppCompatActivity {
    private static final String TAG = VaultActivity.class.getName();

    private VaultUtils vaultUtils;
    private EntriesContainerAdapter entriesContainerAdapter;
    private String vaultPath, vaultName;
    private List<Boolean> receivedParts;
    private VaultUtils.SyncData syncFromData;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_vault);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Input data
        Intent intent = getIntent();

        // Exit if no data
        if (!intent.hasExtra("name") || !intent.hasExtra("relPath") || !intent.hasExtra("vaultMnemonic")) {
            Log.e(TAG, "No data provided");
            finish();
            return;
        }

        // Read wordlist
        String[] mnemonicWords = getResources().getStringArray(R.array.mnemonic_words);

        // Initialize class instances and read input data
        ConfigManager configManager = new ConfigManager(new File(getFilesDir(), CONFIG_FILENAME), DATA_VERSION);
        vaultUtils = new VaultUtils(new File(getFilesDir(), VAULTS_DIR), configManager, mnemonicWords);
        vaultUtils.refreshAvailable();
        vaultPath = intent.getStringExtra("relPath");
        vaultName = intent.getStringExtra("name");

        // Set title (vault name)
        ((TextView) findViewById(R.id.vaultTitle)).setText(vaultName);

        // Open and decrypt vault
        if (!vaultUtils.open(vaultPath, intent.getStringArrayExtra("vaultMnemonic"))) {
            Toast.makeText(this, R.string.vault_open_decrypt_error, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Enable delete device button only if there is at least one sync device
        findViewById(R.id.btnDeleteDevice).setEnabled(!vaultUtils.getDeviceNames().isEmpty());

        // Connect elements
        findViewById(R.id.btnShowMnemonic).setOnClickListener(v -> showMnemonic());
        findViewById(R.id.btnAddEntry).setOnClickListener(v -> addEntry());
        findViewById(R.id.btnRenameVault).setOnClickListener(v -> rename(null));
        findViewById(R.id.btnDeleteVault).setOnClickListener(v -> delete(false));
        findViewById(R.id.btnSyncTo).setOnClickListener(v -> syncToDevice(null, false));
        findViewById(R.id.btnSyncFrom).setOnClickListener(v -> syncFrom(false));
        findViewById(R.id.btnExport).setOnClickListener(v -> export());
        findViewById(R.id.btnDeleteDevice).setOnClickListener(v -> deleteDevice(null, false));

        // Create and connect adapter
        RecyclerView entries = findViewById(R.id.entries);
        entriesContainerAdapter = new EntriesContainerAdapter(vaultUtils.getVaultEntries());
        entriesContainerAdapter.setRowClickListener(this::editEntry);
        entries.setLayoutManager(new LinearLayoutManager(this));
        entries.setAdapter(entriesContainerAdapter);

        // Import data?
        if (intent.hasExtra("import") && intent.getBooleanExtra("import", false)) syncFrom(false);
    }

    /**
     * Sync vault to device
     *
     * @param deviceName existing device or null to create a new one
     * @param createNew  true to create a new one. Please always call with false first to ask user
     */
    private void syncToDevice(String deviceName, boolean createNew) {
        // Recursively ask for device name (or new device)
        if (deviceName == null && !createNew) {
            List<String> deviceNames = vaultUtils.getDeviceNames();

            // Create new device if no devices
            if (deviceNames.isEmpty()) {
                syncToDevice(null, true);
                return;
            }

            // Ask for device
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.sync_to_title);

            builder.setItems(deviceNames.toArray(new String[0]), (dialog, which) -> {
                if (which < deviceNames.size()) syncToDevice(deviceNames.get(which), false);
            });
            builder.setNeutralButton(R.string.add_device, (dialog, which) -> syncToDevice(null, true));

            builder.show();
            return;
        }

        // Create new device
        if (deviceName == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.add_device);
            builder.setMessage(R.string.add_device_title);

            LinearLayout alertLayout = new LinearLayout(this);
            alertLayout.setOrientation(LinearLayout.VERTICAL);

            LayoutInflater inflater = LayoutInflater.from(this);

            LinearLayout dialogInputLayout = (LinearLayout) inflater.inflate(R.layout.text_input_dialog, alertLayout, false);
            TextInputEditText textInputEditText = dialogInputLayout.findViewById(R.id.inputView);

            textInputEditText.setHint(R.string.add_device_hint);

            builder.setView(dialogInputLayout);
            alertLayout.addView(dialogInputLayout);
            builder.setView(alertLayout);

            builder.setNegativeButton(R.string.cancel, null);
            builder.setPositiveButton(R.string.ok, (dialog, which) -> syncToDevice(String.valueOf(textInputEditText.getText()), true));
            builder.show();
            return;
        }

        if (createNew) deviceName = deviceName.trim();

        // Check if name is not empty
        if (deviceName.isEmpty()) return;

        // Check if not exists
        if (createNew) {
            List<String> deviceNames = vaultUtils.getDeviceNames();
            if (deviceNames.contains(deviceName)) {
                Toast.makeText(this, getString(R.string.add_device_error_exists, deviceName), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Try to build sync data
        VaultUtils.SyncData syncData = null;
        try {
            syncData = vaultUtils.syncTo(deviceName, vaultPath);
        } catch (Exception e) {
            Log.e(TAG, "Sync to error", e);
            Toast.makeText(this, String.valueOf(e), Toast.LENGTH_LONG).show();
        }

        // Enable delete device button only if there is at least one sync device
        findViewById(R.id.btnDeleteDevice).setEnabled(!vaultUtils.getDeviceNames().isEmpty());

        // Check if we have anything to sync
        if (syncData == null) {
            Toast.makeText(this, R.string.nothing_to_sync, Toast.LENGTH_SHORT).show();
            return;
        }

        // Start QR viewer
        Intent intent = new Intent(VaultActivity.this, QRViewerActivity.class);
        intent.putExtra("title", getString(R.string.qr_viewer_sync_to_title));
        intent.putExtra("description", getString(R.string.qr_viewer_sync_to_description));
        intent.putExtra("actions", syncData.actions);
        intent.putExtra("salt", syncData.salt);
        startActivity(intent);
    }

    /**
     * Exports current vault
     */
    private void export() {
        // Try to build sync data
        VaultUtils.SyncData syncData = null;
        try {
            syncData = vaultUtils.syncTo(null, vaultPath);
        } catch (Exception e) {
            Log.e(TAG, "Error exporting vault", e);
            Toast.makeText(this, String.valueOf(e), Toast.LENGTH_LONG).show();
        }

        // Check if we have anything to sync
        if (syncData == null) {
            Toast.makeText(this, R.string.nothing_to_sync, Toast.LENGTH_SHORT).show();
            return;
        }

        // Start QR viewer
        Intent intent = new Intent(VaultActivity.this, QRViewerActivity.class);
        intent.putExtra("title", getString(R.string.qr_viewer_export_to_title));
        intent.putExtra("description", getString(R.string.qr_viewer_export_to_description));
        intent.putExtra("actions", syncData.actions);
        intent.putExtra("salt", syncData.salt);
        startActivity(intent);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void syncFrom(boolean fromQRReader) {
        // Reset some variables before the first scan
        if (!fromQRReader) {
            receivedParts = null;
            syncFromData = null;
        }

        // Launch scanner if we not received all data
        if (receivedParts == null || receivedParts.isEmpty() || receivedParts.contains(false) || syncFromData == null || syncFromData.salt == null) {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setPrompt(getString(R.string.qr_scanner_sync_import));
            options.setOrientationLocked(false);
            options.setBeepEnabled(false);
            options.setBarcodeImageEnabled(false);
            barcodeLauncher.launch(options);
            return;
        }

        // Try to sync from / import
        try {
            if (vaultUtils.syncFrom(syncFromData, vaultPath)) {
                Toast.makeText(this, R.string.import_sync_ok, Toast.LENGTH_SHORT).show();

                // Update recycler view
                entriesContainerAdapter.notifyDataSetChanged();
            } else Toast.makeText(this, R.string.import_sync_error, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Sync from / import error", e);
            Toast.makeText(this, String.valueOf(e), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Asks user for device, confirmation and deletes it
     */
    private void deleteDevice(String deviceName, boolean confirmed) {
        List<String> deviceNames = vaultUtils.getDeviceNames();

        // Exit if no devices
        if (deviceNames.isEmpty()) return;

        // Ask for device
        if (deviceName == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.delete_device_select_title);

            builder.setItems(deviceNames.toArray(new String[0]), (dialog, which) -> {
                if (which < deviceNames.size()) deleteDevice(deviceNames.get(which), false);
            });
            builder.setNegativeButton(R.string.cancel, null);

            builder.show();
            return;
        }

        // Ask for confirmation
        if (!confirmed) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.delete_device_confirm_title, deviceName));
            builder.setMessage(R.string.delete_device_confirm_description);

            builder.setPositiveButton(R.string.no, null);
            builder.setNegativeButton(R.string.yes, (dialog, which) -> deleteDevice(deviceName, true));
            builder.show();
            return;
        }

        // Try to delete it
        try {
            if (!vaultUtils.deleteDevice(deviceName, vaultPath)) {
                Toast.makeText(this, R.string.delete_device_error, Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting device", e);
            Toast.makeText(this, String.valueOf(e), Toast.LENGTH_LONG).show();
            return;
        }

        // Show confirmation
        Toast.makeText(this, R.string.delete_device_ok, Toast.LENGTH_SHORT).show();

        // Enable delete device button only if there is at least one sync device
        findViewById(R.id.btnDeleteDevice).setEnabled(!vaultUtils.getDeviceNames().isEmpty());
    }

    /**
     * Adds entry
     */
    private void addEntry() {
        Intent intent = new Intent(VaultActivity.this, EditActivity.class);
        intent.putExtra("title", getString(R.string.add_entry));
        intent.putExtra("password", CryptoUtils.generateSecurePassword());
        vaultEditLauncher.launch(intent);
    }

    /**
     * Opens activity to edit / delete entry
     *
     * @param entryIndex entry index from vaultUtils.getVaultEntries()
     */
    private void editEntry(int entryIndex) {
        VaultEntry vaultEntry = vaultUtils.getVaultEntries().get(entryIndex);
        String id = vaultEntry.getId();
        String site = vaultEntry.getSite();
        String user = vaultEntry.getUsername();
        String password = vaultEntry.getPassword();
        String notes = vaultEntry.getNotes();

        if (id == null || id.isEmpty()) return;

        Intent intent = new Intent(VaultActivity.this, EditActivity.class);

        intent.putExtra("id", id);
        if (site != null && !site.isEmpty()) {
            intent.putExtra("title", site);
            intent.putExtra("site", site);
        }
        if (user != null) intent.putExtra("user", user);
        if (password != null) intent.putExtra("password", password);
        if (notes != null) intent.putExtra("notes", notes);
        intent.putExtra("deletable", true);

        vaultEditLauncher.launch(intent);
    }

    /**
     * Shows current mnemonic phrase
     */
    private void showMnemonic() {
        // Build mnemonic activity intent
        Intent intent = new Intent(VaultActivity.this, MnemonicActivity.class);
        intent.putExtra("title", getString(R.string.mnemonic_viewer_title));
        intent.putExtra("description", getString(R.string.mnemonic_viewer_description_view));
        intent.putExtra("mnemonic", vaultUtils.getMnemonic().getMnemonic());
        intent.putExtra("editable", false);

        // Launch it
        startActivity(intent);
    }

    /**
     * Asks for a new vault name and renames vault
     */
    private void rename(String nameNew) {
        // Ask for name
        if (nameNew == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.vault_rename_title);
            builder.setMessage(R.string.vault_rename_description);

            LinearLayout alertLayout = new LinearLayout(this);
            alertLayout.setOrientation(LinearLayout.VERTICAL);

            LayoutInflater inflater = LayoutInflater.from(this);

            LinearLayout dialogInputLayout = (LinearLayout) inflater.inflate(R.layout.text_input_dialog, alertLayout, false);
            TextInputEditText renameVaultText = dialogInputLayout.findViewById(R.id.inputView);

            renameVaultText.setHint(vaultName);
            renameVaultText.setText(vaultName);

            builder.setView(dialogInputLayout);
            alertLayout.addView(dialogInputLayout);
            builder.setView(alertLayout);

            builder.setNegativeButton(R.string.cancel, null);
            builder.setPositiveButton(R.string.ok, (dialog, which) -> rename(String.valueOf(renameVaultText.getText())));
            builder.show();
            return;
        }

        // Exit if empty
        if (nameNew.trim().isEmpty() || nameNew.equals(vaultName)) return;

        // Check if not exists
        VaultUtils.VaultNamePath vaultNamePathTest = vaultUtils.getNamePath(nameNew, null);
        if (vaultNamePathTest != null) {
            Toast.makeText(this, getString(R.string.vault_create_already_exists, nameNew), Toast.LENGTH_SHORT).show();
            return;
        }

        // Try to rename
        try {
            if (vaultUtils.rename(nameNew, vaultPath)) {
                vaultName = nameNew;
                ((TextView) findViewById(R.id.vaultTitle)).setText(nameNew);
                Toast.makeText(this, R.string.vault_rename_ok, Toast.LENGTH_SHORT).show();
            } else Toast.makeText(this, R.string.vault_rename_error, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error renaming vault", e);
            Toast.makeText(this, String.valueOf(e), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Asks for user confirmation to delete vault and deletes vault and finishes activity
     */
    private void delete(boolean confirmed) {
        if (!confirmed) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.vault_delete_confirm_title);
            builder.setMessage(R.string.vault_delete_confirm_description);
            builder.setPositiveButton(R.string.no, null);
            builder.setNegativeButton(R.string.yes, (dialog, which) -> delete(true));
            builder.show();
            return;
        }

        try {
            if (vaultUtils.delete(vaultPath)) {
                Toast.makeText(this, R.string.vault_delete_ok, Toast.LENGTH_SHORT).show();
                finish();
            } else Toast.makeText(this, R.string.vault_delete_error, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Error deleting vault", e);
            Toast.makeText(this, String.valueOf(e), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Closes vault on destroy
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        vaultUtils.close();
    }

    /**
     * Add / edit / delete entry callback
     */
    @SuppressLint("NotifyDataSetChanged")
    private final ActivityResultLauncher<Intent> vaultEditLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            Intent data = result.getData();

            // No data provided
            if (data == null) return;

            // Extract data
            String id, site, user, password, notes;
            if (data.hasExtra("id")) id = data.getStringExtra("id");
            else id = "";
            if (data.hasExtra("site")) site = data.getStringExtra("site");
            else site = "";
            if (data.hasExtra("user")) user = data.getStringExtra("user");
            else user = "";
            if (data.hasExtra("password")) password = data.getStringExtra("password");
            else password = "";
            if (data.hasExtra("notes")) notes = data.getStringExtra("notes");
            else notes = "";
            boolean delete = data.hasExtra("delete") && data.getBooleanExtra("delete", false);

            // No data provided
            if (!delete && (site == null || site.isEmpty()) && (user == null || user.isEmpty())) return;

            try {
                // ID provided (edit or delete)
                if (id != null && !id.isEmpty()) {
                    // Search entry index
                    int entryIndex = vaultUtils.getEntryIndex(id);
                    if (entryIndex == -1) return;

                    // Delete entry
                    if (delete) {
                        Log.i(TAG, "Deleting entry " + id);
                        vaultUtils.getVaultEntries().remove(entryIndex);
                        vaultUtils.save(vaultPath);

                        Toast.makeText(this, R.string.entry_deleted, Toast.LENGTH_SHORT).show();

                        // Update recycler view
                        entriesContainerAdapter.notifyDataSetChanged();
                        return;
                    }

                    // Edit entry

                    Log.i(TAG, "Editing entry " + id);

                    VaultEntry vaultEntry = vaultUtils.getVaultEntries().get(entryIndex);

                    // Nothing to edit
                    if ((site == null || site.equals(vaultEntry.getSite())) && (user == null || user.equals(vaultEntry.getSite())) && (password == null || password.equals(vaultEntry.getPassword())) && (notes == null || notes.equals(vaultEntry.getNotes())))
                        return;

                    // Update entry
                    vaultEntry.setSite(site);
                    vaultEntry.setUsername(user);
                    vaultEntry.setPassword(password);
                    vaultEntry.setNotes(notes);

                    // Save
                    vaultUtils.save(vaultPath);

                    // Update recycler view
                    entriesContainerAdapter.notifyItemChanged(entryIndex);
                    return;
                }

                // Add entry

                Log.i(TAG, "Adding new entry");

                // Add entry (to the top) and save vault
                VaultEntry entry = new VaultEntry(null, site, user, password, notes);
                vaultUtils.getVaultEntries().add(0, entry);
                vaultUtils.save(vaultPath);

                // Update recycler view
//                entriesContainerAdapter.notifyItemInserted(vaultUtils.getVaultEntries().size() - 1);
                entriesContainerAdapter.notifyItemInserted(0);
            } catch (Exception e) {
                Log.e(TAG, "Error adding / editing / deleting entry", e);
                Toast.makeText(this, String.valueOf(e), Toast.LENGTH_LONG).show();
            }
        }
    });

    /**
     * QR scanner (sync from / import) callback
     */
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
        // Scan cancelled
        if (result.getContents() == null || result.getContents().isEmpty()) {
            Toast.makeText(this, R.string.qr_scanner_canceled, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject partialSyncData = new JSONObject("{" + result.getContents() + "}");

            // Extract QR index
            int partIndex;
            if (partialSyncData.has("i")) partIndex = partialSyncData.getInt("i");
            else partIndex = 0;
            int partsTotal;
            if (partialSyncData.has("n")) partsTotal = partialSyncData.getInt("n");
            else partsTotal = 1;

            // Check if already received
            if (receivedParts != null && receivedParts.size() > partIndex && receivedParts.get(partIndex)) {
                Toast.makeText(this, R.string.import_sync_already_received, Toast.LENGTH_SHORT).show();
                syncFrom(true);
                return;
            }

            // Create and fix list if needed
            if (receivedParts == null) receivedParts = new ArrayList<>();
            while (receivedParts.size() < partsTotal) receivedParts.add(false);

            // Create sync data instance if needed
            if (syncFromData == null) syncFromData = new VaultUtils.SyncData(new ArrayList<>(), null);

            // Extract sync salt
            if (partialSyncData.has("salt")) {
                syncFromData.salt = partialSyncData.getString("salt");
                Log.i(TAG, "Received sync salt");
            }

            // Extract actions
            if (partialSyncData.has("acts")) {
                JSONArray actions = partialSyncData.getJSONArray("acts");
                for (int i = 0; i < actions.length(); i++)
                    syncFromData.actions.add(actions.getJSONObject(i).toString().replace("\\/", "/"));
                Log.i(TAG, "Received " + actions.length() + " actions");
            }

            // Mark this part as received
            Toast.makeText(this, getString(R.string.import_sync_received_part, partIndex + 1, partsTotal), Toast.LENGTH_SHORT).show();
            receivedParts.set(partIndex, true);

            // Request next part or finish
            syncFrom(true);
        } catch (Exception e) {
            Log.e(TAG, "Sync from / import data parse error", e);
            Toast.makeText(this, String.valueOf(e), Toast.LENGTH_LONG).show();
        }
    });
}