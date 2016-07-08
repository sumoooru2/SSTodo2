package jp.ac.titech.itpro.sdl.sumoooru2.sstodo2;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


public class BTActivity extends AppCompatActivity {

    private ListView deviceListView;

    private ArrayList<BluetoothDevice> deviceList;
    private ArrayAdapter<BluetoothDevice> deviceListAdapter;

    private BluetoothAdapter btAdapter;
    private BroadcastReceiver scanReceiver;
    private IntentFilter scanFilter;


    private final static String[] PERMISSIONS = {
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.btscan_main);

        deviceListView = (ListView) findViewById(R.id.dev_list);
        deviceList = new ArrayList<>();
        deviceListAdapter = new ArrayAdapter<BluetoothDevice>(this, 0, deviceList) {
            @Override
            public View getView(int pos, View view, ViewGroup parent) {
                if (view == null) {
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    view = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
                }
                BluetoothDevice device = getItem(pos);
                TextView nameView = (TextView) view.findViewById(android.R.id.text1);
                TextView addrView = (TextView) view.findViewById(android.R.id.text2);
                nameView.setText(device.getName());
                addrView.setText(device.getAddress());
                return view;
            }
        };
        deviceListView.setAdapter(deviceListAdapter);
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id){
                final BluetoothDevice device = (BluetoothDevice) parent.getItemAtPosition(pos);
                new AlertDialog.Builder(BTActivity.this)
                        .setTitle(device.getName())
                        .setMessage("connect?")
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.yes,
                                new DialogInterface.OnClickListener(){
                                    @Override
                                    public void onClick(DialogInterface dialog, int which){
                                        if(btAdapter.isDiscovering()){
                                            btAdapter.cancelDiscovery();
                                        }
                                        Intent data = new Intent();
                                        data.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
                                        BTActivity.this.setResult(Activity.RESULT_OK, data);
                                        BTActivity.this.finish();
                                    }
                                })
                        .setNegativeButton(android.R.string.no, null)
                        .show();
            }
        });

        scanReceiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent){
                String action = intent.getAction();
                switch(action){
                    case BluetoothDevice.ACTION_FOUND:
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        deviceListAdapter.add(device);
                        deviceListAdapter.notifyDataSetChanged();
                        Log.d("test", "add bluetooth device");
                        break;
//                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
//                        break;
//                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
//                        break;
                }
            }
        };
        scanFilter = new IntentFilter();
        scanFilter.addAction(BluetoothDevice.ACTION_FOUND);
//        scanFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
//        scanFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(btAdapter != null){
            //setupBT();
        }else{
            Toast.makeText(this, "Bluetooth is not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        for(BluetoothDevice device : btAdapter.getBondedDevices()){
            deviceListAdapter.add(device);
        }
        deviceListAdapter.notifyDataSetChanged();

    }

    @Override
    public void onResume(){
        super.onResume();
        registerReceiver(scanReceiver, scanFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(scanReceiver);
    }

}
