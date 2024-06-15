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

import org.json.JSONException;
import org.json.JSONObject;

public class VaultEntry {
    private final String id;
    private String site;
    private String username;
    private String password;
    private String notes;

    /**
     * Initializes entry from strings
     *
     * @param id null to generate a new one
     */
    public VaultEntry(String id, String site, String username, String password, String notes) {
        this.site = site;
        this.username = username;
        this.password = password;
        this.notes = notes;

        // Generate unique ID if not provided
        if (id == null || id.isEmpty()) id = CryptoUtils.base64Encode(CryptoUtils.generateRandom(8));
        this.id = id;
    }

    /**
     * Initializes entry from JSON
     */
    public VaultEntry(JSONObject entryJSON) throws JSONException {
        if (entryJSON.has("id")) this.id = entryJSON.getString("id");
        else this.id = CryptoUtils.base64Encode(CryptoUtils.generateRandom(8));

        if (entryJSON.has("site")) this.site = entryJSON.getString("site");
        else this.site = "";

        if (entryJSON.has("user")) this.username = entryJSON.getString("user");
        else this.username = "";

        if (entryJSON.has("pass")) this.password = entryJSON.getString("pass");
        else this.password = "";

        if (entryJSON.has("notes")) this.notes = entryJSON.getString("notes");
        else this.notes = "";
    }

    /**
     * @return entry as JSON (with id, site, username, password and notes keys only if they are not empty)
     */
    public JSONObject getAsJSON() throws JSONException {
        JSONObject entry = new JSONObject();
        entry.put("id", id);

        if (site != null && !site.isEmpty()) entry.put("site", site);
        if (username != null && !username.isEmpty()) entry.put("user", username);
        if (password != null && !password.isEmpty()) entry.put("pass", password);
        if (notes != null && !notes.isEmpty()) entry.put("notes", notes);
        return entry;
    }

    /**
     * @return true if current entry is equal to entry
     */
    public boolean equals(VaultEntry entry) {
        if (entry == null) return false;

        return entry.getId().equals(id) && entry.getSite().equals(site) && entry.getUsername().equals(username) && entry.getPassword().equals(password) && entry.getNotes().equals(notes);
    }

    public String getId() {
        return id;
    }

    public String getSite() {
        return site;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getNotes() {
        return notes;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
