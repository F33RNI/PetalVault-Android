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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class ConfigManager {
    private static final String TAG = ConfigManager.class.getName();

    private final File configFile;
    private final String version;
    private JSONObject config;

    /**
     * Initializes ConfigManager instance
     *
     * @param configFile new File(context.getFilesDir(), ...)
     * @param version    current data format version (must be >2.0.0)
     */
    public ConfigManager(File configFile, String version) {
        this.configFile = configFile;
        this.version = version;
        refresh();
    }

    /**
     * Refreshes config from file
     */
    public void refresh() {
        this.config = JSONFileUtils.readJsonFromFile(this.configFile);

        // Put version
        try {
            this.config.put("version", version);
        } catch (JSONException e) {
            Log.e(TAG, "Unable to put version into config", e);
        }
    }

    /**
     * Retrieves value from config by key
     *
     * @param key          config key to get value of
     * @param defaultValue value to return if key doesn't exists in config
     * @return key's value or defaultValue
     */
    public Object get(String key, Object defaultValue) {
        // Retrieve from config
        if (config.has(key)) {
            try {
                return config.get(key);
            } catch (JSONException e) {
                Log.w(TAG, "Error getting value for key: " + key, e);
            }
        }

        // No key -> return default value
        return defaultValue;
    }

    /**
     * Sets config key to value and saves config file
     *
     * @param key   config key
     * @param value key's value
     */
    public void set(String key, Object value) {
        try {
            config.put(key, value);
            saveConfigToFile();
        } catch (JSONException e) {
            Log.w(TAG, "Error setting value for key: " + key, e);
        }
    }

    /**
     * Saves config to the JSON file
     */
    private void saveConfigToFile() {
        JSONFileUtils.writeJsonToFile(configFile, config);
    }
}

