package com.muharremtac.android.arduino.altiayakli;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class BluetoothActivity extends Activity {

	private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
	
	TextView accelerometerText;
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private ListView mConversationView;

    private String mConnectedDeviceName = null;
    private ArrayAdapter<String> mConversationArrayAdapter;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothService mCarService = null;
    private Sensor accelerometer;
    private SensorManager sensorManager;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        accelerometerText=(TextView) findViewById(R.id.accelerometerText);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    @SuppressLint("NewApi")
	private final void setStatus(int resId) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(resId);
    }

    @SuppressLint("NewApi")
	private final void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(subTitle);
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            if (mCarService == null) setupCar();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (mCarService != null) {
            if (mCarService.getState() == BluetoothService.STATE_NONE) {
              mCarService.start();
            }
        }
    }
 
    private void setupCar() {
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mConversationView = (ListView) findViewById(R.id.listView);
        mConversationView.setAdapter(mConversationArrayAdapter);
        mCarService = new BluetoothService(this, mHandler);

    }

    @Override
    public synchronized void onPause() {
    	if(sensorManager!=null){
        	sensorManager.unregisterListener(sensorEventListener);
    	}
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
    	if(sensorManager!=null){
    		sensorManager.unregisterListener(sensorEventListener);
    	}
        super.onDestroy();
        if (mCarService != null) mCarService.stop();
    }

    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }
    
  private void sendData(byte[] send){
	  if (mCarService.getState() != BluetoothService.STATE_CONNECTED) {
          Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
          return;
      }
	  
	  
	  if(send.length>0)
		  mCarService.write(send);
  }
 
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BluetoothService.STATE_CONNECTED:
                	setStatus(getString(R.string.title_connected_to));
                    setStatus(mConnectedDeviceName);
                    mConversationArrayAdapter.clear();
                    break;
                case BluetoothService.STATE_CONNECTING:
                	setStatus(R.string.title_connecting);
                    break;
                case BluetoothService.STATE_LISTEN:
                case BluetoothService.STATE_NONE:
                	setStatus(R.string.title_not_connected);
                    break;
                } 
                break;
           
            case MESSAGE_READ:
//                String s = msg.what+"";
//				if("".equals(s)){
//					sendData("0".getBytes());
//				}
//				if("50".equals(s)){
//					sendData("c".getBytes());
//				}else if("60".equals(s)){
//					sendData("a".getBytes());
//				}else if("70".equals(s)){
//					sendData("b".getBytes());
//				}else if("100".equals(s)){
//					sendData("d".getBytes());
//				}else{
//					sendData(s.getBytes());
//				}
                
                break;
            case MESSAGE_DEVICE_NAME:
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                if(sensorManager==null){
                	sensorManager=(SensorManager) getSystemService(SENSOR_SERVICE);
                }
                if(accelerometer==null){
                	accelerometer=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                }
                
                sensorManager.registerListener(sensorEventListener, accelerometer,SensorManager.SENSOR_DELAY_NORMAL);
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            if (resultCode == Activity.RESULT_OK) {
                String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                mCarService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            if (resultCode == Activity.RESULT_OK) {
                setupCar();
            } else {
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.scan:
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
        }
        return false;
    }
    
    
	private final SensorEventListener sensorEventListener = new SensorEventListener() {
		
		
		public void onSensorChanged(SensorEvent event) {
			synchronized (this) {
	            if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER)
	
	            {
	               int xAcceleration=(int) (-event.values[0] * 10);
	               int yAcceleration=(int) (-event.values[1] * 10);
	               int zAcceleration=(int) (-event.values[2] * 10);
	            	
	               accelerometerText.setText("X: "+ xAcceleration + "\n\rY: "+ yAcceleration+ "\n\rZ: " + zAcceleration);
	               if(xAcceleration<=-20){
	            	   sendData("1".getBytes());
	               }else if(xAcceleration>=30){
	            	   sendData("5".getBytes());
	               }else if(xAcceleration>=20){
	            	   sendData("4".getBytes());
	               }else if(xAcceleration<=10){
	            	   sendData("0".getBytes());
	               }else if(yAcceleration<=-20){
	            	   sendData("c".getBytes());
	               }else if(yAcceleration<=10){
	            	   sendData("0".getBytes());
	               }else if(yAcceleration>=20){
	            	   sendData("b".getBytes());
	               }
	            }
	        }
		}
		
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			
		}
	};
}