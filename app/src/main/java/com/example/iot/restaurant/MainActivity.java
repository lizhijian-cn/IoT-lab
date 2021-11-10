package com.example.iot.restaurant;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.topelec.control.rfid.RfidCommand;
import com.topelec.control.rfid.RfidControl;
import com.topelec.control.zigbee.SensorControl;

public class MainActivity extends AppCompatActivity {

    private RfidControl rfidControl;
    private SensorControl sensorControl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}