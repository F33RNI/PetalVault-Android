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

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class VaultUtils {
    private static final String TAG = VaultUtils.class.getName();

    private static final String NOT_SAFE_FILENAME_REGEX = "[^a-zA-Z0-9._\\- ]";

    private final File vaultsDir;
    private final ConfigManager configManager;

    private JSONObject vault = new JSONObject();
    private final ArrayList<VaultNamePath> vaultNamePaths = new ArrayList<>();
    private final Mnemonic mnemonic;
    private final ArrayList<VaultEntry> vaultEntries = new ArrayList<>();
    private CryptoUtils.MasterKey masterKey;

    VaultUtils(File vaultsDir, ConfigManager configManager, String[] wordlist) {
        this.vaultsDir = vaultsDir;
        this.configManager = configManager;

        mnemonic = new Mnemonic(wordlist);
    }

    /**
     * Parses names and paths of available vaults and saves them into vaultNamePaths
     */
    public void refreshAvailable() {
        vaultNamePaths.clear();

        JSONArray vaults = (JSONArray) configManager.get("vaults", new JSONArray());
        if (vaults.length() == 0) {
            Log.w(TAG, "No vaults available");
            return;
        }

        for (int i = 0; i < vaults.length(); i++) {
            try {
                String vaultPath = vaults.getString(i);

                // Read vault
                JSONObject vaultTemp = read(vaultPath);

                // Check
                if (vaultTemp == null) continue;

                // Extract name and add to the array list
                vaultNamePaths.add(new VaultNamePath(vaultTemp.getString("name"), vaultPath));
            } catch (JSONException e) {
                Log.w(TAG, "Unable to parse vault", e);
            }
        }

        Log.i(TAG, "Available vaults: " + vaultNamePaths.size());
    }

    public boolean create(String name, String[] mnemonicWords, String password) throws JSONException, RuntimeException {
        Log.i(TAG, "Creating new vault: " + name);

        // Close current vault
        close();

        mnemonic.fromMnemonic(mnemonicWords);

        // Set name and version
        vault.put("name", name);
        vault.put("version", configManager.get("version", "2.0.0"));

        // Encrypt mnemonic if needed
        if (password != null && !password.isEmpty()) {
            CryptoUtils.MnemonicEncrypted mnemonicEncrypted = CryptoUtils.encryptMnemonic(mnemonicWords, password);
            if (mnemonicEncrypted == null) throw new RuntimeException("Mnemonic is null");

            vault.put("mnemonic_encrypted", CryptoUtils.base64Encode(mnemonicEncrypted.mnemonicEncrypted));
            vault.put("mnemonic_salt_1", CryptoUtils.base64Encode(mnemonicEncrypted.salt1));
            vault.put("mnemonic_salt_2", CryptoUtils.base64Encode(mnemonicEncrypted.salt2));
        }

        // Save it
        return save(null);
    }

    /**
     * Reads vault from relPath and checks for required keys
     *
     * @param relPath path to .json, relative to vaultsDir
     * @return vault as JSONObject or null in case of error
     */
    public JSONObject read(String relPath) {
        Log.i(TAG, "Reading vault from: " + relPath);
        try {
            // Read vault
            JSONObject vaultTemp = JSONFileUtils.readJsonFromFile(new File(vaultsDir, relPath));

            // Name and master salt are required
            if (!vaultTemp.has("name") || !vaultTemp.has("master_salt")) return null;

            // Check version
            short versionMajorVault = Short.parseShort(vaultTemp.getString("version").split("\\.")[0].trim());
            short versionMajorApp = Short.parseShort(((String) configManager.get("version", "2.0.0")).split("\\.")[0].trim());
            if (!vaultTemp.has("version") || versionMajorVault < 2 || versionMajorVault > versionMajorApp) return null;

            return vaultTemp;
        } catch (Exception e) {
            Log.e(TAG, "Error opening vault", e);
        }
        return null;
    }

    /**
     * Reads and decrypts vault
     *
     * @param relPath       path to .json, relative to vaultsDir
     * @param mnemonicWords mnemonic phrase
     * @return true if read successfully, false in case of error
     */
    public boolean open(String relPath, String[] mnemonicWords) {
        // Close current vault
        close();

        // Try to read
        vault = read(relPath);
        if (vault == null) {
            vault = new JSONObject();
            close();
            return false;
        }

        try {
            // Build mnemonic
            mnemonic.fromMnemonic(mnemonicWords);

            // Build master key
            masterKey = CryptoUtils.entropyToMasterKey(mnemonic.getEntropy(), CryptoUtils.base64Decode(vault.getString("master_salt")));

            // Decrypt entries
            if (vault.has("entries")) {
                JSONArray entries = vault.getJSONArray("entries");
                for (int i = 0; i < entries.length(); i++) {
                    // Extract and decrypt entry
                    JSONObject entryEncrypted = entries.getJSONObject(i);
                    JSONObject entryDecrypted = CryptoUtils.decryptEntry(entryEncrypted, masterKey.masterKey);
                    if (entryDecrypted == null) throw new RuntimeException("Entry is null");

                    // Add to array list
                    vaultEntries.add(new VaultEntry(entryDecrypted));
                }

                Log.i(TAG, "Decrypted " + entries.length() + " entries");

                // Seems OK
                return true;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error reading vault", e);
            close();
        }
        return false;
    }

    /**
     * Saves current vault
     *
     * @param relPath existing path or null to create a new one
     * @return true if saved successfully
     */
    public boolean save(String relPath) throws JSONException {
        return save(relPath, true);
    }

    /**
     * Exports / syncs data to device
     *
     * @param deviceName name of existing device / new device or null to just export
     * @param vaultPath  relative path of current vault (for saving)
     * @return actions, sync salt
     */
    public SyncData syncTo(String deviceName, String vaultPath) throws JSONException, RuntimeException {
        // Ignore if no vault was opened or no entries
        if (!vault.has("name") || vaultPath == null || vaultPath.isEmpty() || vaultEntries.isEmpty()) return null;

        JSONObject devices = null;
        JSONArray deviceEntries = null;
        JSONObject deviceEntriesAndSalt = null;
        byte[] deviceSalt = null;
        byte[] deviceMasterKey = null;

        // Existing or new device (sync to)
        if (deviceName != null && !deviceName.isEmpty()) {
            devices = getDevices();

            // New device
            if (!devices.has(deviceName)) devices.put(deviceName, new JSONObject());

            // Extract entries and salt
            deviceEntriesAndSalt = devices.getJSONObject(deviceName);
            if (deviceEntriesAndSalt.has("entries")) deviceEntries = deviceEntriesAndSalt.getJSONArray("entries");
            if (deviceEntriesAndSalt.has("salt"))
                deviceSalt = CryptoUtils.base64Decode(deviceEntriesAndSalt.getString("salt"));
        }

        // Build device key from it's salt and main mnemonic
        if (deviceSalt != null)
            deviceMasterKey = CryptoUtils.entropyToMasterKey(mnemonic.getEntropy(), deviceSalt).masterKey;

        // Decrypt all device entries
        ArrayList<VaultEntry> deviceEntriesDecrypted = new ArrayList<>();
        if (deviceEntries != null && deviceMasterKey != null) {
            for (int i = 0; i < deviceEntries.length(); i++) {
                JSONObject deviceEntry = deviceEntries.getJSONObject(i);
                JSONObject deviceEntryDecryptedJSON = CryptoUtils.decryptEntry(deviceEntry, deviceMasterKey);
                if (deviceEntryDecryptedJSON == null) throw new RuntimeException("Unable to decrypt device entry");
                VaultEntry deviceEntryDecrypted = new VaultEntry(deviceEntryDecryptedJSON);
                deviceEntriesDecrypted.add(deviceEntryDecrypted);
            }
        }

        // Build array lists of IDs
        ArrayList<String> entryIds = new ArrayList<>();
        for (VaultEntry entry : vaultEntries)
            entryIds.add(entry.getId());
        ArrayList<String> deviceEntryIds = new ArrayList<>();
        for (VaultEntry entry : deviceEntriesDecrypted)
            deviceEntryIds.add(entry.getId());

        ArrayList<String> syncActions = new ArrayList<>();

        // Build array of sync actions starting from delete entries
        for (String id : deviceEntryIds) {
            if (!entryIds.contains(id)) {
                JSONObject action = new JSONObject();
                action.put("act", "delete");
                action.put("id", id);
                syncActions.add(action.toString().replace("\\/", "/"));
            }
        }

        // Generate master sync key from vault's mnemonic
        CryptoUtils.MasterKey syncKey = CryptoUtils.entropyToMasterKey(mnemonic.getEntropy(), null);
        String syncSaltBase64 = CryptoUtils.base64Encode(syncKey.salt);

        // Revers vault's entry IDs because we need to keep order on new device
        Collections.reverse(entryIds);

        // Add non-existing entries actions and sync actions (from bottom to top)
        for (String id : entryIds) {
            VaultEntry entry = getEntryByID(id);
            VaultEntry deviceEntry = getEntryByID(id, deviceEntriesDecrypted);

            // Just in case
            if (entry == null) {
                Log.w(TAG, "Unable to find entry by ID: " + id);
                continue;
            }

            // Nothing to sync
            if (deviceEntry != null && deviceEntry.equals(entry)) continue;

            // Encrypt
            JSONObject encryptedEntry = CryptoUtils.encryptEntry(entry.getAsJSON(), syncKey.masterKey);
            if (encryptedEntry == null) throw new RuntimeException("Unable to encrypt entry");

            //"add" action in case of new entry, "sync" if exists
            encryptedEntry.put("act", deviceEntry == null ? "add" : "sync");
            syncActions.add(encryptedEntry.toString().replace("\\/", "/"));
        }

        // Check if we have anything to sync
        if (syncActions.isEmpty()) {
            Log.i(TAG, "Nothing to sync/export");
            return null;
        }

        if (deviceName != null && !deviceName.isEmpty()) {
            // Remove previous entries
            if (deviceEntriesAndSalt.has("entries")) deviceEntriesAndSalt.remove("entries");

            // Encrypt and add entries to device
            JSONArray deviceEntriesEncrypted = new JSONArray();
            for (VaultEntry entry : vaultEntries)
                deviceEntriesEncrypted.put(CryptoUtils.encryptEntry(entry.getAsJSON(), syncKey.masterKey));
            deviceEntriesAndSalt.put("entries", deviceEntriesEncrypted);

            // Add salt
            deviceEntriesAndSalt.put("salt", syncSaltBase64);

            // Add devices
            vault.put("devices", devices);

            // Save without re-encrypting
            if (!save(vaultPath, false)) return null;
        }

        return new SyncData(syncActions, syncSaltBase64);
    }

    /**
     * Syncs vault
     *
     * @param syncData  sync salt and actions
     * @param vaultPath where to save vault (relative path)
     * @return true if synced successfully
     */
    public boolean syncFrom(SyncData syncData, String vaultPath) throws JSONException, RuntimeException {
        // Exit if no data
        if (syncData == null || syncData.salt == null || syncData.actions == null || syncData.actions.isEmpty())
            return false;

        // Build sync key
        byte[] syncKey = CryptoUtils.entropyToMasterKey(mnemonic.getEntropy(), CryptoUtils.base64Decode(syncData.salt)).masterKey;

        for (int i = 0; i < syncData.actions.size(); i++) {
            JSONObject actionData = new JSONObject(syncData.actions.get(i));
            String action = actionData.getString("act");
            Log.i(TAG, "Action: " + action);

            // Delete entry
            if (action.equals("delete") && actionData.has("id")) {
                String id = actionData.getString("id");
                VaultEntry entryToDelete = getEntryByID(id);
                if (entryToDelete == null) continue;

                Log.i(TAG, "Deleting entry " + id);
                vaultEntries.remove(entryToDelete);
                continue;
            }

            // Add or sync
            if ((action.equals("add") || action.equals("sync")) && actionData.has("enc") && actionData.has("iv")) {
                JSONObject entryEncrypted = new JSONObject();
                entryEncrypted.put("enc", actionData.getString("enc"));
                entryEncrypted.put("iv", actionData.getString("iv"));

                // Decrypt entry from action
                JSONObject entryDecrypted = CryptoUtils.decryptEntry(entryEncrypted, syncKey);
                if (entryDecrypted == null || !entryDecrypted.has("id")) continue;

                String id = entryDecrypted.getString("id");

                // Extract data
                String site, user, pass, notes;
                if (entryDecrypted.has("site")) site = entryDecrypted.getString("site");
                else site = null;
                if (entryDecrypted.has("user")) user = entryDecrypted.getString("user");
                else user = null;
                if (entryDecrypted.has("pass")) pass = entryDecrypted.getString("pass");
                else pass = null;
                if (entryDecrypted.has("notes")) notes = entryDecrypted.getString("notes");
                else notes = null;

                // Search for it
                VaultEntry entry = getEntryByID(id);

                // Sync
                if (entry != null) {
                    if (site != null) entry.setSite(site);
                    if (user != null) entry.setUsername(user);
                    if (pass != null) entry.setPassword(pass);
                    if (notes != null) entry.setNotes(notes);
                }

                // Add
                else {
                    if (site == null) site = "";
                    if (user == null) user = "";
                    if (pass == null) pass = "";
                    if (notes == null) notes = "";
                    entry = new VaultEntry(id, site, user, pass, notes);
                    vaultEntries.add(0, entry);
                }
            }
        }

        // Seems OK -> save vault with re-encryption
        return save(vaultPath);
    }

    /**
     * Saves current vault
     *
     * @param relPath   existing path or null to create a new one
     * @param reEncrypt true to rotate encryption, false to not. NOTE: New entries will not be saved if false!
     * @return true if saved successfully
     */
    public boolean save(String relPath, boolean reEncrypt) throws JSONException {
        // Ignore if no vault was opened
        if (!vault.has("name")) return false;

        // Generate new master key
        if (reEncrypt) {
            masterKey = CryptoUtils.entropyToMasterKey(mnemonic.getEntropy(), null);
            vault.put("master_salt", CryptoUtils.base64Encode(masterKey.salt));
        }

        // Create VAULTS_DIR if needed
        if (!vaultsDir.exists()) {
            Log.i(TAG, "Creating " + vaultsDir.getAbsoluteFile() + " directory");
            if (!vaultsDir.mkdirs()) return false;
        }

        String vaultName = vault.getString("name");

        // Build safe filename
        if (relPath == null || relPath.isEmpty()) {
            StringBuilder filenameSafe = new StringBuilder(vaultName.trim().replaceAll(NOT_SAFE_FILENAME_REGEX, ""));
            if (filenameSafe.length() == 0) filenameSafe.append("_");

            // Make unique filename
            while (true) {
                File vaultFile = new File(vaultsDir, filenameSafe + ".json");
                if (!vaultFile.exists()) break;
                filenameSafe.append("_");
            }
            filenameSafe.append(".json");
            relPath = filenameSafe.toString();
        }

        // Save entries
        if (reEncrypt) {
            // Remove encrypted entries because we need to rotate master key
            vault.remove("entries");

            // Encrypt each entry
            JSONArray entriesEncrypted = new JSONArray();
            for (int i = 0; i < vaultEntries.size(); i++) {
                try {
                    JSONObject entryJSON = vaultEntries.get(i).getAsJSON();
                    entriesEncrypted.put(CryptoUtils.encryptEntry(entryJSON, masterKey.masterKey));
                } catch (Exception e) {
                    Log.w(TAG, "Error encrypting entry", e);
                }
            }
            Log.i(TAG, "Encrypted " + entriesEncrypted.length() + " entries");
            vault.put("entries", entriesEncrypted);
        }

        // Finally, save file
        JSONFileUtils.writeJsonToFile(new File(vaultsDir, relPath), vault);

        // Add to the existing vaults if not exists
        if (getNamePath(vaultName, relPath) == null) {
            vaultNamePaths.add(new VaultNamePath(vaultName, relPath));
            JSONArray vaultPaths = (JSONArray) configManager.get("vaults", new JSONArray());
            vaultPaths.put(relPath);
            configManager.set("vaults", vaultPaths);
        }

        return true;
    }

    /**
     * Renames vault and saves it without re-encrypting
     *
     * @param nameNew   new name
     * @param vaultPath relative path
     * @return true if renamed successfully
     */
    public boolean rename(String nameNew, String vaultPath) throws JSONException {
        if (nameNew == null || nameNew.isEmpty() || vaultPath == null || vaultPath.isEmpty()) return false;

        // Ignore if no vault was opened
        if (!vault.has("name")) return false;
        VaultNamePath namePath = getNamePath(null, vaultPath);
        if (namePath == null) return false;

        // Update locally
        vault.put("name", nameNew);

        // Update in vaultNamePaths
        namePath.name = nameNew;

        // Save
        if (!save(vaultPath, false)) return false;

        // Update in config
        JSONArray vaultPaths = (JSONArray) configManager.get("vaults", new JSONArray());
        for (int i = 0; i < vaultPaths.length(); i++) {
            String vaultPath_ = vaultPaths.getString(i);
            if (vaultPath_.equals(vaultPath)) {
                vaultPaths.put(i, vaultPath);
                break;
            }
        }
        configManager.set("vaults", vaultPaths);

        // Seems OK
        return true;
    }

    /**
     * Deletes and closes vault
     *
     * @param vaultPath relative path
     * @return true if deleted successfully
     */
    public boolean delete(String vaultPath) throws JSONException {
        // Ignore if no vault was opened
        if (!vault.has("name")) return false;
        VaultNamePath namePath = getNamePath(null, vaultPath);
        if (namePath == null) return false;

        // Delete from vaultNamePaths
        vaultNamePaths.remove(namePath);

        // Delete from config
        JSONArray vaultPaths = (JSONArray) configManager.get("vaults", new JSONArray());
        for (int i = 0; i < vaultPaths.length(); i++) {
            String vaultPath_ = vaultPaths.getString(i);
            if (vaultPath_.equals(vaultPath)) {
                vaultPaths.remove(i);
                break;
            }
        }
        configManager.set("vaults", vaultPaths);

        // Finally, close vault
        close();

        return true;
    }

    /**
     * Closes current vault
     */
    public void close() {
        // Clear internal JSON
        Iterator<String> keys = vault.keys();
        try {
            while (keys.hasNext()) vault.remove(vault.keys().next());
        } catch (Exception e) {
            Log.w(TAG, "Unable to clean current vault", e);
        }

        // Reset private variables
        mnemonic.generateRandom();
        vaultEntries.clear();
        masterKey = null;

        // Run garbage collector
        System.gc();
    }

    /**
     * Checks if entry exists by name, relative path or both
     *
     * @param name    name of the vault or null to ignore
     * @param relPath vault's relative path or null to ignore
     * @return VaultNamePath instance if it's exists or null if not
     */
    public VaultNamePath getNamePath(String name, String relPath) {
        for (int i = 0; i < vaultNamePaths.size(); i++) {
            if (name != null && relPath != null && !name.isEmpty() && !relPath.isEmpty() && vaultNamePaths.get(i).name.equals(name) && vaultNamePaths.get(i).relPath.equals(relPath))
                return vaultNamePaths.get(i);
            else if (name != null && !name.isEmpty() && vaultNamePaths.get(i).name.equals(name))
                return vaultNamePaths.get(i);
            else if (relPath != null && !relPath.isEmpty() && vaultNamePaths.get(i).relPath.equals(relPath))
                return vaultNamePaths.get(i);
        }
        return null;
    }

    /**
     * Searches for entry's index by it's ID
     *
     * @param id unique ID
     * @return index in vaultEntries or -1 if not found
     */
    public int getEntryIndex(String id) {
        if (id == null || id.isEmpty()) return -1;

        for (int i = 0; i < vaultEntries.size(); i++) {
            VaultEntry vaultEntry = vaultEntries.get(i);
            if (vaultEntry.getId().equals(id)) return i;
        }

        return -1;
    }

    /**
     * Searches for entry from vaultEntries by it's ID
     *
     * @param id unique ID
     * @return entry from vaultEntries or null if not found
     */
    public VaultEntry getEntryByID(String id) {
        return getEntryByID(id, vaultEntries);
    }

    /**
     * Searches for entry from entries by it's ID
     *
     * @param id      unique ID
     * @param entries entries to use instead of vaultEntries
     * @return entry from entries or null if not found
     */
    public VaultEntry getEntryByID(String id, ArrayList<VaultEntry> entries) {
        if (id == null || id.isEmpty()) return null;

        for (int i = 0; i < entries.size(); i++) {
            VaultEntry vaultEntry = entries.get(i);
            if (vaultEntry.getId().equals(id)) return vaultEntry;
        }

        return null;
    }

    /**
     * Deletes device by it's name
     *
     * @param deviceName device name
     * @param vaultPath  relative path to vault (to save it)
     * @return true if deleted successfully
     */
    public boolean deleteDevice(String deviceName, String vaultPath) throws JSONException {
        JSONObject devices = getDevices();
        if (!devices.has(deviceName)) return false;

        // Delete and save without re-encrypting
        devices.remove(deviceName);
        return save(vaultPath, false);
    }

    /**
     * Reads devices object from vault or returns new JSON object without putting it into vault
     *
     * @return device objects
     */
    public JSONObject getDevices() throws JSONException {
        if (vault.has("devices")) return vault.getJSONObject("devices");
        else return new JSONObject();
    }

    /**
     * @return list of device names
     */
    public List<String> getDeviceNames() {
        List<String> deviceList = new ArrayList<>();
        try {
            JSONObject object = new JSONObject(getDevices().toString());
            Iterator<String> stringIterator = object.keys();
            while (stringIterator.hasNext()) deviceList.add(stringIterator.next());
        } catch (JSONException e) {
            Log.w(TAG, "Error retrieving devices list", e);
        }
        return deviceList;
    }

    /**
     * @return available vaults. Call refreshAvailable() to refresh this
     */
    public ArrayList<VaultNamePath> getVaultNamePaths() {
        return vaultNamePaths;
    }

    /**
     * @return available entries. Call save() to save them
     */
    public ArrayList<VaultEntry> getVaultEntries() {
        return vaultEntries;
    }

    /**
     * @return mnemonic of currently opened vault
     */
    public Mnemonic getMnemonic() {
        return mnemonic;
    }

    // Utility class for storing vault name and path
    public static class VaultNamePath {
        public String name, relPath;

        VaultNamePath(String name, String relPath) {
            this.name = name;
            this.relPath = relPath;
        }
    }

    // Utility class for storing sync actions as JSON->String and sync salt
    public static class SyncData {
        public final ArrayList<String> actions;
        public String salt;

        public SyncData(ArrayList<String> actions, String salt) {
            this.actions = actions;
            this.salt = salt;
        }
    }
}
