package com.example.nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;

public class MainActivity extends Activity implements View.OnClickListener {

    public static final String TAG = "MainActivity";
    private TextView tagId;
    private ImageView mImageView;
    private boolean NFCEnabled;
    private String WriteString;
    private String CurAction;
    private int WritePGNum;

    DBHelper dbHelper;

    public static String unHex(String arg){
        StringBuilder stringBuilder = new StringBuilder();

        for(int i = 0; i < arg.length(); i += 2){
            String s = arg.substring(i, (i + 2));

            int decimal = Integer.parseInt(s, 16);
            stringBuilder.append((char) decimal);
        }

        return stringBuilder.toString();
    }

    public void sendResult(String Id, String DataRfId, String[] TechList){
        Intent intent = new Intent();

        if(DataRfId.isEmpty()){
            DataRfId = "empty";
        }

        intent.putExtra("tech", TechList);
        intent.putExtra("result", DataRfId);

        if(CurAction.equals("read") && !DataRfId.equals("empty")){
            intent.putExtra("text", unHex(DataRfId));
        } else{
            intent.putExtra("text", DataRfId);
        }

        intent.putExtra("event", CurAction);
        intent.putExtra("uid", Id);
        setResult(RESULT_OK, intent);
        finish();
    }

    public static String ByteArrayToHexString(byte[] bytes){
        final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;

        for(int i = 0; i < bytes.length; i++){
            v = bytes[i] & 0xFF;
            hexChars[i * 2] = hexArray[v >>> 4];
            hexChars[i * 2 + 1] = hexArray[v & 0x0F];
        }

        return new String(hexChars);
    }


    @Override
    protected void onNewIntent(Intent intent){
        StringBuilder DataRfId = new StringBuilder();
        int pgcount;
        String Id = "";
        String[] TechList;

        NfcA nfcA = null;

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        if(tag == null){
            sendResult(Id, DataRfId.toString(), null);
            return;
        }

        Id = ByteArrayToHexString(tag.getId());
        TechList = tag.getTechList();

        try{
            nfcA = NfcA.get(tag);
            if(nfcA == null){
                sendResult(Id, DataRfId.toString(), TechList);
                return;
            }

            nfcA.connect();

            if(!nfcA.isConnected()){
                sendResult(Id, DataRfId.toString(), TechList);
                return;
            }

            if(CurAction.equals("write")){
                int ind = 0;
                int writeLengtht = WriteString.length();
                String writeStr;

                for(int pageNum = WritePGNum; pageNum <= nfcA.getMaxTransceiveLength() - 2 && ind < writeLengtht; pageNum++){
                    byte[] PG = new byte[]{(byte) 0x000, (byte) 0x000, (byte) 0x000, (byte) 0x000};

                    if(writeLengtht < ind + 4){
                        writeStr = WriteString.substring(ind, writeLengtht);

                        if(writeStr.length() >= 1){
                            PG[0] = (byte) ((int) writeStr.charAt(0) & 0x0ff);
                        }
                        if(writeStr.length() >= 2){
                            PG[1] = (byte) ((int) writeStr.charAt(1) & 0x0ff);
                        }
                        if(writeStr.length() >= 3){
                            PG[2] = (byte) ((int) writeStr.charAt(2) & 0x0ff);
                        }
                    } else {
                        writeStr = WriteString.substring(ind, ind + 4);

                        for(int i = 0; i < 3; i++){
                            PG[i] = (byte) ((int) writeStr.charAt(i) & 0x0ff);
                        }
                    }

                    byte[] result = nfcA.transceive(new byte[]{
                            (byte) 0xA2,
                            (byte) (pageNum & 0x0ff),
                            PG[0],
                            PG[1],
                            PG[2],
                            PG[3]
                    });

                    if(result == null){
                        DataRfId.append(", error");
                    } else if((result.length == 1) && ((result[0] & 0x00A) != 0x00A)){
                        DataRfId.append(ByteArrayToHexString(result));
                    } else{
                        DataRfId.append(", success");
                    }

                    ind += 4;
                }
            } else{
                pgcount = nfcA.getMaxTransceiveLength() / 4;

                for(int pageNum = 0; pageNum < pgcount; pageNum += 4){
                    byte[] result = nfcA.transceive(new byte[]{
                            (byte) 0x30,
                            (byte) (pageNum & 0x0ff)
                    });

                    DataRfId.append(ByteArrayToHexString(result));
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            if(nfcA != null){
                try{
                    nfcA.close();
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onPause(){
        super.onPause();

        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(this);

        if(nfc != null && NFCEnabled){
            nfc.disableForegroundDispatch(this);
            NFCEnabled = false;
        }

        finish();
    }

    @Override
    public void onResume(){
        super.onResume();

        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(this);

        if(nfc != null){
            final Intent intent = new Intent(this.getApplicationContext(), this.getClass());

            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

            String[][] techList = new String[3][1];

            techList[0][0] = "android.nfc.tech.NdefFormatable";
            techList[1][0] = "android.nfc.tech.NfcA";
            techList[2][0] = "android.nfc.tech.Ndef";

            IntentFilter[] filters = new IntentFilter[2];

            filters[0] = new IntentFilter();
            filters[0].addAction(NfcAdapter.ACTION_TECH_DISCOVERED);
            filters[0].addCategory(Intent.CATEGORY_DEFAULT);
            filters[1] = new IntentFilter();
            filters[1].addAction(NfcAdapter.ACTION_TECH_DISCOVERED);
            filters[1].addCategory(Intent.CATEGORY_DEFAULT);

            nfc.enableForegroundDispatch(this, pendingIntent, filters, techList);

            NFCEnabled = true;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(this);

        //TODO дописать главный метод установки параметров и привязать к БД
    }

    @Override
    public void onClick(View view){
        //Cчитываем знаечение полей и сохраняем в эти переменные
        String productName = product_name.getText().toString();
        String productId = product_id.getText().toString();

        SQLiteDatabase database = dbHelper.getWritableDatabase(); //Создается и обновляется новая таблица, если нет существующей

        //для добавления новых строк в таблицу
        ContentValues values = new ContentValues();

        //Разделяем действия по отдельным кнопкам
        switch (view.getId()){
            case R.id.add:
                values.put(DBHelper.KEY_PRODUCT_NAME, productName);
                values.put(DBHelper.KEY_PRODUCT_ID, productId);

                database.insert(DBHelper.TABLE_CONTACTS, null, values);
                break;
            case R.id.delete:
                database.delete(DBHelper.TABLE_CONTACTS, null, null);
                break;
        }

        dbHelper.close();
    }
}