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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EntriesContainerAdapter extends RecyclerView.Adapter<EntriesContainerAdapter.RowViewHolder> {
    private final List<VaultEntry> vaultEntries;
    public RowClickListener rowClickListener;

    EntriesContainerAdapter(List<VaultEntry> vaultEntries) {
        this.vaultEntries = vaultEntries;
        rowClickListener = null;
    }

    public void setRowClickListener(RowClickListener rowClickListener) {
        this.rowClickListener = rowClickListener;
    }

    @NonNull
    @Override
    public RowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the corresponding layout of the parent item
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.entries_row, parent, false);
        return new RowViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RowViewHolder holder, int position) {
        final int index = holder.getAdapterPosition();
        holder.getSiteView().setText(vaultEntries.get(index).getSite());
        holder.getUsernameView().setText(vaultEntries.get(index).getUsername());
        holder.bindIndex(index);
    }

    @Override
    public int getItemCount() {
        return vaultEntries.size();
    }

    public class RowViewHolder extends RecyclerView.ViewHolder {
        private final TextView site, username;
        public int index;

        RowViewHolder(final View itemView) {
            super(itemView);

            site = itemView.findViewById(R.id.site);
            username = itemView.findViewById(R.id.username);
            index = 0;

            // Set short click listeners
            if (rowClickListener != null) {
                itemView.setOnClickListener(view -> rowClickListener.onRowClick(index));
            }
        }

        public void bindIndex(int index) {
            this.index = index;
        }

        public TextView getSiteView() {
            return site;
        }

        public TextView getUsernameView() {
            return username;
        }
    }
}
