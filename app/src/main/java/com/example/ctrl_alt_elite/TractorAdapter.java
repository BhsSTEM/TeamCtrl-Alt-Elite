package com.example.ctrl_alt_elite;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class TractorAdapter extends RecyclerView.Adapter<TractorAdapter.TractorViewHolder> {

    private List<Tractor> tractorList;
    private boolean isMapContext;

    public TractorAdapter(List<Tractor> tractorList) {
        this(tractorList, false);
    }

    public TractorAdapter(List<Tractor> tractorList, boolean isMapContext) {
        this.tractorList = tractorList;
        this.isMapContext = isMapContext;
    }

    @NonNull
    @Override
    public TractorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tracter, parent, false);
        return new TractorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TractorViewHolder holder, int position) {
        Tractor tractor = tractorList.get(position);
        Context context = holder.itemView.getContext();

        holder.txtName.setText(tractor.getName());
        holder.txtYear.setText(context.getString(R.string.year_format, String.valueOf(tractor.getYear())));
        holder.txtModel.setText(context.getString(R.string.model_format, tractor.getModel()));
        
        // Load image using Glide
        String imageUrl = tractor.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("link")) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.pngimg_com___tractor_png101303_removebg_preview)
                    .into(holder.imgTractor);
        } else {
            holder.imgTractor.setImageResource(R.drawable.pngimg_com___tractor_png101303_removebg_preview);
        }

        // Edit button is always visible
        holder.btnEdit.setVisibility(View.VISIBLE);
        holder.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, AddTractorActivity.class);
            intent.putExtra("TRACTOR_DATA", tractor);
            context.startActivity(intent);
        });

        if (isMapContext) {
            // Map context: Show Map button, Hide Remove button
            holder.btnMap.setVisibility(View.VISIBLE);
            holder.btnRemove.setVisibility(View.GONE);

            holder.btnMap.setOnClickListener(v -> {
                if (context instanceof evansMapActivity) {
                    ((evansMapActivity) context).showTractorOnMap(tractor);
                }
            });
        } else {
            // Manage context: Hide Map button, Show Remove button
            holder.btnMap.setVisibility(View.GONE);
            holder.btnRemove.setVisibility(View.VISIBLE);

            holder.btnRemove.setOnClickListener(v -> {
                showDeleteConfirmationDialog(context, tractor);
            });
        }
    }

    private void showDeleteConfirmationDialog(Context context, Tractor tractor) {
        new AlertDialog.Builder(context)
                .setTitle("Remove Tractor")
                .setMessage("Are you sure you want to remove the tractor permanently? This action cannot be undone.")
                .setPositiveButton("Remove", (dialog, which) -> {
                    deleteTractorFromFirestore(context, tractor);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteTractorFromFirestore(Context context, Tractor tractor) {
        if (tractor.getDocumentId() == null) {
            Toast.makeText(context, "Error: Tractor ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore.getInstance().collection("tractors")
                .document(tractor.getDocumentId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Tractor removed successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to remove tractor: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return tractorList.size();
    }

    public static class TractorViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtYear, txtModel;
        ImageView imgTractor;
        MaterialButton btnEdit, btnRemove, btnMap;

        public TractorViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txt_tractor_name);
            txtYear = itemView.findViewById(R.id.txt_tractor_year);
            txtModel = itemView.findViewById(R.id.txt_tractor_model);
            imgTractor = itemView.findViewById(R.id.img_tractor);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnRemove = itemView.findViewById(R.id.btn_remove);
            btnMap = itemView.findViewById(R.id.btn_map);
        }
    }
}
