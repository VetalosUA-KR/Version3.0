package com.example.ssa_ezra.myapplication;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener, View.OnClickListener {
    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

    private TextView tvMsg;
    private ReceiveBroadcastReceiver imageChangeBroadcastReceiver;
    private AlertDialog enableNotificationListenerAlertDialog;

    private static MainActivity inst;
    ArrayList<String> smsMessagesList = new ArrayList<String>();
    ListView smsListView;

    ArrayAdapter arrayAdapter;

    TextToSpeech tts;

    Switch aSwitch;
    boolean onOff;
    ImageButton btnSpeak;

    DBHelper dbHelper1;
    SQLiteDatabase database;
    ContentValues contentValues;
    Cursor cursor;

    boolean isInList1 = false;
    boolean isInList2 = false;


    ArrayList<String> phoneList = new ArrayList<String>();

    static final String TAG = "myLogs";
    static final int PAGE_COUNT = 10;




    public static  MainActivity instance()
    {
        return inst;
    }

    public void onStart(){
        super.onStart();
        inst = this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        onOff = true;
        dbHelper1 = new DBHelper(this);
        tvMsg = (TextView) this.findViewById(R.id.image_change_explanation);

        tts = new TextToSpeech(this, this);

        btnSpeak = (ImageButton) findViewById(R.id.btnSpeak);

        btnSpeak.setOnClickListener(this);

        smsListView = (ListView) findViewById(R.id.List);

        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, smsMessagesList);
        smsListView.setAdapter(arrayAdapter);


       ///if Permission Is Not GRANTED
        if (ContextCompat.checkSelfPermission(getBaseContext(),
                "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) {
            // Show SMS
            refreshSMSInbox();
        } else {
            final int REQUEST_CODE_ASK_PERMISSIONS = 123;
            ActivityCompat.requestPermissions(MainActivity.this, new String[]
                    {"android.permission.READ_SMS"}, REQUEST_CODE_ASK_PERMISSIONS);
        }

        if (!isNotificationServiceEnabled()) {
            enableNotificationListenerAlertDialog = buildNotificationServiceAlertDialog();
            enableNotificationListenerAlertDialog.show();
        }

        // Finally we register a receiver to tell the MainActivity when a notification has been received
        imageChangeBroadcastReceiver = new ReceiveBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.example.ssa_ezra.myapplication");
        registerReceiver(imageChangeBroadcastReceiver, intentFilter);

    }

    /**
     * Is Notification Service Enabled.
     */
    private boolean isNotificationServiceEnabled(){
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(),
                ENABLED_NOTIFICATION_LISTENERS);
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }




    ///этот метод отвечает за сообщения внутри телефона(не за соц.сети)
    public void refreshSMSInbox() {

        ContentResolver contentResolver = getContentResolver();
        Cursor smsInboxCursor = contentResolver.query(Uri.parse("content://sms/inbox"), null, null, null, null);
        int indexBody = smsInboxCursor.getColumnIndex("Body");
        int indexAdress = smsInboxCursor.getColumnIndex("address");
        if(indexBody < 0 || !smsInboxCursor.moveToFirst()) return;


        database = dbHelper1.getWritableDatabase();

        contentValues = new ContentValues();

        cursor = database.query(DBHelper.TABLE_CONTACTS,null,null,null,null,null,null);

        if (cursor.moveToFirst())
        {
            int idIndex = cursor.getColumnIndex(DBHelper.KEY_ID);
            int phoneNumber = cursor.getColumnIndex(DBHelper.PHONE_NUMBER);

            do {
                phoneList.add(cursor.getString(phoneNumber));
            } while (cursor.moveToNext());

        } else {
            Log.d("mLogi", "0 rows");
        }


        String str = "SMS From: " + smsInboxCursor.getString(indexAdress) +
                "\n";

        String fromAdress = smsInboxCursor.getString(indexAdress);

        ///added info about who sended message for us
        String from = "SMS From: " + smsInboxCursor.getString(indexAdress);
        String sms = smsInboxCursor.getString(indexBody);

        for(int i=0;i<phoneList.size();i++)
        {
            if(phoneList.get(i).equals(fromAdress))
            {
                isInList1 = true;
                break;
            }
        }

        if(isInList1 == true)
        {
            arrayAdapter.add(from + "\n");
            tvMsg.setText("Error" + "\n");
        }
        else
        {
            String text_string = sms.toString();
            ///проверка на ссылку
            if (text_string.contains("https") || text_string.contains("www"))
            {
                arrayAdapter.add(from + "\n");
                tvMsg.setText(from + "\n" + sms + "\n");

                speakOut();
           } else {

                arrayAdapter.add(from + "\n");
                tvMsg.setText(from + "\n" + sms + "\n");
                speakOut(); }
        }

        ///освобождаем память
        cursor.close();

        ///закрываем базу
        dbHelper1.close();
    }

    public void updateList(final String smsMessage){
        arrayAdapter.insert(smsMessage,0);
        arrayAdapter.notifyDataSetChanged();
    }

    public void onDestroy()
    {
        // Don't forget to shutdown tts!
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            unregisterReceiver(imageChangeBroadcastReceiver);
        }
        super.onDestroy();
    }


    @Override
    public void onInit(int status)
    {
        if (status == TextToSpeech.SUCCESS)
        {

            int result = tts.setLanguage(Locale.ENGLISH);

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
            {
                Log.e("TTS", "This Language is not supported");
            } else {
                btnSpeak.setEnabled(true);
                speakOut();
            }

        } else {
            Log.e("TTS", "Initilization Failed!");
        }

    }

    @Override
    public void onClick(View v)
    {
        if(!onOff)
        {
            onOff = true;
            btnSpeak.setImageResource(R.drawable.on);
        }
        else
        {
            onOff = false;
            btnSpeak.setImageResource(R.drawable.off);
        }
    }


    /**
     * Receive Broadcast Receiver.
     * */
    public class ReceiveBroadcastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            int receivedNotificationCode = intent.getIntExtra("Notification Code",-1);
            String packages = intent.getStringExtra("package");
            String title = intent.getStringExtra("title");
            String text = intent.getStringExtra("text");

            if(text != null)
            {
                if(!text.contains("new messages") && !text.contains("WhatsApp Web is currently active") && !text.contains("WhatsApp Web login"))
                {
                    String android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                            Settings.Secure.ANDROID_ID);
                    String devicemodel = android.os.Build.MANUFACTURER+android.os.Build.MODEL+android.os.Build.BRAND+android.os.Build.SERIAL;

                    arrayAdapter.clear();

                    String text_string = text.toString();

                    for(int i=0;i<phoneList.size();i++)
                    {
                        if(phoneList.get(i).equals(title))
                        {
                            isInList2 = true;
                            break;
                        }
                    }

                    if(isInList2 == true)
                    {
                        arrayAdapter.add(title + "\n");
                        tvMsg.setText("Error "+ "\n");
                        speakOut();
                    }

                    else {

                        ///проверка на ссылку
                        if (text_string.contains("https") || text_string.contains("www")) {
                            arrayAdapter.add(title + "\n");
                            tvMsg.setText(title + " sent you link " + "\n");
                            speakOut();
                        } else {

                            arrayAdapter.add(title + "\n");
                            tvMsg.setText(title + "\n" + text + "\n");
                            speakOut();
                        }
                    }
                }
            }
        }
    }

    ///speech to text
    private void speakOut()
    {
        if(onOff == true)
        {
            String text = tvMsg.getText().toString();
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    /**
     * Build Notification Listener Alert Dialog.
     */
    private AlertDialog buildNotificationServiceAlertDialog(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(R.string.notification_listener_service);
        alertDialogBuilder.setMessage(R.string.notification_listener_service_explanation);
        alertDialogBuilder.setPositiveButton(R.string.yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    }
                });
        alertDialogBuilder.setNegativeButton(R.string.no,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // If you choose to not enable the notification listener
                        // the app. will not work as expected
                    }
                });
        return(alertDialogBuilder.create());
    }







}
