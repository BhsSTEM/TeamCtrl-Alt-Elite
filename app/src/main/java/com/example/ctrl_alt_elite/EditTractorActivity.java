package com.example.ctrl_alt_elite;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;

public class EditTractorActivity extends BaseActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityContent(R.layout.activity_edit_tractor);
    }
   /* private void pinEntered(){
        new AlertDialog.Builder(context)
                .setTitle("Remove Tractor")
                .setMessage("Do you want to autofill the rest of the info using the data connected to your PIN?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    autoFill();
                })
                .setNegativeButton("No", null)
                .show();
    }
    private void autoFill(){

    }

*/}
