package jp.ac.titech.itpro.sdl.sumoooru2.sstodo2;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_BT1 = 1, REQ_SYNC = 2, REQ_MAKETODOLIST = 3, REQ_STOPSPIN = 4;
    private final static UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private ArrayList<Note> todoList;
    private ArrayAdapter<Note> todoAdapter;
    private BTSocket btSocket;

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

        ListView todoListView = (ListView) findViewById(R.id.todo_list);
        todoList = new ArrayList<>();
        todoAdapter = new ArrayAdapter<Note>(this, 0, todoList) {
            @Override
            public View getView(int pos, View view, ViewGroup parent) {
                final Note note = getItem(pos);
                if (!note.visible) {
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    view = inflater.inflate(R.layout.todo_empty, parent, false);
                    return view;
                }
                if (note.isTag) {
                    LayoutInflater inflater = LayoutInflater.from(getContext());
//                    view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
                    view = inflater.inflate(android.R.layout.test_list_item, parent, false);
                    view.setBackgroundColor(Color.LTGRAY);
                    TextView tv1 = (TextView) view.findViewById(android.R.id.text1);
                    tv1.setTextColor(Color.DKGRAY);
                    tv1.setText(note.tag);
//                    tv1.setText(note.tag.substring(1, note.tag.length()-1));
//                    view.setVisibility(View.GONE);
                    return view;
                }

                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd");
                LayoutInflater inflater = LayoutInflater.from(getContext());
                if (note.isEditing()) {
                    view = inflater.inflate(R.layout.todo_elem2_edit, parent, false);
                    Button ok = (Button) view.findViewById(R.id.edit_ok);
                    final View finalView = view;
                    ok.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            EditText et = (EditText) finalView.findViewById(R.id.texts);
                            String texts = et.getText().toString();
                            note.changeTexts(texts);
                            et = (EditText) finalView.findViewById(R.id.limit);
                            String limit = et.getText().toString();
                            note.changeLimit(limit);
                            save(todoList);
                            note.setEditing(false);
                            todoAdapter.notifyDataSetChanged();
                        }
                    });
                    Button cancel = (Button) view.findViewById(R.id.edit_cancel);
                    cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            note.setEditing(false);
                            todoAdapter.notifyDataSetChanged();
                        }
                    });

                } else {
                    view = inflater.inflate(R.layout.todo_elem2, parent, false);
                    view.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            Toast.makeText(MainActivity.this, "long click", Toast.LENGTH_SHORT).show();
                            note.setEditing(true);
                            todoAdapter.notifyDataSetChanged();
                            return false;
                        }
                    });
                    view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            TextView texts = (TextView) v.findViewById(R.id.texts);
                            note.toggle();
                            texts.setText(note.makeText());
                        }
                    });
                }

//                Button button = (Button) view.findViewById(R.id.edit_button);
                TextView texts = (TextView) view.findViewById(R.id.texts);
                texts.setText(note.makeText());
                try {
                    Date dt = sdf.parse(note.date);
                    TextView date = (TextView) view.findViewById(R.id.limit);
                    date.setText(note.date);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(dt);
                    cal.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
                    cal.set(Calendar.HOUR, 23);
                    cal.set(Calendar.MINUTE, 59);
                    long diffMilli = (cal.getTimeInMillis() - Calendar.getInstance().getTimeInMillis());
                    long diffDay = diffMilli / (1000 * 60 * 60 * 24);
//                    left.setText(sdf.format(dt));
                    TextView left = (TextView) view.findViewById(R.id.left_days);
                    if (diffMilli < 0) {
                        left.setTextColor(Color.RED);
                        left.setText("Over");
                    } else if (diffDay == 0) {
//                        long diffHour = diffMilli / (1000 * 60 * 60);
//                        left.setText(diffHour + " hours");
                        left.setTextColor(Color.MAGENTA);
                        left.setText("Today");
                    } else {
                        if (diffDay == 1) {
                            left.setTextColor(Color.GREEN);
                        }
                        left.setText(diffDay + " days");
                    }
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
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage("processing...");
        progressDialog.setCancelable(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (btSocket != null && btSocket.isConnected()) {
            btSocket.close();
        }
    }

    private void makeTodoList() {
        try {

            String prevTag = "";

            JsonReader reader = new JsonReader(new InputStreamReader(openFileInput(".lastsync"), "UTF-8"));
            reader.beginObject();
            if (!reader.nextName().equals("list")) {
                throw new IOException("not list");
            }
            reader.beginArray();
            while (reader.hasNext()) {
                Note note = noteFromJson(reader);
//                Log.d("make", note.tag);

                prevTag = note.setFlags(prevTag);
                todoAdapter.add(note);

            }
            todoAdapter.notifyDataSetChanged();
            reader.endArray();
            reader.endObject();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
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
                                if (!socket.isConnected()) {
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

    private ProgressDialog progressDialog;
    private Handler commHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
//                case REQ_SYNC:
//                    btSocket.sync();
//                    progressDialog.dismiss();
//                    break;
                case REQ_MAKETODOLIST:
                    makeTodoList();
                    progressDialog.dismiss();
                    break;
                case REQ_STOPSPIN:
                    progressDialog.dismiss();
                    break;
            }
            return false;
        }
    });

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
//            case R.id.action_settings:
//                todoAdapter.add(new Note());
//                todoAdapter.notifyDataSetChanged();
//                todoListView.smoothScrollByOffset(todoAdapter.getCount());
//                return true;
            case R.id.action_settings2:
                //startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 123);
                if (btSocket != null && btSocket.isConnected()) {
                    Toast.makeText(MainActivity.this, "socket is already valid", Toast.LENGTH_SHORT).show();
                    break;
                }
                if (btSocket != null) {
                    Log.d("btsock", "" + btSocket.isConnected());
                }
                startActivityForResult(new Intent(this, BTActivity.class), REQ_BT1);
                return true;
