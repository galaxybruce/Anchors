package com.effective.android.anchors;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.List;

public class Utils {

    public static void insertAfterTask(@NonNull Task insert, @NonNull Task targetTask){
        if(insert == null  || targetTask == null){
            return;
        }
        List<Task> taskBehinds = targetTask.getBehindTasks();
        for(Task behind: taskBehinds){
            behind.removeDepend(targetTask);
            insert.behind(behind);
        }
        targetTask.getBehindTasks().clear();
        insert.dependOn(targetTask);
    }

    /**
     * 比较两个 task
     * {@link Task#getPriority()} 值高的，优先级高
     * {@link Task#getExecuteTime()} 添加到队列的时间最早，优先级越高
     *
     * @param task
     * @param o
     * @return
     */
    public static int compareTask(@NonNull Task task, @NonNull Task o) {
        if (task.getPriority() < o.getPriority()) {
            return 1;
        }
        if (task.getPriority() > o.getPriority()) {
            return -1;
        }
        if (task.getExecuteTime() < o.getExecuteTime()) {
            return -1;
        }
        if (task.getExecuteTime() > o.getExecuteTime()) {
            return 1;
        }
        return 0;
    }


    public static void assertMainThread() {
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            throw new RuntimeException("AnchorsManager#start should be invoke on MainThread!");
        }
    }

    /**
     * 包名判断是否为主进程
     */
    public static boolean isMainProcess() {
        return TextUtils.equals(AnchorsManager.instance().application.getPackageName(), getProcessName());
    }

    /**
     * 获取进程全名
     */
    static String getProcessName() {
        ActivityManager am = (ActivityManager) AnchorsManager.instance().application.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) {
            return "";
        }
        List<ActivityManager.RunningAppProcessInfo> runningApps = am.getRunningAppProcesses();
        if (runningApps == null) {
            return "";
        }
        for (ActivityManager.RunningAppProcessInfo proInfo : runningApps) {
            if (proInfo.pid == android.os.Process.myPid()) {
                if (proInfo.processName != null) {
                    return proInfo.processName;
                }
            }
        }
        return "";
    }
}
