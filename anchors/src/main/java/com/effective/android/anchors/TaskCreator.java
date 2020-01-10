package com.effective.android.anchors;

import androidx.annotation.NonNull;

public interface TaskCreator {

    @NonNull
    Task createTask(String taskName);
}
