package com.cappielloantonio.tempo.ui.dialog;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.service.BaseMediaService;
import com.cappielloantonio.tempo.util.Preferences;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public class SleepTimerDialog extends DialogFragment {
    private TextView sleepTimerStatusLabel;
    private NumberPicker sleepTimerMinutesPicker;
    private View sleepTimerPickerContainer;

    private Handler updateHandler;
    private Runnable updateRunnable;

    private final String[] minuteValues = {"5", "10", "15", "30", "45", "60", "90", "120"};

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_sleep_timer, null);

        sleepTimerStatusLabel = dialogView.findViewById(R.id.sleep_timer_status_label);
        sleepTimerMinutesPicker = dialogView.findViewById(R.id.sleep_timer_minutes_picker);
        sleepTimerPickerContainer = dialogView.findViewById(R.id.sleep_timer_picker_container);

        setupPicker();

        boolean timerActive = Preferences.isSleepTimerActive();
        boolean endOfTrack = Preferences.isSleepTimerEndOfTrack();
        long remaining = Preferences.getSleepTimerRemainingMillis();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity())
                .setView(dialogView)
                .setTitle(R.string.sleep_timer_title);

        if (timerActive && endOfTrack) {
            showEndOfTrackUI(builder);
        } else if (timerActive && remaining > 0) {
            showActiveTimerUI(builder, remaining);
        } else {
            showInactiveTimerUI(builder);
        }

        return builder.create();
    }

    private void setupPicker() {
        String[] displayValues = new String[minuteValues.length + 1];
        for (int i = 0; i < minuteValues.length; i++) {
            displayValues[i] = minuteValues[i] + " min";
        }
        displayValues[minuteValues.length] = getString(R.string.sleep_timer_end_of_track);

        sleepTimerMinutesPicker.setMinValue(0);
        sleepTimerMinutesPicker.setMaxValue(displayValues.length - 1);
        sleepTimerMinutesPicker.setDisplayedValues(displayValues);
        sleepTimerMinutesPicker.setWrapSelectorWheel(false);

        int lastSelection = Preferences.getSleepTimerLastSelection();
        if (lastSelection >= 0 && lastSelection < displayValues.length) {
            sleepTimerMinutesPicker.setValue(lastSelection);
        }
    }

    private void showInactiveTimerUI(MaterialAlertDialogBuilder builder) {
        sleepTimerStatusLabel.setVisibility(View.GONE);
        sleepTimerPickerContainer.setVisibility(View.VISIBLE);

        builder.setPositiveButton(R.string.sleep_timer_start_button, (dialog, id) -> {
            int selectedIndex = sleepTimerMinutesPicker.getValue();
            if (selectedIndex < minuteValues.length) {
                int minutes = Integer.parseInt(minuteValues[selectedIndex]);
                startSleepTimer(minutes);
            } else {
                startSleepTimerEndOfTrack();
            }
            Preferences.setSleepTimerLastSelection(selectedIndex);
        });
        builder.setNegativeButton(R.string.sleep_timer_cancel_button, (dialog, id) -> dialog.cancel());
    }

    private void showActiveTimerUI(MaterialAlertDialogBuilder builder, long remainingMillis) {
        sleepTimerPickerContainer.setVisibility(View.GONE);
        sleepTimerStatusLabel.setVisibility(View.VISIBLE);

        String remainingText = formatRemainingTime(remainingMillis);
        sleepTimerStatusLabel.setText(getString(R.string.sleep_timer_remaining, remainingText));

        builder.setPositiveButton(R.string.sleep_timer_stop_button, (dialog, id) -> cancelSleepTimer());
        builder.setNegativeButton(R.string.sleep_timer_cancel_button, (dialog, id) -> dialog.cancel());

        startRemainingTimeUpdates();
    }

    private void showEndOfTrackUI(MaterialAlertDialogBuilder builder) {
        sleepTimerPickerContainer.setVisibility(View.GONE);
        sleepTimerStatusLabel.setVisibility(View.VISIBLE);
        sleepTimerStatusLabel.setText(R.string.sleep_timer_end_of_track_active);

        builder.setPositiveButton(R.string.sleep_timer_stop_button, (dialog, id) -> cancelSleepTimer());
        builder.setNegativeButton(R.string.sleep_timer_cancel_button, (dialog, id) -> dialog.cancel());
    }

    private void startRemainingTimeUpdates() {
        updateHandler = new Handler(Looper.getMainLooper());
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (sleepTimerStatusLabel != null) {
                    long remaining = Preferences.getSleepTimerRemainingMillis();
                    if (remaining > 0) {
                        sleepTimerStatusLabel.setText(getString(R.string.sleep_timer_remaining, formatRemainingTime(remaining)));
                        updateHandler.postDelayed(this, 1000);
                    } else {
                        dismiss();
                    }
                }
            }
        };
        updateHandler.postDelayed(updateRunnable, 1000);
    }

    private String formatRemainingTime(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    private void startSleepTimer(int minutes) {
        Preferences.setSleepTimerActive(true);
        Preferences.setSleepTimerRemainingMillis(minutes * 60_000L);
        Preferences.setSleepTimerEndOfTrack(false);
        Preferences.setSleepTimerStartTime(System.currentTimeMillis());
        Preferences.setSleepTimerDurationMinutes(minutes);

        requireActivity().sendBroadcast(
                new Intent(BaseMediaService.ACTION_SLEEP_TIMER_START)
                        .putExtra("duration_minutes", minutes)
                        .setPackage(requireActivity().getPackageName())
        );
    }

    private void startSleepTimerEndOfTrack() {
        Preferences.setSleepTimerActive(true);
        Preferences.setSleepTimerRemainingMillis(-1);
        Preferences.setSleepTimerEndOfTrack(true);

        requireActivity().sendBroadcast(
                new Intent(BaseMediaService.ACTION_SLEEP_TIMER_END_OF_TRACK)
                        .setPackage(requireActivity().getPackageName())
        );
    }

    private void cancelSleepTimer() {
        Preferences.setSleepTimerActive(false);
        Preferences.setSleepTimerRemainingMillis(0);
        Preferences.setSleepTimerEndOfTrack(false);

        requireActivity().sendBroadcast(
                new Intent(BaseMediaService.ACTION_SLEEP_TIMER_CANCEL)
                        .setPackage(requireActivity().getPackageName())
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (updateHandler != null && updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
    }
}
