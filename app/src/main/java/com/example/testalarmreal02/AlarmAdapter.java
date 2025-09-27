package com.example.testalarmreal02;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>{private List<Alarm> alarmList;

    public void setAlarmList(List<Alarm> alarmList) {
        this.alarmList = alarmList;
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
        holder.switchOnOff.setChecked(alarm.isOnOff());
    }

    @Override
    public int getItemCount() {
        return alarmList == null ? 0 : alarmList.size();
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
