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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class JSONFileUtils {
    private static final String TAG = JSONFileUtils.class.getName();

    /**
     * Reads a JSON object from a specified file in the app's internal storage.
     *
     * @param file file to read the JSON data from
     * @return the JSON object read from the file, or an empty JSON object if the file does not exist or an error occurs
     */
    public static JSONObject readJsonFromFile(File file) {
        JSONObject jsonObject = new JSONObject();
        if (file.exists() && file.length() > 0) {
            Log.i(TAG, "Trying to read JSON from file " + file.getAbsoluteFile());
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                byte[] buffer = new byte[(int) file.length()];
                fileInputStream.read(buffer);
                String jsonContent = new String(buffer, StandardCharsets.UTF_8);
                jsonObject = new JSONObject(jsonContent);
            } catch (IOException | JSONException e) {
                Log.w(TAG, "Unable to load JSON from file: " + file.getAbsoluteFile(), e);
            }
        } else Log.w(TAG, "File " + file.getAbsoluteFile() + " doesn't exist");
        return jsonObject;
    }

    /**
     * Writes a JSON object to a specified file in the app's internal storage.
     *
     * @param file       file to write the JSON data to
     * @param jsonObject the JSON object to be written to the file
     */
    public static void writeJsonToFile(File file, JSONObject jsonObject) {
        Log.i(TAG, "Trying to save JSON as file " + file.getAbsoluteFile());
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            fileOutputStream.write(jsonObject.toString(4).getBytes(StandardCharsets.UTF_8));
        } catch (IOException | JSONException e) {
            Log.w(TAG, "Unable to save JSON to file: " + file.getAbsoluteFile(), e);
        }
    }
}
