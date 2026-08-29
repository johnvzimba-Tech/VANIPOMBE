package com.vanipombe.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

public class FormActivity extends Activity {
    private EditText etName, etPhone, etNotes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etNotes = findViewById(R.id.etNotes);
        Button btn = findViewById(R.id.btnSubmit);

        btn.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String notes = etNotes.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a name.", Toast.LENGTH_SHORT).show();
                return;
            }

            String entry = "Name: " + name + "\nPhone: " + phone + "\nNotes: " + notes + "\n---\n";

            try (FileOutputStream fos = openFileOutput("submissions.txt", MODE_APPEND);
                 OutputStreamWriter osw = new OutputStreamWriter(fos)) {
                osw.write(entry);
            } catch (Exception e) {
                Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
            etName.setText("");
            etPhone.setText("");
            etNotes.setText("");
        });
    }
}
