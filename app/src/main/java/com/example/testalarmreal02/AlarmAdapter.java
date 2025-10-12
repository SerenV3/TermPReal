package com.example.testalarmreal02;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder> {
    private List<Alarm> alarmList;

    public void setAlarmList(List<Alarm> alarmList) {
        this.alarmList = alarmList;
    }

    // [추가] 특정 위치의 알람 객체를 반환하는 메소드 (삭제 기능에 사용)
    public Alarm getAlarmAt(int position) {
        return alarmList.get(position);
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alarm, parent, false);
        return new AlarmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        Alarm alarm = alarmList.get(position);
        holder.tvAmPm.setText(alarm.getAmPm());
        holder.tvTime.setText(alarm.getFormattedTime());
        // [수정] Room Entity에 맞게 isEnabled() 메소드를 사용합니다.
        holder.switchOnOff.setChecked(alarm.isEnabled());
    }

    @Override
    public int getItemCount() {
        return alarmList != null ? alarmList.size() : 0;
    }

    public class AlarmViewHolder extends RecyclerView.ViewHolder {
        TextView tvAmPm, tvTime;
        SwitchMaterial switchOnOff;
        public AlarmViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAmPm = itemView.findViewById(R.id.tv_am_pm);
            tvTime = itemView.findViewById(R.id.tv_time);
            switchOnOff = itemView.findViewById(R.id.switch_on_off);
        }
    }
}
