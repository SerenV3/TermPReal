package com.example.alarm;

import android.content.Context;
import android.graphics.Color;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * [기존 주석] RecyclerView에 알람 목록을 표시하기 위한 어댑터.
 * RecyclerView에 알람 목록을 표시하기 위한 어댑터.
 *
 *  `ListAdapter`를 상속받아 `DiffUtil`을 사용합니다.
 *   `ListAdapter`는 리스트 데이터의 변경사항을 효율적으로 처리하여 RecyclerView의 성능을 향상시키는
 *   현대적인 방식의 어댑터입니다. DiffUtil이 백그라운드 스레드에서 이전 리스트와 새 리스트를 비교하여
 *   꼭 필요한 최소한의 업데이트(삽입, 삭제, 이동, 변경)만 계산해서 알려주므로,
 *   `notifyDataSetChanged()`를 호출하는 것보다 훨씬 효율적.
 */
public class AlarmAdapter extends ListAdapter<Alarm, AlarmAdapter.AlarmViewHolder> {

    public interface OnAlarmInteractionListener {
        void onAlarmToggled(Alarm alarm, boolean isEnabled);
        void onItemClick(int position);
        void onItemLongClick(int position);
    }

    private final OnAlarmInteractionListener interactionListener;
    private boolean isSelectionMode = false;
    private final SparseBooleanArray selectedItems = new SparseBooleanArray();

    public AlarmAdapter(@NonNull DiffUtil.ItemCallback<Alarm> diffCallback, OnAlarmInteractionListener listener) {
        super(diffCallback);
        this.interactionListener = listener;
    }

    /**
     * [기존 주석] 각 아이템 뷰의 UI 요소들을 보관하는 ViewHolder 클래스.
     */
    public class AlarmViewHolder extends RecyclerView.ViewHolder {
        final TextView amPmTextView;
        final TextView timeTextView;
        final TextView repeatDaysTextView;
        final SwitchCompat alarmSwitch;
        final Context context;

        public AlarmViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();

            amPmTextView = itemView.findViewById(R.id.amPmTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            repeatDaysTextView = itemView.findViewById(R.id.repeatDaysTextView);
            alarmSwitch = itemView.findViewById(R.id.alarmSwitch);

            setupClickListeners();
        }

        private void setupClickListeners() {
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && interactionListener != null) {
                    interactionListener.onItemClick(position);
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && interactionListener != null) {
                    interactionListener.onItemLongClick(position);
                    return true;
                }
                return false;
            });

            alarmSwitch.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && interactionListener != null) {
                    interactionListener.onAlarmToggled(getItem(position), alarmSwitch.isChecked());
                }
            });
        }

        /**
         * [기존 주석] ViewHolder에 데이터를 바인딩(연결)하는 메소드입니다.
         */
        void bind(Alarm alarm) {
            amPmTextView.setText(alarm.getAmPm());
            timeTextView.setText(alarm.getFormattedTime());
            alarmSwitch.setChecked(alarm.isEnabled());

            if (alarm.isRepeating()) {
                repeatDaysTextView.setVisibility(View.VISIBLE);
                StringJoiner joiner = new StringJoiner(", ");
                if (alarm.isSundayEnabled()) joiner.add("일");
                if (alarm.isMondayEnabled()) joiner.add("월");
                if (alarm.isTuesdayEnabled()) joiner.add("화");
                if (alarm.isWednesdayEnabled()) joiner.add("수");
                if (alarm.isThursdayEnabled()) joiner.add("목");
                if (alarm.isFridayEnabled()) joiner.add("금");
                if (alarm.isSaturdayEnabled()) joiner.add("토");
                repeatDaysTextView.setText(joiner.toString());
            } else {
                repeatDaysTextView.setVisibility(View.GONE);
            }

            updateSelectionState(getAdapterPosition());
        }

        private void updateSelectionState(int position) {
            if (isSelectionMode && selectedItems.get(position, false)) {
                itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.selected_item_background));
            } else {
                itemView.setBackgroundColor(Color.TRANSPARENT);
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
        holder.bind(getItem(position));
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        } else {
            holder.bind(getItem(position));
        }
    }

    public void setSelectionMode(boolean selectionMode) {
        this.isSelectionMode = selectionMode;
        if (!selectionMode) {
            clearSelection();
        }
        notifyDataSetChanged();
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
        notifyItemChanged(position, "PAYLOAD_SELECTION_CHANGED");
    }

    public void clearSelection() {
        selectedItems.clear();
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

    /**
     * [기존 주석] ListAdapter가 데이터 변경을 효율적으로 처리하기 위해 사용하는 DiffUtil.ItemCallback 구현체.
     */
    public static class AlarmDiff extends DiffUtil.ItemCallback<Alarm> {
        @Override
        public boolean areItemsTheSame(@NonNull Alarm oldItem, @NonNull Alarm newItem) {
            return oldItem.getId() == newItem.getId();
        }

        /**
         * [기존 주석, 내용 추가] 두 아이템의 '내용'이 동일한지 확인하는 로직입니다.
         * [새로운 내용] 알람 이름과 날씨 TTS 설정을 포함하여, 화면에 표시되거나 데이터에 영향을 미치는
         * 모든 속성을 비교해야 정확한 UI 업데이트가 이루어집니다.
         */
        @Override
        public boolean areContentsTheSame(@NonNull Alarm oldItem, @NonNull Alarm newItem) {
            return oldItem.getHour() == newItem.getHour() &&
                   oldItem.getMinute() == newItem.getMinute() &&
                   oldItem.isEnabled() == newItem.isEnabled() &&
                   Objects.equals(oldItem.getName(), newItem.getName()) && // 알람 이름 비교 추가
                   oldItem.isVibrationEnabled() == newItem.isVibrationEnabled() &&
                   Objects.equals(oldItem.getSoundUri(), newItem.getSoundUri()) &&
                   oldItem.isMondayEnabled() == newItem.isMondayEnabled() &&
                   oldItem.isTuesdayEnabled() == newItem.isTuesdayEnabled() &&
                   oldItem.isWednesdayEnabled() == newItem.isWednesdayEnabled() &&
                   oldItem.isThursdayEnabled() == newItem.isThursdayEnabled() &&
                   oldItem.isFridayEnabled() == newItem.isFridayEnabled() &&
                   oldItem.isSaturdayEnabled() == newItem.isSaturdayEnabled() &&
                   oldItem.isSundayEnabled() == newItem.isSundayEnabled() &&
                   oldItem.isWeatherTtsEnabled() == newItem.isWeatherTtsEnabled(); // 날씨 TTS 설정 비교 추가
        }
    }
}
