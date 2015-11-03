**DeviceHive Android Gateway for BLE devices**
--------------------------------

DeviceHive Android Gateway for Bluetooth Low Energy devices makes it possible to connect multiple Bluetooth Low Energy devices to DeviceHive IoT clouds through single Android device. 
Just imagine multiple sensors, or buttons, or indicators, connected to your smart home through single Android phone, tablet or other device! All your need - just start Gateway, connect to your device and send a command (or subscribe for sensor data notifications).

**Commands**
--------

To achieve such interconnection between your devices and DeviceHive, you can use such commands. They should be sent to DeviceHive IoT cloud (with Admin Panel for example) through Gateway to perform certain BLE actions that conform to Generic Access Protocol:

**scan** - Start scanning for bluetooth devices with automatic timeout 

**scan/start, scan/stop** - Start and stop scanning manually. Can be used in manual demos or automatical scripts that detect whether necessary device is discovered and scanning can be finished

**gatt/connect, gatt/disconnect** - Connect to device that is discovered, disconnect from connected device. Use json with such format as parameter for this command:

    {"device":"A1:A2:A3:A4:A5:A6"}

where "A1:A2:A3:A4:A5:A6" is address of your BLE device.  
Actually Gateway supports autoconnect - if device is discovered, but wasn't connected, call to any command will initiate connection to the device, and if it succeeds, perform command on that connection.

**gatt/primary** - get list of GATT services. Usually BLE device provides certain services that gives access to main functionality. 
Use the same format for json parameter:

    {"device":"A1:A2:A3:A4:A5:A6"}

where "A1:A2:A3:A4:A5:A6" is address of your BLE device.

**gatt/characteristics** - get list of GATT characteristics for selected service. Specify device address and serviceUUID in json parameter:

    {"device":"A1:A2:A3:A4:A5:A6", "serviceUUID":"AA00"}

Notice that Android BTLE Gateway understands short UUID format ("AA00") and translates it to full UUID seamlessly. Of course, you can use long format too, if you feel more comfortable with such numbers like "F00000A1-0451-4000-B000-000000000000".

**gatt/read** - Read value from characteristic:

    {"device":"A1:A2:A3:A4:A5:A6", "serviceUUID":"AA00", "characteristicUUID":"AA01"}

You must point deviceID and serviceUUID because Gateway supports multiple connections - and each command should determine exact destination without ambiguity. Result json will contain value and status:

    {"value":"0x9C, 0x00, 0x10, 0x5A", 
    "device":"A1:A2:A3:A4:A5:A6", "serviceUUID":"AA00", "characteristicUUID":"AA01"
    "status":"success"}

**gatt/write** - Write value to specified characteristic. Uses the same format as read, but requires value to be defined (otherwise fails with warning that user must define value key for this command):

    {"device":"A1:A2:A3:A4:A5:A6", "serviceUUID":"AA00", "characteristicUUID":"AA01", "value":"01"}

**gatt/notifications, gatt/notifications/stop** - Subscribe for notifications from device, unsubscribe. These notifications will be stacked in the "Notifications" section in DeviceHive Admin Panel. Use such json parameter format:

    {"device":"A1:A2:A3:A4:A5:A6", "serviceUUID":"AA00", "characteristicUUID":"AA01"}

Because notifications work for exact characteristic, you don't need to write any boilerplate commands, Gateway will perform this job for you.

**Usage with Freeboard**
--------------------

You can use [Dashboard](https://github.com/devicehive/freeboard) to display live data from DeviceHive notifications. 
Just checkout latest version of freeboard with devicehive plugin, start "index.html" and load there [dashboard-sensortag-temp.json](https://github.com/devicehive/android-ble/blob/master/dashboard-sensortag-temp.json) from DeviceHive Android Gateway for BLE devices project root. Then just retrieve api_key from DeviceHive Admin Panel and insert it to Dashboard settings.   
  
Enable characteristic you want to be read, for example by writing there 0x01:  
**gatt/write**  
`    
{"device":"A1:A2:A3:A4:A5:A6", "serviceUUID":"AA00", "characteristicUUID":"AA02", "value":"01"}
`  
  
Enable notifications:  
**gatt/notifications**  
`    
{"device":"A1:A2:A3:A4:A5:A6", "serviceUUID":"AA00", "characteristicUUID":"AA01"}
`   

Viola! As soon as you subscribe for notifications, they will update Dashboard displays in realtime.  
  
When you need unsubscribe, use **gatt/notifications/stop** command with the same json as for subscription.

**Media materials**
---------------

Watch this [Official DeviceHive Video](https://youtu.be/4i-_m0A6uxk) which shows live usage of DeviceHive Android Gateway for BLE devices with Texas Instruments CC2650 SensorTag

If something seems still unclear - please, seek for answer in this [Presentation with live use cases](http://prezi.com/m-u42rgdqaan/?utm_campaign=share&utm_medium=copy&rc=ex0share), explaining usage of DeviceHive Android Gateway for BLE devices with Texas Instruments CC2650 SensorTag from the previous video.
 

**DeviceHive license**
------------------

DeviceHive is developed by DataArt Apps and distributed under Open Source MIT license. This basically means you can do whatever you want with the software as long as the copyright notice is included. This also means you don't have to contribute the end product or modified sources back to Open Source, but if you feel like sharing, you are highly encouraged to do so!

© Copyright 2015 DataArt Apps · All Rights Reserved