//            case R.id.action_settings3:
//                btSocket.send();
//                break;
            case R.id.action_settings4:
                if (btSocket != null && btSocket.isConnected()) {
                    progressDialog.setMessage("pulling...");
                    progressDialog.show();
                    new Thread() {
                        @Override
                        public void run() {
                            btSocket.pull();
                        }
                    }.start();
                } else {
                    Toast.makeText(MainActivity.this, "socket is not valid", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.action_settings5:
                deleteFile(".lastsync");
                break;
            case R.id.action_settings6:
                if (btSocket != null && btSocket.isConnected()) {
                    progressDialog.setMessage("synchronizing...");
                    progressDialog.show();
                    new Thread() {
                        @Override
                        public void run() {
                            btSocket.sync();
                        }
                    }.start();
//                    btSocket.sync();
                } else {
                    Toast.makeText(MainActivity.this, "socket is not valid", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.action_settings7:
//                ProgressDialog progressDialog = new ProgressDialog(this);
//                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
//                progressDialog.setMessage("processing...");
//                progressDialog.setCancelable(true);
//                progressDialog.show();
//                progressDialog.dismiss();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void save(ArrayList<Note> notes) {
        try {
            //TODO
            deleteFile(".lastsync");
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(openFileOutput(".lastsync", MODE_PRIVATE), "UTF-8"));
            writer.beginObject();
            writer.name("list");
            writer.beginArray();

            for (Note n : notes) {
                n.write(writer);
            }

            writer.endArray();
            writer.endObject();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Note noteFromJson(JsonReader reader) throws IOException {
        Note note = new Note();
        reader.beginObject();
        while (reader.hasNext()) {
            note.read(reader);
        }
        reader.endObject();
        return note;
    }

    private class BTSocket {
        private JsonReader reader;
        private JsonWriter writer;
        private BufferedWriter bw;
        private OutputStreamWriter osw;
        private BluetoothSocket socket;

        public BTSocket(BluetoothSocket socket) {
            this.socket = socket;
            try {
                reader = new JsonReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                writer = new JsonWriter(osw = new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
                bw = new BufferedWriter(osw);
                reader.setLenient(true);
                writer.beginArray();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public boolean isConnected() {
            return socket.isConnected();
        }

        public void pull() {
            todoList.clear();
//?            todoAdapter.notifyDataSetChanged();
//            commHandler.sendMessage(commHandler.obtainMessage(123));
            try {
                writer.beginObject();
                writer.name("type").value("pull");
                writer.endObject();
                writer.flush();
                ArrayList<Note> recvNotes = new ArrayList<>();

                reader.beginObject();
                if (!reader.nextName().equals("list")) {
                    throw new IOException("not list");
                }
                reader.beginArray();
                while (reader.hasNext()) {
                    Note note = noteFromJson(reader);
//                    Log.d("pull", note.tag);
                    recvNotes.add(note);
                    if (note.tag.equals("__end")) {
                        break;
                    }
                }
                reader.endArray();
                reader.endObject();
                Log.d("pull num ", "" + recvNotes.size());
                save(recvNotes);
            } catch (IOException e) {
                e.printStackTrace();
            }
            commHandler.sendMessage(commHandler.obtainMessage(REQ_MAKETODOLIST));
//            makeTodoList();
        }

        public void sync() {
            try {
                writer.beginObject();
                writer.name("type").value("sync");
                writer.endObject();
                writer.flush();

                BufferedReader breader = new BufferedReader(new InputStreamReader(openFileInput(".lastsync"), "UTF-8"));
                String data = breader.readLine();
                breader.close();
                bw.write(data);
                bw.flush();

                reader.beginObject();
                String name = reader.nextName();//result_sync
                Log.d("sync", name);
                String result = reader.nextString();
                Log.d("sync", result);
                reader.endObject();
                if (result.equals("ok")) {
                    pull();
                }else{
                    commHandler.sendMessage(commHandler.obtainMessage(REQ_STOPSPIN));
                }


            } catch (IOException e) {
                e.printStackTrace();
            }
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
