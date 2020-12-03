package com.example.nfc;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button add, delete;
    EditText product_name, product_id;

    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Устанавливаем кнопки и поля
        add = (Button)findViewById(R.id.add);
        add.setOnClickListener(this);
        delete = (Button)findViewById(R.id.delete);
        delete.setOnClickListener(this);

        product_id = (EditText)findViewById(R.id.product_id);
        product_id.setOnClickListener(this);
        product_name = (EditText)findViewById(R.id.product_name);
        product_name.setOnClickListener(this);

        dbHelper = new DBHelper(this);
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