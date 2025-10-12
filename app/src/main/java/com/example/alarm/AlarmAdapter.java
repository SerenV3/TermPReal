package com.example.alarm;

import android.content.Context;
import android.graphics.Color; // 선택된 아이템 배경색 변경용
import android.text.format.DateFormat;
import android.util.SparseBooleanArray; // 선택 상태 저장용
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView; // CardView 참조용
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AlarmAdapter extends ListAdapter<Alarm, AlarmAdapter.AlarmViewHolder> {

    // --- 리스너 인터페이스 정의 ---
    public interface OnAlarmInteractionListener {
        void onAlarmToggled(Alarm alarm, boolean isEnabled);
        void onItemClick(int position);
        void onItemLongClick(int position);
    }

    private final OnAlarmInteractionListener interactionListener;
    private boolean isSelectionMode = false;
    private final SparseBooleanArray selectedItems = new SparseBooleanArray();

    // --- 생성자 수정 ---
    public AlarmAdapter(@NonNull DiffUtil.ItemCallback<Alarm> diffCallback, OnAlarmInteractionListener listener) {
        super(diffCallback);
        this.interactionListener = listener;
    }

    // --- ViewHolder 정의 ---
    public class AlarmViewHolder extends RecyclerView.ViewHolder {
        private final TextView alarmTimeTextView;
        private final SwitchCompat alarmSwitch;
        private final Context context;
        private final CardView cardView; // 아이템 루트 뷰 (배경색 변경용)

        public AlarmViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();
            alarmTimeTextView = itemView.findViewById(R.id.alarm_time_text_view);
            alarmSwitch = itemView.findViewById(R.id.alarm_switch);

            if (itemView instanceof CardView) {
                cardView = (CardView) itemView;
            } else {
                cardView = null;
            }

            // --- 클릭 및 롱클릭 리스너 수정 ---
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && interactionListener != null) {
                    // 그냥 Activity에 클릭 이벤트가 발생했다고 알리기만 함
                    interactionListener.onItemClick(position);
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && interactionListener != null) {
                    // 그냥 Activity에 롱클릭 이벤트가 발생했다고 알리기만 함
                    interactionListener.onItemLongClick(position);
                    return true; // 이벤트 소비
                }
                return false;
            });
        }

        public void bind(Alarm alarm) {
            // 시스템 설정에 따라 시간 표시 (12시간제 AM/PM 또는 24시간제)
            String timeToDisplay;
            if (DateFormat.is24HourFormat(context)) {
                timeToDisplay = String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute);
            } else {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, alarm.hour);
                calendar.set(Calendar.MINUTE, alarm.minute);
                SimpleDateFormat sdf12 = new SimpleDateFormat("h:mm a", Locale.getDefault());
                timeToDisplay = sdf12.format(calendar.getTime());
            }
            alarmTimeTextView.setText(timeToDisplay);

            // 스위치 리스너 로직 (기존과 거의 동일, interactionListener 참조만 변경)
            alarmSwitch.setOnCheckedChangeListener(null);
            alarmSwitch.setChecked(alarm.isEnabled);
            alarmSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed() && alarm.isEnabled != isChecked && interactionListener != null) {
                    interactionListener.onAlarmToggled(alarm, isChecked);
                }
            });

            // --- 선택 상태에 따른 UI 변경 ---
            int position = getAdapterPosition();
            if (isSelectionMode() && selectedItems.get(position, false)) {
                // 선택된 상태
                if (cardView != null) {
                    cardView.setCardBackgroundColor(Color.LTGRAY); // 예시: 밝은 회색
                } else {
                    itemView.setBackgroundColor(Color.LTGRAY);
                } // CardView가 아닌 경우 대비
            } else {
                // 선택되지 않은 상태
                if (cardView != null) {
                    cardView.setCardBackgroundColor(Color.WHITE); // 기본 배경색
                } else {
                    itemView.setBackgroundColor(Color.TRANSPARENT);
                }
            }
        }
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alarm, parent, false);
        return new AlarmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        Alarm currentAlarm = getItem(position);
        holder.bind(currentAlarm);
    }

    // --- 다중 선택 관련 public 메소드 ---

    public void setSelectionMode(boolean selectionMode) {
        this.isSelectionMode = selectionMode;
        if (!selectionMode) {
            clearSelection();
        }
        notifyDataSetChanged(); // 전체 뷰 갱신
    }

    public boolean isSelectionMode() {
        return isSelectionMode;
    }

    public void toggleSelection(int position) {
        if (selectedItems.get(position, false)) {
            selectedItems.delete(position);
        } else {
            selectedItems.put(position, true);
        }
        notifyItemChanged(position);
    }

    public void clearSelection() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    public List<Alarm> getSelectedAlarms() {
        List<Alarm> alarms = new ArrayList<>();
        for (int i = 0; i < selectedItems.size(); i++) {
            alarms.add(getItem(selectedItems.keyAt(i)));
        }
        return alarms;
    }

    // --- DiffUtil.ItemCallback (기존과 동일) ---
    public static class AlarmDiff extends DiffUtil.ItemCallback<Alarm> {
        @Override
        public boolean areItemsTheSame(@NonNull Alarm oldItem, @NonNull Alarm newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Alarm oldItem, @NonNull Alarm newItem) {
            return oldItem.hour == newItem.hour &&
                   oldItem.minute == newItem.minute &&
                   oldItem.isEnabled == newItem.isEnabled;
        }
    }
}
