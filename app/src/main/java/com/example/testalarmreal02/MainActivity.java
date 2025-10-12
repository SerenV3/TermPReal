package com.example.testalarmreal02;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * 앱의 메인 화면.
 * ViewModel을 통해 데이터베이스의 알람 목록을 가져와 RecyclerView에 표시하고,
 * 사용자의 입력을 받아 알람을 추가/삭제하는 역할을 합니다.
 */
public class MainActivity extends AppCompatActivity {

    private FloatingActionButton btnAddAlarm;
    private RecyclerView recyclerView;
    private AlarmAdapter alarmAdapter;

    // UI 데이터를 관리할 ViewModel
    private AlarmViewModel alarmViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // EdgeToEdge.enable(this); // 이 코드는 제거하거나, 테마를 NoActionBar로 설정해야 합니다. 여기서는 주석 처리합니다.
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // --- 1. ViewModel 초기화 ---
        // ViewModelProvider를 통해 ViewModel 인스턴스를 가져옵니다.
        // 이렇게 하면 화면 회전과 같은 상황에도 데이터가 안전하게 보존됩니다.
        alarmViewModel = new ViewModelProvider(this).get(AlarmViewModel.class);

        // --- 2. UI 요소 초기화 및 RecyclerView 설정 ---
        btnAddAlarm = findViewById(R.id.btn_add_alarm);
        recyclerView = findViewById(R.id.recycler_view);

        alarmAdapter = new AlarmAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(alarmAdapter);

        // --- 3. LiveData 구독 (Observing) ---
        // ViewModel의 getAllAlarms()가 반환하는 LiveData를 구독합니다.
        // 데이터베이스에 변경이 생기면(추가, 수정, 삭제), 람다식 내부 코드가 자동으로 실행됩니다.
        alarmViewModel.getAllAlarms().observe(this, alarms -> {
            // 어댑터에 새로운 데이터 목록을 전달합니다.
            alarmAdapter.setAlarmList(alarms);
            alarmAdapter.notifyDataSetChanged(); // 데이터가 변경되었음을 어댑터에 알립니다.
        });

        // --- 4. 사용자 입력 처리 ---
        // 알람 추가 버튼 클릭 리스너
        btnAddAlarm.setOnClickListener(view -> {
            // "새로운 알람을 추가합니다" 라는 간단한 메시지를 표시합니다.
            Toast.makeText(this, "오전 7시 알람을 추가합니다.", Toast.LENGTH_SHORT).show();
            // ViewModel에 새로운 알람(예: 오전 7시)을 삽입하도록 요청합니다.
            alarmViewModel.insert(new Alarm(7, 0));
        });

        // --- 5. (삭제 기능) RecyclerView에 스와이프 기능 추가 ---
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false; // 드래그 앤 드롭은 사용하지 않음
            }

            // 사용자가 아이템을 왼쪽으로 스와이프했을 때 호출됩니다.
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // 스와이프된 위치의 알람 객체를 가져옵니다.
                Alarm alarmToDelete = alarmAdapter.getAlarmAt(viewHolder.getAdapterPosition());
                // ViewModel에 해당 알람을 삭제하도록 요청합니다.
                alarmViewModel.delete(alarmToDelete);
                Toast.makeText(MainActivity.this, "알람이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }).attachToRecyclerView(recyclerView); // ItemTouchHelper를 RecyclerView에 연결
    }
}
