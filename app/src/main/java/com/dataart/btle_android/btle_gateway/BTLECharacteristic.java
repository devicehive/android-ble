package com.dataart.btle_android.btle_gateway;

/**
 * Created by alrybakov
 */
public class BTLECharacteristic {

    public String device;
    public String characteristicUUID;
    public String serviceUUID;

    public BTLECharacteristic(String deviceUUID, String serviceUUID, String characteristicUUID) {
        this.serviceUUID = serviceUUID;
        this.device = deviceUUID;
        this.characteristicUUID = characteristicUUID;
    }
}
