package com.dataart.btle_android.btle_gateway.model;

/**
 * Created by alrybakov
 */
public class BTLECharacteristic {

    public final String device;
    public final String characteristicUUID;
    public final String serviceUUID;

    public BTLECharacteristic(String deviceUUID, String serviceUUID, String characteristicUUID) {
        this.serviceUUID = serviceUUID;
        this.device = deviceUUID;
        this.characteristicUUID = characteristicUUID;
    }
}
