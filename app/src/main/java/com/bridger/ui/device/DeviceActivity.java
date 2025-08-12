package com.bridger.ui.device;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bridger.R;

public class DeviceActivity extends AppCompatActivity {

    public static final String EXTRA_DEVICE_ADDRESS = "device_address";
    public static final String EXTRA_DEVICE_NAME = "device_name"; // Add for displaying the name

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        String deviceAddress = getIntent().getStringExtra(EXTRA_DEVICE_ADDRESS);
        String deviceName = getIntent().getStringExtra(EXTRA_DEVICE_NAME); // Получаем имя

        if (deviceAddress == null) {
            Toast.makeText(this, "Device address not provided.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView deviceNameTextView = findViewById(R.id.device_name_text_view);
        TextView deviceAddressTextView = findViewById(R.id.device_address_text_view);

        deviceNameTextView.setText(deviceName != null && !deviceName.isEmpty() ? deviceName : "Unknown Device");
        deviceAddressTextView.setText(deviceAddress);

        // TODO: Implement BLE service discovery and display
    }
}
