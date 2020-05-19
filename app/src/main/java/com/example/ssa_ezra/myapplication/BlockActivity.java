package com.example.ssa_ezra.myapplication;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import java.util.ArrayList;
import  java.util.Arrays;

public class BlockActivity extends AppCompatActivity implements  View.OnClickListener {


    ArrayList<String> phones = new ArrayList();
    ArrayAdapter<String> adapter;

    ArrayList<String> selectedPhones = new ArrayList();

    ListView phonesList;

    EditText phoneEditText;

    Button button1;
    Button button2;
    Button button3;
    Button button4;
    Button button5;
    TextView txt;

    DBHelper dbHelper;

    boolean viewContactList = false;
    boolean viewBlockedList = false;

    SQLiteDatabase database;

    String pn;

    private static final int REQUEST_CODE_READ_CONTACTS=1;
    private static boolean READ_CONTACTS_GRANTED =false;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block);


        dbHelper = new DBHelper(this);

        phoneEditText = (EditText) findViewById(R.id.TextFields);

        button1 = (Button) findViewById(R.id.button1);
        button1.setOnClickListener(this);

        button2 = (Button) findViewById(R.id.button2);
        button2.setOnClickListener(this);

        button3 = (Button) findViewById(R.id.button3);
        button3.setOnClickListener(this);

        button4 = (Button) findViewById(R.id.button3);
        button4.setOnClickListener(this);

        button5 = (Button) findViewById(R.id.button5);
        button5.setOnClickListener(this);

        txt = (TextView)findViewById(R.id.textView);

        phonesList = (ListView) findViewById(R.id.phonesList);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, phones);
        phonesList.setAdapter(adapter);

        // обработка установки и снятия отметки в списке
        phonesList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            String phone;
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id)
            {
                // получаем нажатый элемент
                phone = adapter.getItem(position);
                if(phonesList.isItemChecked(position)==true)
                {
                    selectedPhones.add(phone);
                    //pn = adapter.getItem(position);

                }
                else{
                    selectedPhones.remove(phone);
                }
            }

        });

        // получаем разрешения
        int hasReadContactPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS);
        // если устройство до API 23, устанавливаем разрешение
        if(hasReadContactPermission == PackageManager.PERMISSION_GRANTED){
            READ_CONTACTS_GRANTED = true;
        }
        else{
            // вызываем диалоговое окно для установки разрешений
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_CODE_READ_CONTACTS);
        }
        // если разрешение установлено, загружаем контакты
        if (READ_CONTACTS_GRANTED){
            button4.setOnClickListener(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){

        switch (requestCode){
            case REQUEST_CODE_READ_CONTACTS:
                if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    READ_CONTACTS_GRANTED = true;
                }
        }
        if(READ_CONTACTS_GRANTED){
            button4.setOnClickListener(this);
        }
        else{
            Toast.makeText(this, "Требуется установить разрешения", Toast.LENGTH_LONG).show();
        }
    }




    ///переход на главное активити
    @Override
    public void onClick(View v) {

        //берем информацию того что ввели
        String phone = phoneEditText.getText().toString();
        ///подключение к базе данных
        database = dbHelper.getWritableDatabase();

        ContentValues contentValues = new ContentValues();

        switch (v.getId())
        {
            ///button Next
            case R.id.button1:
                //database.delete(DBHelper.TABLE_CONTACTS,null,null);
                Intent intent = new Intent(this,MainActivity.class);
                startActivity(intent);
                break;

            ///button +
            case R.id.button2:
                contentValues.put(DBHelper.PHONE_NUMBER,phone);
                database.insert(DBHelper.TABLE_CONTACTS,null,contentValues);
                //txt.append(selectedPhones);

                for(int i=0; i< selectedPhones.size();i++)
                {
                    phone = selectedPhones.get(i);
                    String[] words = phone.split("\\+");
                    txt.append(words[words.length-1]+"\n");

                    contentValues.put(DBHelper.PHONE_NUMBER,words[words.length-1]);
                    database.insert(DBHelper.TABLE_CONTACTS,null,contentValues);

                    /*deleteTitle(phone);
                    adapter.remove(selectedPhones.get(i));*/

                }
                if(!phone.isEmpty() && phones.contains(phone)==false)
                {
                    adapter.setNotifyOnChange(true);
                    //adapter.add(phone);
                    phoneEditText.setText("");
                }
                break;

            ///button -
            case R.id.button3:

                //database.delete(DBHelper.TABLE_CONTACTS,null,null);
                // получаем и удаляем выделенные элементы
                String phoneN;
                for(int i=0; i< selectedPhones.size();i++)
                {
                    phone = selectedPhones.get(i);
                    deleteTitle(phone);
                    adapter.remove(selectedPhones.get(i));

                }
                // снимаем все ранее установленные отметки
                phonesList.clearChoices();
                // очищаем массив выбраных объектов
                selectedPhones.clear();

                adapter.notifyDataSetChanged();
                break;

            ///button Show
            case R.id.button4:
                viewContactList = !viewContactList;
                if(viewContactList)
                {
                    Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[] {ContactsContract.CommonDataKinds.Phone._ID, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER}, null, null, null);
                    startManagingCursor(cursor);
                    if (cursor.getCount() > 0)
                    {
                        int phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                        int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                        while (cursor.moveToNext())
                        {
                            String phoneNum = cursor.getString(phoneIndex);
                            String phoneName = cursor.getString(nameIndex);
                            // process them as you want
                            adapter.add(phoneName+" "+ phoneNum);
                            Log.i("DATA"," ID "+cursor.getString(0)+" NAME "+cursor.getString(1)+" PHONE "+cursor.getString(2));
                        }
                    }
                    break;
                }
                else
                {
                    adapter.clear();
                }
            case R.id.button5:
                viewBlockedList = !viewBlockedList;
                if(viewBlockedList)
                {
                    Cursor cursor = database.query(DBHelper.TABLE_CONTACTS,null,null,null,null,null,null);
                    if(cursor.moveToFirst())
                    {
                        int phoneIndex = cursor.getColumnIndex(DBHelper.PHONE_NUMBER);
                        do {
                            String phoneNum = cursor.getString(phoneIndex);
                            adapter.add(phoneNum);
                        }while(cursor.moveToNext());
                    }
                    cursor.close();
                }
                else
                {
                    adapter.clear();
                }
        }

        dbHelper.close();
    }


    public void deleteTitle(String phone)
    {
        database.delete(DBHelper.TABLE_CONTACTS, DBHelper.PHONE_NUMBER + " = ?", new String[] {phone});
        //database.close();
        //dbHelper.close();
    }

}
