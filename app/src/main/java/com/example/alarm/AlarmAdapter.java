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
import java.util.StringJoiner;

/**
 * RecyclerView에 알람 목록을 표시하기 위한 어댑터.
 *
 * `ListAdapter`를 상속받아 `DiffUtil`을 사용합니다.
 * `ListAdapter`는 리스트 데이터의 변경사항을 효율적으로 처리하여 RecyclerView의 성능을 향상시키는
 * 현대적인 방식의 어댑터입니다. DiffUtil이 백그라운드 스레드에서 이전 리스트와 새 리스트를 비교하여
 * 꼭 필요한 최소한의 업데이트(삽입, 삭제, 이동, 변경)만 계산해서 알려주므로,
 * `notifyDataSetChanged()`를 호출하는 것보다 훨씬 효율적입니다.
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
     * 각 아이템 뷰의 UI 요소들을 보관하는 ViewHolder 클래스.
     */
    public class AlarmViewHolder extends RecyclerView.ViewHolder {
        // [기존 뷰 유지] 시간(오전/오후, 시:분)과 스위치
        final TextView amPmTextView;
        final TextView timeTextView;
        // [추가] 반복 요일을 표시할 TextView를 멤버 변수로 추가합니다.
        final TextView repeatDaysTextView;
        final SwitchCompat alarmSwitch;
        final Context context;

        public AlarmViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();

            // XML 레이아웃의 뷰들을 ID를 통해 코드와 연결합니다.
            amPmTextView = itemView.findViewById(R.id.amPmTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            // [추가] XML 레이아웃에 추가한 TextView(repeatDaysTextView)를 ViewHolder에 연결합니다.
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
         * [수정] ViewHolder에 데이터를 바인딩할 때, 반복 요일 정보도 업데이트하도록 호출을 추가합니다.
         */
        void bind(Alarm alarm) {
            updateTime(alarm);
            updateSwitch(alarm);
            // [추가] 반복 요일 정보를 TextView에 표시하는 메소드를 호출합니다.
            updateRepeatDays(alarm);
            updateSelectionState(getAdapterPosition());
        }

        void bind(Alarm alarm, List<Object> payloads) {
            if (payloads.isEmpty()) {
                bind(alarm);
                return;
            }

            for (Object payload : payloads) {
                if (payload.equals("PAYLOAD_SWITCH_CHANGED")) {
                    updateSwitch(alarm);
                }
                if (payload.equals("PAYLOAD_SELECTION_CHANGED")) {
                    updateSelectionState(getAdapterPosition());
                }
            }
        }

        private void updateTime(Alarm alarm) {
            amPmTextView.setText(alarm.getAmPm());
            timeTextView.setText(alarm.getFormattedTime());
        }

        private void updateSwitch(Alarm alarm) {
            alarmSwitch.setChecked(alarm.isEnabled());
        }

        /**
         * [추가] 알람 객체의 반복 요일 정보를 읽어와 TextView에 표시하는 새로운 헬퍼 메소드입니다.
         * @param alarm 표시할 Alarm 객체
         */
        private void updateRepeatDays(Alarm alarm) {
            if (alarm.isRepeating()) {
                // 1. 이 알람이 반복 알람인 경우, TextView를 화면에 보이도록 설정합니다.
                repeatDaysTextView.setVisibility(View.VISIBLE);

                // 2. 선택된 요일들을 "월, 화, 수" 형태의 문자열로 만듭니다.
                // StringJoiner는 문자열을 특정 구분자(여기서는 ", ")로 연결할 때 편리한 클래스입니다.
                StringJoiner joiner = new StringJoiner(", ");
                if (alarm.isMondayEnabled()) joiner.add("월");
                if (alarm.isTuesdayEnabled()) joiner.add("화");
                if (alarm.isWednesdayEnabled()) joiner.add("수");
                if (alarm.isThursdayEnabled()) joiner.add("목");
                if (alarm.isFridayEnabled()) joiner.add("금");
                if (alarm.isSaturdayEnabled()) joiner.add("토");
                if (alarm.isSundayEnabled()) joiner.add("일");

                // 3. 완성된 문자열을 TextView에 설정합니다.
                repeatDaysTextView.setText(joiner.toString());

            } else {
                // 4. 이 알람이 반복 알람이 아닌 경우, TextView를 화면에서 완전히 숨깁니다.
                repeatDaysTextView.setVisibility(View.GONE);
            }
        }

        private void updateSelectionState(int position) {
            if (isSelectionMode && selectedItems.get(position, false)) {
                // [수정] 직접 정의된 색상 리소스를 사용하도록 수정합니다.
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
        holder.bind(getItem(position), payloads);
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
     * `ListAdapter`가 데이터 변경을 효율적으로 처리하기 위해 사용하는 DiffUtil.ItemCallback 구현체.
     */
    public static class AlarmDiff extends DiffUtil.ItemCallback<Alarm> {
        @Override
        public boolean areItemsTheSame(@NonNull Alarm oldItem, @NonNull Alarm newItem) {
            return oldItem.getId() == newItem.getId();
        }

        /**
         * [수정] 두 아이템의 내용이 동일한지 확인하는 로직을 업데이트합니다.
         * 이제 시간, 활성화 상태뿐만 아니라, **모든 요일의 반복 여부**까지 비교해야
         * DiffUtil이 반복 설정 변경을 올바르게 감지하고 UI를 갱신할 수 있습니다.
         */
        @Override
        public boolean areContentsTheSame(@NonNull Alarm oldItem, @NonNull Alarm newItem) {
            return oldItem.getHour() == newItem.getHour() &&
                   oldItem.getMinute() == newItem.getMinute() &&
                   oldItem.isEnabled() == newItem.isEnabled() &&
                   oldItem.isMondayEnabled() == newItem.isMondayEnabled() &&
                   oldItem.isTuesdayEnabled() == newItem.isTuesdayEnabled() &&
                   oldItem.isWednesdayEnabled() == newItem.isWednesdayEnabled() &&
                   oldItem.isThursdayEnabled() == newItem.isThursdayEnabled() &&
                   oldItem.isFridayEnabled() == newItem.isFridayEnabled() &&
                   oldItem.isSaturdayEnabled() == newItem.isSaturdayEnabled() &&
                   oldItem.isSundayEnabled() == newItem.isSundayEnabled();
        }

        @Override
        public Object getChangePayload(@NonNull Alarm oldItem, @NonNull Alarm newItem) {
            if (oldItem.isEnabled() != newItem.isEnabled()) {
                return "PAYLOAD_SWITCH_CHANGED";
            }
            // [개선] 만약 요일 반복 설정만 변경되었다면, 해당 부분만 업데이트 하도록 payload를 추가할 수도 있습니다.
            // 여기서는 편의상 전체를 다시 그리도록 null을 반환합니다.
            return null;
        }
    }
}
