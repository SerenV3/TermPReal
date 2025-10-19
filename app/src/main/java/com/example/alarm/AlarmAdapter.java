package com.example.alarm;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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

    /**
     * 어댑터와 상호작용(아이템 클릭, 롱클릭, 스위치 토글) 이벤트를
     * Activity(또는 Fragment)에 전달하기 위한 리스너 인터페이스.
     */
    public interface OnAlarmInteractionListener {
        void onAlarmToggled(Alarm alarm, boolean isEnabled);
        void onItemClick(int position);
        void onItemLongClick(int position);
    }

    private final OnAlarmInteractionListener interactionListener;

    // --- 다중 선택 상태 관리를 위한 변수 ---
    private boolean isSelectionMode = false;
    private final SparseBooleanArray selectedItems = new SparseBooleanArray();


    /**
     * 어댑터 생성자.
     * @param diffCallback 데이터 아이템의 변경사항을 계산하는 DiffUtil.ItemCallback 구현체.
     * @param listener Activity에서 구현한 리스너 객체.
     */
    public AlarmAdapter(@NonNull DiffUtil.ItemCallback<Alarm> diffCallback, OnAlarmInteractionListener listener) {
        super(diffCallback);
        this.interactionListener = listener;
    }

    /**
     * 각 아이템 뷰의 UI 요소들을 보관하는 ViewHolder 클래스.
     * `RecyclerView`는 화면에 보이지 않는 아이템의 뷰(ViewHolder)를 재활용하여
     * 메모리 사용량을 줄이고 스크롤 성능을 향상시킵니다.
     */
    public class AlarmViewHolder extends RecyclerView.ViewHolder {
        // [수정] XML 변경에 따라 TextView들을 분리
        final TextView amPmTextView;
        final TextView timeTextView;
        final SwitchCompat alarmSwitch;
        final Context context;

        public AlarmViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();
            // [수정] 변경된 ID로 뷰를 찾습니다.
            amPmTextView = itemView.findViewById(R.id.am_pm_text_view);
            timeTextView = itemView.findViewById(R.id.time_text_view);
            alarmSwitch = itemView.findViewById(R.id.alarm_switch);

            // --- 리스너 설정: ViewHolder 생성자에서 한 번만 설정하여 성능 최적화 ---
            setupClickListeners();
        }

        /**
         * 클릭/롱클릭 리스너를 설정합니다.
         * 이 메소드는 ViewHolder가 생성될 때 한 번만 호출되므로,
         * bind() 메소드에서 매번 리스너를 새로 생성하는 것보다 효율적입니다.
         */
        private void setupClickListeners() {
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                // getAdapterPosition()은 아이템이 삭제되는 중간 과정 등에서 NO_POSITION을 반환할 수 있으므로, 항상 유효성 검사를 해야 합니다.
                if (position != RecyclerView.NO_POSITION && interactionListener != null) {
                    interactionListener.onItemClick(position);
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && interactionListener != null) {
                    interactionListener.onItemLongClick(position);
                    return true; // true를 반환하여 롱클릭 이벤트를 여기서 소비(consume)하고,
                                 // 일반 클릭 이벤트가 연달아 호출되는 것을 막습니다.
                }
                return false;
            });

            // 스위치 클릭 리스너도 여기서 설정
            alarmSwitch.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && interactionListener != null) {
                    interactionListener.onAlarmToggled(getItem(position), alarmSwitch.isChecked());
                }
            });
        }

        /**
         * ViewHolder에 실제 데이터를 바인딩(연결)하는 메소드.
         * 스크롤 등으로 새로운 아이템이 화면에 표시되어야 할 때마다 호출됩니다.
         * @param alarm 표시할 Alarm 객체
         */
        void bind(Alarm alarm) {
            updateTime(alarm);
            updateSwitch(alarm);
            updateSelectionState(getAdapterPosition());
        }

        /**
         * Payload를 사용하여 ViewHolder의 일부만 업데이트합니다.
         * 이렇게 하면 뷰 전체를 다시 그리지 않고 변경된 부분만 효율적으로 업데이트할 수 있습니다.
         *
         * @param alarm 표시할 Alarm 객체
         * @param payloads DiffUtil에서 전달된 변경 정보 객체
         */
        void bind(Alarm alarm, List<Object> payloads) {
            if (payloads.isEmpty()) {
                // payload가 없으면 전체를 다시 바인딩합니다.
                bind(alarm);
                return;
            }

            // payload가 있으면, 변경된 부분만 선택적으로 업데이트합니다.
            for (Object payload : payloads) {
                if (payload.equals("PAYLOAD_SWITCH_CHANGED")) {
                    updateSwitch(alarm);
                }
                if (payload.equals("PAYLOAD_SELECTION_CHANGED")) {
                    updateSelectionState(getAdapterPosition());
                }
            }
        }

        // --- bind() 메소드의 책임을 분리한 헬퍼 메소드들 ---

        /**
         * [수정] Alarm 객체의 헬퍼 메소드를 사용하여 시간 관련 TextView들을 업데이트합니다.
         */
        private void updateTime(Alarm alarm) {
            // Alarm.java에 추가한 헬퍼 메소드를 사용하여 오전/오후와 시간 문자열을 가져옵니다.
            amPmTextView.setText(alarm.getAmPm());
            timeTextView.setText(alarm.getFormattedTime());
        }

        private void updateSwitch(Alarm alarm) {
            alarmSwitch.setChecked(alarm.isEnabled());
        }

        private void updateSelectionState(int position) {
            if (isSelectionMode && selectedItems.get(position, false)) {
                itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.selected_item_background));
            } else {
                itemView.setBackgroundColor(Color.TRANSPARENT);
            }
        }
    }


    // --- ListAdapter의 필수 오버라이드 메소드들 ---

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


    // --- 다중 선택 관련 public 메소드 ---

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
        /**
         * 두 아이템이 동일한 객체인지 확인합니다. (보통 고유 ID를 비교)
         * RecyclerView가 아이템의 이동, 삭제, 추가를 감지하는 데 사용됩니다.
         */
        @Override
        public boolean areItemsTheSame(@NonNull Alarm oldItem, @NonNull Alarm newItem) {
            return oldItem.getId() == newItem.getId();
        }

        /**
         * 두 아이템의 내용이 동일한지 확인합니다.
         * areItemsTheSame()이 true를 반환할 때만 호출됩니다.
         * RecyclerView가 아이템의 내용 변경(업데이트)을 감지하는 데 사용됩니다.
         * 이 메소드가 false를 반환하면, onBindViewHolder가 호출되어 뷰를 새로 그립니다.
         */
        @Override
        public boolean areContentsTheSame(@NonNull Alarm oldItem, @NonNull Alarm newItem) {
            return oldItem.getHour() == newItem.getHour() &&
                   oldItem.getMinute() == newItem.getMinute() &&
                   oldItem.isEnabled() == newItem.isEnabled();
        }

        /**
         * (선택적 최적화) areContentsTheSame()이 false일 때, 어떤 내용이 변경되었는지
         * 구체적인 정보를 담은 "payload" 객체를 반환할 수 있습니다.
         * 여기서 null이 아닌 객체를 반환하면, RecyclerView는 전체 뷰를 다시 그리는 onBindViewHolder(holder, position) 대신,
         * payload를 전달하는 onBindViewHolder(holder, position, payloads)를 호출합니다.
         */
        @Override
        public Object getChangePayload(@NonNull Alarm oldItem, @NonNull Alarm newItem) {
            if (oldItem.isEnabled() != newItem.isEnabled()) {
                return "PAYLOAD_SWITCH_CHANGED";
            }
            return null;
        }
    }
}
