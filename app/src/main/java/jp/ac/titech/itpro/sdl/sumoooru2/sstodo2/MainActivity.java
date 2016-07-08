package jp.ac.titech.itpro.sdl.sumoooru2.sstodo2;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_BT1 = 1;
    private final static UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private ListView todoListView;
    private ArrayList<Note> todoList;
    private ArrayAdapter<Note> todoAdapter;
    private BTSocket btSocket;

    private class Note {
        public String tag, date;
        public ArrayList<String> texts = new ArrayList<>();

        public String makeText() {
            return makeText(-1);
        }

        public String makeText(int lines) {
            if (texts == null) {
                return "";
            }
            String ret = "";
            int cnt = 0;
            for (String t : texts) {
                ret += t + '\n';
                cnt++;
                if (cnt == lines) {
                    ret += ".....";
                    break;
                }
            }
            return ret;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        todoListView = (ListView) findViewById(R.id.todo_list);
        todoList = new ArrayList<>();
        todoAdapter = new ArrayAdapter<Note>(this, 0, todoList) {
            @Override
            public View getView(int pos, View view, ViewGroup parent) {
                Note note = getItem(pos);
                if (note.texts == null) {
                    LayoutInflater inflater = LayoutInflater.from(getContext());
//                    view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
                    view = inflater.inflate(android.R.layout.test_list_item, parent, false);
                    TextView tv1 = (TextView) view.findViewById(android.R.id.text1);
                    tv1.setText(note.tag);
//                    tv1.setText(note.tag.substring(1, note.tag.length()-1));
                    return view;
                }

                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd");
                LayoutInflater inflater = LayoutInflater.from(getContext());
                view = inflater.inflate(R.layout.todo_elem, parent, false);
                TextView texts = (TextView) view.findViewById(R.id.texts);
                texts.setText(note.makeText(5));
                try {
                    Date dt = sdf.parse(note.date);
                    TextView date = (TextView) view.findViewById(R.id.date);
                    date.setText(note.date);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(dt);
                    cal.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
                    long diffDay = (cal.getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) / (1000 * 60 * 60 * 24);
//                    left.setText(sdf.format(dt));
                    TextView left = (TextView) view.findViewById(R.id.left_days);
                    left.setText(diffDay + " days");
                } catch (ParseException e) {
                    if (!note.date.equals("before")) {
                        e.printStackTrace();
                    }
                }
                return view;
            }
        };
        todoListView.setAdapter(todoAdapter);

    }

    @Override
    public void onResume() {
        super.onResume();
        makeTodoList();
    }

    @Override
    public void onPause(){
        super.onPause();
        if (btSocket != null && btSocket.isConnected()) {
            btSocket.close();
        }
    }

    private void makeTodoList(){
        try {
            FileInputStream in = openFileInput(".lastsync");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String str = "";
            String tmp;
            while ((tmp = reader.readLine()) != null) {
                str = str + tmp + "\n";
            }
            reader.close();
            Log.d("test", str);

            String prevTag = "";

            JsonReader reader2 = new JsonReader(new InputStreamReader(openFileInput(".lastsync"), "UTF-8"));
            reader2.beginObject();
            if (!reader2.nextName().equals("list")){
                throw new IOException("not list");
            }
            reader2.beginArray();
            while(reader2.hasNext()) {
                Note note = new Note();
                reader2.beginObject();
                while(reader2.hasNext()) {
                    switch (reader2.nextName()) {
                        case "date":
                            note.date = reader2.nextString();
                            break;
                        case "texts":
                            reader2.beginArray();
                            while (reader2.hasNext()) {
                                note.texts.add(reader2.nextString());
                            }
                            reader2.endArray();
                            break;
                        case "tag":
                            note.tag = reader2.nextString();
                            break;
                    }
                }
                reader2.endObject();
                if (!prevTag.equals(note.tag)) {
                    prevTag = note.tag;
                    Note tag = new Note();
                    tag.tag = note.tag;
                    tag.texts = null;
                    todoAdapter.add(tag);
                }

                todoAdapter.add(note);

            }
            todoAdapter.notifyDataSetChanged();
            reader2.endArray();
            reader2.endObject();
            reader2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int reqCode, int resCode, Intent data) {
        Log.d("test", "onactr " + reqCode);
        switch (reqCode) {
            case REQ_BT1:
                if (resCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "test", Toast.LENGTH_SHORT).show();
                    //connect1((BluetoothDevice) data.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
                    new AsyncTask<BluetoothDevice, Void, BluetoothSocket>() {

                        @Override
                        protected BluetoothSocket doInBackground(BluetoothDevice... params) {
                            BluetoothSocket socket = null;
                            try {
                                socket = params[0].createRfcommSocketToServiceRecord(SPP_UUID);
                                if(!socket.isConnected()){
                                    socket.connect();
                                }
                            } catch (IOException e) {
                                if (socket != null) {
                                    try {
                                        socket.close();
                                    } catch (IOException e1) {
                                        e1.printStackTrace();
                                    }
                                    socket = null;
                                }
                                e.printStackTrace();
                            }
                            return socket;
                        }

                        @Override
                        protected void onPostExecute(BluetoothSocket socket) {
                            if (socket == null) {
                                Toast.makeText(MainActivity.this, "fail to connect", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            Toast.makeText(MainActivity.this, "success to connect", Toast.LENGTH_SHORT).show();

                            btSocket = new BTSocket(socket);
                            btSocket.init();
//                            btSocket.start();
                            Log.d("test", "start socket");
                        }
                    }.execute((BluetoothDevice) data.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
                } else {
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_settings:
                todoAdapter.add(new Note());
                todoAdapter.notifyDataSetChanged();
                todoListView.smoothScrollByOffset(todoAdapter.getCount());
                return true;
            case R.id.action_settings2:
                //startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 123);
                if (btSocket != null && btSocket.isConnected()) {
                    Toast.makeText(MainActivity.this, "socket is already valid", Toast.LENGTH_SHORT).show();
                    break;
                }
                if(btSocket != null){
                    Log.d("btsock", "" + btSocket.isConnected());
                }
                startActivityForResult(new Intent(this, BTActivity.class), REQ_BT1);
                return true;
            case R.id.action_settings3:
                btSocket.send();
                break;
            case R.id.action_settings4:
                if(btSocket != null && btSocket.isConnected()){
                    btSocket.pull();
                }else{
                    Toast.makeText(MainActivity.this, "socket is not valid", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.action_settings5:
                deleteFile(".lastsync");
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private class BTSocket{
        private JsonReader reader;
        private JsonWriter writer;
        private BluetoothSocket socket;

        public BTSocket(BluetoothSocket socket) {
            this.socket = socket;
            try {
                reader = new JsonReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                writer = new JsonWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
                reader.setLenient(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public boolean isConnected(){
            return socket.isConnected();
        }

        public void init(){
            try {
                writer.beginArray();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void send() {
            try {
                writer.beginObject();
                writer.name("seq").value(1);
                writer.name("time").value("12:34");
                writer.name("content").value("hello");
                writer.name("sender").value("me");
                writer.endObject();
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void pull() {
            todoList.clear();
            todoAdapter.notifyDataSetChanged();
            try {
                writer.beginObject();
                writer.name("type").value("pull");
                writer.endObject();
                writer.flush();
                ArrayList<Note> recvNotes = new ArrayList<>();

                boolean last = true;
                while (last && reader.hasNext()) {
                    Note note = new Note();
                    boolean ok = true;
                    reader.beginObject();
                    while (reader.hasNext()) {
                        String name = reader.nextName();
                        switch (name) {
                            case "texts":
                                reader.beginArray();
                                while (reader.hasNext()) {
                                    while (reader.hasNext()) {
                                        String val = reader.nextString();
                                        note.texts.add(val);
                                    }
                                }
                                reader.endArray();
                                break;
                            case "date": {
                                note.date = reader.nextString();
                                break;
                            }
                            case "tag": {
                                switch (note.tag = reader.nextString()) {
                                    case "__end":
                                        last = false;
                                    case "__begin":
                                        ok = false;
                                        break;
                                }
                                break;
                            }
                            default:
                                Log.d("reader", "??? " + name);
                                break;
                        }

                    }
                    reader.endObject();
                    if (ok && (!note.texts.get(0).equals("") && !note.texts.isEmpty())) {
                        recvNotes.add(note);
                    }
                }
                try {
                    //TODO
                    deleteFile(".lastsync");
                    JsonWriter writer2 = new JsonWriter(new OutputStreamWriter(openFileOutput(".lastsync", MODE_PRIVATE), "UTF-8"));
                    writer2.beginObject();
                    writer2.name("list");
                    writer2.beginArray();
                    for (Note n : recvNotes) {
                        Log.d("test", n.makeText(2));
                        writer2.beginObject();
                        writer2.name("date").value(n.date);
                        writer2.name("texts");
                        writer2.beginArray();
                        for(String s : n.texts){
                            writer2.value(s);
                        }
                        writer2.endArray();
                        writer2.name("tag").value(n.tag);
                        writer2.endObject();
//                        String str = "保存する文字列";
//                        FileOutputStream out = openFileOutput( "test.txt", MODE_PRIVATE );
//                        out.write( str.getBytes()   );

                    }

                    writer2.endArray();
                    writer2.endObject();
                    writer2.flush();
                    writer2.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            makeTodoList();
        }

        public void close() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
