package org.dieschnittstelle.mobile.android.skeleton;

import androidx.appcompat.app.AppCompatActivity;
import org.dieschnittstelle.mobile.android.skeleton.model.Todo;

import android.os.Bundle;

import org.dieschnittstelle.mobile.android.skeleton.R;

import java.time.LocalDate;
import java.time.LocalTime;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Todo todo = new Todo();
        todo.setName("Einkaufen");
        todo.setDescription("Milch, Eier, Brot besorgen");
        todo.setCompleted(false);
        todo.setFavourite(true);
        todo.setDueDate(LocalDate.of(2023, 11, 15));
        todo.setDueTime(LocalTime.of(18, 0));

    }
}
