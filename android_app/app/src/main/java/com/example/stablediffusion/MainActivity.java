package com.example.stablediffusion;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("stable-diffusion");
    }

    private EditText promptEditText;
    private EditText stepsEditText;
    private EditText cfgScaleEditText;
    private Spinner samplerSpinner;
    private Spinner schedulerSpinner;
    private EditText denoiseEditText;
    private Switch hiresFixSwitch;
    private Button generateButton;
    private ProgressBar progressBar;
    private ImageView resultImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        promptEditText = findViewById(R.id.prompt);
        stepsEditText = findViewById(R.id.steps);
        cfgScaleEditText = findViewById(R.id.cfg_scale);
        samplerSpinner = findViewById(R.id.sampler);
        schedulerSpinner = findViewById(R.id.scheduler);
        denoiseEditText = findViewById(R.id.denoise);
        hiresFixSwitch = findViewById(R.id.hires_fix);
        generateButton = findViewById(R.id.generate_button);
        progressBar = findViewById(R.id.progress_bar);
        resultImageView = findViewById(R.id.result_image);

        // Setup spinners
        ArrayAdapter<CharSequence> samplerAdapter = ArrayAdapter.createFromResource(this,
                R.array.sampler_options, android.R.layout.simple_spinner_item);
        samplerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        samplerSpinner.setAdapter(samplerAdapter);

        ArrayAdapter<CharSequence> schedulerAdapter = ArrayAdapter.createFromResource(this,
                R.array.scheduler_options, android.R.layout.simple_spinner_item);
        schedulerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        schedulerSpinner.setAdapter(schedulerAdapter);

        generateButton.setOnClickListener(v -> {
            generateImage();
        });
    }

    private void generateImage() {
        String prompt = promptEditText.getText().toString();
        int steps = Integer.parseInt(stepsEditText.getText().toString());
        float cfgScale = Float.parseFloat(cfgScaleEditText.getText().toString());
        String sampler = samplerSpinner.getSelectedItem().toString();
        String scheduler = schedulerSpinner.getSelectedItem().toString();
        float denoise = Float.parseFloat(denoiseEditText.getText().toString());
        boolean hiresFix = hiresFixSwitch.isChecked();
        String loraDir = "/sdcard/stable-diffusion/lora/";


        progressBar.setVisibility(View.VISIBLE);
        generateButton.setEnabled(false);

        new Thread(() -> {
            final Bitmap resultBitmap = generateImageBitmap(prompt, cfgScale, steps, sampler, scheduler, denoise, hiresFix, loraDir);

            runOnUiThread(() -> {
                if (resultBitmap != null) {
                    resultImageView.setImageBitmap(resultBitmap);
                } else {
                    Toast.makeText(MainActivity.this, "Failed to generate image", Toast.LENGTH_SHORT).show();
                }
                progressBar.setVisibility(View.GONE);
                generateButton.setEnabled(true);
            });
        }).start();
    }

    public native Bitmap generateImageBitmap(String prompt, float cfgScale, int steps, String sampler, String scheduler, float denoise, boolean hiresFix, String loraDir);
}
