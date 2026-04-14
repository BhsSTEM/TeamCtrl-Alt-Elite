package com.example.ctrl_alt_elite;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
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
        holder.txtYear.setText("Year: " + tractor.getYear());
        holder.txtModel.setText("Model: " + tractor.getModel());
    }

    @Override
    public int getItemCount() {
        return tractorList.size();
    }

    static class TractorViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtYear, txtModel;

        public TractorViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txt_tractor_name);
            txtYear = itemView.findViewById(R.id.txt_tractor_year);
            txtModel = itemView.findViewById(R.id.txt_tractor_model);
        }
    }
}
