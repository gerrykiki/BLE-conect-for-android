package com.wistron.gerry.myapplication;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Toast;


import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {



    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBLEdevice;
    private BluetoothGatt mGatt;
    private BluetoothLeScanner bluetoothLeScanner;
    //final static private UUID mHeartRateServiceUuid = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    //final static private UUID mOx = UUID.fromString("00001822-0000-1000-8000-00805f9b34fb");
    final static private UUID NORDIC_UART_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    final static private UUID RX_CHARACTERISTIC = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    final static private UUID TX_CHARACTERISTIC = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    void buttonOnClick(View view) {

        BluetoothGattService service = mGatt.getService(NORDIC_UART_SERVICE);
        BluetoothGattCharacteristic txCharacteristic = service.getCharacteristic(TX_CHARACTERISTIC);
        initCharacteristicReading(mGatt,txCharacteristic);
        System.out.println("Button click");
    }

    private void notice(){

        BluetoothGattService service = mGatt.getService(NORDIC_UART_SERVICE);
        BluetoothGattCharacteristic txCharacteristic = service.getCharacteristic(TX_CHARACTERISTIC);
        initCharacteristicReading(mGatt,txCharacteristic);
        System.out.println("Button click");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            finish();
        }
        // 初始化蓝牙适配器
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        // 确保蓝牙在设备上可以开启
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //System.out.println("Callback");
        if (!mBluetoothAdapter.isEnabled()){
            final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent,REQUEST_ENABLE_BT);
            System.out.println("mBluetoothAdapter is not enable");
            return;
        }
        int permission = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            //還沒獲取權限要做什麼呢

            //和使用者要求權限
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);
        }else{
            //以獲取權限要做的事情
            System.out.println("ACCESS_COARSE_LOCATION is enable");
            //Toast.makeText(this, "已經拿到權限囉!", Toast.LENGTH_SHORT).show();
        }
        ScanFunction(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == REQUEST_ENABLE_BT){
            if (resultCode == Activity.RESULT_CANCELED){
                finish();
            }
            else {

            }
        }
        super.onActivityResult(requestCode,resultCode,data);
    }

    public static int[] createDataOfWatchSetAutoResult() {
        int[] data = {
                0xf6,
                1,  // 0: off; 1: on
                0
        };
        data[2] = (data[0] + data[1]) & 0xff;

        return data;
    }

    private void ScanFunction(boolean enable){
        if (enable){
            System.out.println("Scan Functon enable");

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                System.out.println("Go LOLLIPOP");
                bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
                bluetoothLeScanner.startScan(scanCallback);
            }


            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            Set<BluetoothDevice> devices = adapter.getBondedDevices();
            for(int i=0; i<devices.size(); i++)
            {
                BluetoothDevice device = (BluetoothDevice) devices.iterator().next();
                System.out.println(device.getName());
            }

        }
    }

    private void Sandiness(){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            System.out.println("Go LOLLIPOP");
            //bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            bluetoothLeScanner.stopScan(scanCallback);
            mGatt = mBLEdevice.connectGatt(this,true,mGattCallback);
        }

    }

    private void onHandleData(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        BluetoothDevice device = gatt.getDevice();
        UUID uuid = characteristic.getUuid();

        if (TX_CHARACTERISTIC.equals(uuid)) {
            //System.out.println(characteristic.getValue().toString());
            extractUartDataMap(characteristic);
            /*
            System.out.println("%%SpO2", dataMap.get("%%SpO2"));
            System.out.println("bpm", dataMap.get("bpm"));
            System.out.println("signalQuality", dataMap.get("signalQuality"));
            System.out.println("motion", dataMap.get("motion"));
            */
        }

    }

    public static void extractUartDataMap(@NonNull BluetoothGattCharacteristic characteristic) {
        byte[] rawData = characteristic.getValue();
        String str = new String(rawData, StandardCharsets.UTF_8);
        StringBuffer buffer = new StringBuffer("0x");
        int i;
        for (byte b : rawData) {
            i = b & 0xff;
            buffer.append(Integer.toHexString(i));
        }
        System.out.println("read data:" + buffer.toString());
        // SpO2 & bpm
        Integer spo2PR = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
        Integer bpm = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 2);
        Integer signalQuality = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 3); // 0~255
        Integer motion = (rawData[4] >> 3) & 1;  // motion bit: 0x08. 0: idle; 1: moving

        String log = String.format(Locale.getDefault(), "%3d, %3d, %3d, %d", spo2PR, bpm, signalQuality, motion);
        Log.d("UART_test", log);
        //UnityPlayer.UnitySendMessage("Main Camera", "Message", log);

    }

    private void initCharacteristicWriting(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic) {
        UUID uuid = characteristic.getUuid();

        if (RX_CHARACTERISTIC.equals(uuid)) {
            //initSetTimeWriting(gatt, characteristic);
            initUartReading(gatt, characteristic);
        }
    }

    private void initUartReading(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic) {
        int[] data = createDataOfWatchSetAutoResult();

        byte[] dataBytes = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            dataBytes[i] = (byte) data[i];
        }
        characteristic.setValue(dataBytes);
        //addBusyAction(gatt, GattBusyAction.WRITE_CHARACTERISTIC, characteristic);
        System.out.println("Writedata");
        gatt.writeCharacteristic(characteristic);
        //notice();
    }

    private void initCharacteristicReading(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic) {
        final int charaProp = characteristic.getProperties();
        System.out.println("txCharacteristic = "+ charaProp);
        enableNotifications(characteristic);

        if ((charaProp & characteristic.PROPERTY_READ) > 0) {
            //addBusyAction(gatt, GattBusyAction.READ_CHARACTERISTIC, characteristic);
            System.out.println("readCharacteristic");
            gatt.readCharacteristic(characteristic);

        }
        if ((charaProp & characteristic.PROPERTY_NOTIFY) > 0) {
            boolean isEnableNotification = gatt.setCharacteristicNotification(characteristic, true);
            if(isEnableNotification) {
                List<BluetoothGattDescriptor> descriptorList = characteristic.getDescriptors();
                System.out.println("descriptorList = " + descriptorList.toString());
                if(descriptorList != null && descriptorList.size() > 0) {
                    for(BluetoothGattDescriptor descriptor : descriptorList) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        System.out.println("descriptor = "+ descriptor.toString());

                    }
                }
            }
        }

    }

    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    protected final boolean enableNotifications(final BluetoothGattCharacteristic characteristic) {
        final BluetoothGatt gatt = mGatt;
        if (gatt == null || characteristic == null)
            return false;

        // Check characteristic property
        final int properties = characteristic.getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0)
            return false;

        Log.d("BLE", "gatt.setCharacteristicNotification(" + characteristic.getUuid() + ", true)");
        gatt.setCharacteristicNotification(characteristic, true);
        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            Log.v("BLE", "Enabling notifications for " + characteristic.getUuid());
            Log.d("BLE", "gatt.writeDescriptor(" + CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID + ", value=0x01-00)");
            return gatt.writeDescriptor(descriptor);
        }
        return false;
    }


    private void initDataReading(@NonNull BluetoothGatt gatt) {
        BluetoothGattService service = gatt.getService(NORDIC_UART_SERVICE);
        if (service == null){
            System.out.println("没有得到心率服务");
        }
        else {
            System.out.println("得到心率服务");
            BluetoothGattCharacteristic rxCharacteristic = service.getCharacteristic(RX_CHARACTERISTIC);
            if (rxCharacteristic != null) {
                //initCharacteristicWriting(gatt, rxCharacteristic);
                System.out.println("RX_CHARACTERISTIC found");
                //System.out.println("TX_CHARACTERISTIC found success");
                initCharacteristicWriting(gatt,rxCharacteristic);

            }

            BluetoothGattCharacteristic txCharacteristic = service.getCharacteristic(TX_CHARACTERISTIC);
            if (txCharacteristic != null) {
                //initCharacteristicWriting(gatt, rxCharacteristic);
                System.out.println("TX_CHARACTERISTIC found");
                //System.out.println("TX_CHARACTERISTIC found success");
                initCharacteristicReading(gatt,txCharacteristic);

            }


        }
    }



    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            System.out.println("Connection State Changed:" + (newState == BluetoothProfile.STATE_CONNECTED ? "Connected" : "Disconnected"));
            //System.out.println("BluetoothGatt中中中"+ newState);
            if (BluetoothGatt.STATE_CONNECTED == newState) {
                System.out.println("on Connect");
                gatt.discoverServices();
            }else if (BluetoothGatt.STATE_DISCONNECTED == newState){
                //System.out.println("断开 中中中");
                //Toast.makeText(mContext, "断开连接", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            //onHandleData(gatt, characteristic);
            System.out.println("Data change");
            onHandleData(gatt,characteristic);

        }


        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            //发现服务，在蓝牙连接的时候会调用

            if (status == BluetoothGatt.GATT_SUCCESS){
                initDataReading(gatt);
            }

            /*
            List<BluetoothGattService> list = gatt.getServices();
            //System.out.println(list);

            for (BluetoothGattService bluetoothGattService:list){
                String str = bluetoothGattService.getUuid().toString();
                System.out.println("GATT Service " + str);
                List<BluetoothGattCharacteristic> gattCharacteristics = bluetoothGattService
                        .getCharacteristics();
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    System.out.println("gatt Characteristic" + gattCharacteristic.getUuid());
                }
            }
            */
            //EnableNotification(true,gatt,alertLevel);//必须要有，否则接收不到数据
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            //Log.e("onCharacteristicRead中", "数据接收了哦"+bytesToHexString(characteristic.getValue()));
            System.out.println("Accept data");
        }
    };


    private ScanCallback scanCallback=new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            System.out.println("Result");

            byte[] scanData = new byte[0];
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                scanData = result.getScanRecord().getBytes();
                System.out.println("ResultscanData" + scanData.toString());
                System.out.println("onScanResult :"+result.getScanRecord().getDeviceName());
                if (result.getScanRecord().getDeviceName() !=null && result.getScanRecord().getDeviceName().equals("oCare100_MBT")){
                    mBLEdevice = result.getDevice();
                    Sandiness();
                }
                //mBLEdevice = result.getDevice();
                //mBluetoothAdapter.notifyDataSetChanged();

            }

            //把byte数组转成16进制字符串，方便查看
            //Log.e("TAG","onScanResult :"+CYUtils.Bytes2HexString(scanData));
        }
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            System.out.println("onScanResultList :"+results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };
}
