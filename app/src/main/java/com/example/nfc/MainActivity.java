package com.example.nfc;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String errorDetected = "NFC метка не обнаружена";
    public static final String writeSuccess = "Успешное считывание!";
    public static final String writeError = "Ошибка при записи, попробуйте снова";

    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter intentFilter[];
    boolean writeMode;
    Tag tag;
    Context context;

    TextView editMessage, nfcContents;
    Button activateButton;

    DBHelper dbHelper;

    private NdefRecord createRecord(String text) throws UnsupportedEncodingException{
        String lang = "ru";
        byte[] textBytes = text.getBytes();
        byte[] langBytes = lang.getBytes("UTF-8");
        int langLength = langBytes.length;
        int textLength = textBytes.length;
        byte[] payload = new byte[1 + langLength + textLength];

        //установка байт состояний
        payload[0] = (byte) langLength;

        //копирование длины байтов и текстовых байтов в payload
        System.arraycopy(langBytes, 0, payload, 1, langLength);
        System.arraycopy(langBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);

        return record;
    }

    private void write(String text, Tag tag) throws IOException, FormatException{
        NdefRecord[] records = { createRecord(text) };
        NdefMessage message = new NdefMessage(records);

        //получение экземпляра Ndef для метки
        Ndef ndef = Ndef.get(tag);

        //включить ввод / вывод
        ndef.connect();

        //написание сообщения
        ndef.writeNdefMessage(message);

        //закрытие соединения
        ndef.close();
    }

    private void buildTadViews(NdefMessage[] messages){
        if(messages == null || messages.length == 0){
            return;
        }

        String text = "";
        byte[] payload = messages[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16"; //получение текстовой кодировки
        int languageCodeLength = payload[0] & 0063; //получение языковой кодировки

        try{
            //получение текста
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        } catch (UnsupportedEncodingException e){
            Log.e("UnsupportedEncoding", e.toString());
        }

        nfcContents.setText("Содержимое NFC: " + text);
    }

    private void readFromIntent(Intent intent){
        String action = intent.getAction();

        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)){
            Parcelable[] rawMessage = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] messages = null;

            if(rawMessage != null){
                messages = new NdefMessage[rawMessage.length];

                for(int i = 0; i < rawMessage.length; i++){
                    messages[i] = (NdefMessage) rawMessage[i];
                }
            }

            buildTadViews(messages);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editMessage = (TextView) findViewById(R.id.editMessage);
        activateButton = (Button) findViewById(R.id.activateButton);
        nfcContents = (TextView) findViewById(R.id.nfcContents);

        context = this;

        activateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    //при необнаружении NFC метки
                    if(tag == null){
                        Toast.makeText(context, errorDetected, Toast.LENGTH_LONG).show();
                    } else {
                        //при успешном обнаружении
                        write("Просканируйте NFC метку: " + editMessage.getText().toString(), tag);
                        Toast.makeText(context, writeSuccess, Toast.LENGTH_LONG).show();
                    }
                } catch (IOException e){
                    Toast.makeText(context, writeError, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                } catch (FormatException e){
                    Toast.makeText(context, writeError, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        });

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        //проверка на подходящее устройство
        if(nfcAdapter == null){
            Toast.makeText(this, "Данное устройство не поддерживает NFC", Toast.LENGTH_SHORT).show();
            finish();
        }

        readFromIntent(getIntent());

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);

        intentFilter = new IntentFilter[] { tagDetected };
    }

    @Override
    public void onClick(View view){
        //Cчитываем знаечение полей и сохраняем в эти переменные
        String productId = tag.getId().toString();

        SQLiteDatabase database = dbHelper.getWritableDatabase(); //Создается и обновляется новая таблица, если нет существующей

        //для добавления новых строк в таблицу
        ContentValues values = new ContentValues();

        values.put(DBHelper.KEY_PRODUCT_ID, productId);
        database.insert(DBHelper.TABLE_CONTACTS, null, values);

        //считываем данные бд в консоль
        Cursor cursor = database.query(DBHelper.TABLE_CONTACTS, null, null, null, null, null, null);

        if(cursor.moveToFirst()){
            int idIndex = cursor.getColumnIndex(DBHelper.KEY_ID);
            int idIndex2 = cursor.getColumnIndex(DBHelper.KEY_PRODUCT_ID);

            do{
                Log.d("mLog", "ID = " + cursor.getInt(idIndex) + ", Product_ID = " + cursor.getInt(idIndex2));
            } while (cursor.moveToNext());
        } else {
            Log.d("mLog", "0 rows");
        }

        cursor.close();

        dbHelper.close();
    }

    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);

        setIntent(intent);
        readFromIntent(intent);

        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())){
            tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        }
    }

    @Override
    public void onPause(){
        super.onPause();

        writeModeOff();
    }

    @Override
    public void onResume(){
        super.onResume();

        writeModeOn();
    }

    //разрешить запись
    private void writeModeOn(){
        writeMode = true;
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilter, null);
    }

    //отклонить запись
    private void writeModeOff(){
        writeMode = false;
        nfcAdapter.disableForegroundDispatch(this);
    }
}