package com.example.testalarmreal02;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private FloatingActionButton btnAddAlarm;
    private RecyclerView recyclerView;
    private AlarmAdapter alarmAdapter;
    private List<Alarm> alarmList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnAddAlarm = findViewById(R.id.btn_add_alarm);
        recyclerView = findViewById(R.id.recycler_view);

        // RecyclerView 설정
        alarmAdapter = new AlarmAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(alarmAdapter);

        alarmList = new ArrayList<>();

        // 더미 데이터를 넣어서 리사이클러뷰가 잘 작동하는지 확인
        alarmList.add(new Alarm(9, 20));
        alarmList.add(new Alarm(10, 30));
        alarmList.add(new Alarm(13, 15));
        alarmList.add(new Alarm(18, 55));

        alarmAdapter.setAlarmList(alarmList);
        alarmAdapter.notifyDataSetChanged(); // RecyclerView에 데이터 변경 알림

    }
}
