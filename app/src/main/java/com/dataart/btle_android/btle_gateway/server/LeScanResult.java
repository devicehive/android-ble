package com.dataart.btle_android.btle_gateway.server;

import android.bluetooth.BluetoothDevice;

import java.util.Objects;

class LeScanResult {

    private BluetoothDevice mDevice;
    private int mRssi;
    private byte[] mScanRecord;

    public LeScanResult(BluetoothDevice device, int rssi, byte[] scanRecord) {
        mDevice = device;
        mRssi = rssi;
        mScanRecord = scanRecord;
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public int getRssi() {
        return mRssi;
    }

    public byte[] getScanRecord() {
        return mScanRecord;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o instanceof BluetoothDevice) {
            return Objects.equals(this.mDevice.getAddress(), ((BluetoothDevice) o).getAddress());
        }

//            devices equals by address
        if (o instanceof LeScanResult) {
            //noinspection SimplifiableIfStatement
            if (this.mDevice == null || ((LeScanResult) o).mDevice == null)
                return false;

            return Objects.equals(this.mDevice.getAddress(), ((LeScanResult) o).mDevice.getAddress());
        }

        return false;
    }
}
