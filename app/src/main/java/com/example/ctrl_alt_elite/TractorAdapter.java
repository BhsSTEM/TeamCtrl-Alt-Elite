package com.example.ctrl_alt_elite;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class TractorAdapter extends RecyclerView.Adapter<TractorAdapter.TractorViewHolder> {

    private List<Tractor> tractorList;

    public TractorAdapter(List<Tractor> tractorList) {
        this.tractorList = tractorList;
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
        holder.txtName.setText(tractor.getName());
        holder.txtYear.setText(holder.itemView.getContext().getString(R.string.year_format, tractor.getYear()));
        holder.txtModel.setText(holder.itemView.getContext().getString(R.string.model_format, tractor.getModel()));
        
        holder.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), AddTractorActivity.class);
            intent.putExtra("TRACTOR_DATA", tractor);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return tractorList.size();
    }

    public static class TractorViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtYear, txtModel;
        MaterialButton btnEdit;

        public TractorViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txt_tractor_name);
            txtYear = itemView.findViewById(R.id.txt_tractor_year);
            txtModel = itemView.findViewById(R.id.txt_tractor_model);
            btnEdit = itemView.findViewById(R.id.btn_edit);
        }
    }
}
