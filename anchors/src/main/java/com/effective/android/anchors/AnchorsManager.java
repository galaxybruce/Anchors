package com.effective.android.anchors;


import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.app.Application;
import android.text.TextUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AnchorsManager {

    public boolean debuggable = false;
    public Application application;

    public Set<String> anchorTaskIds = new HashSet<>();
    public boolean isMainProcess;

    private AnchorsManager() {
    }

    private static class SingletonHolder {
        private static final AnchorsManager INSTANCE = new AnchorsManager();
    }

    public static AnchorsManager instance() {
        return SingletonHolder.INSTANCE;
    }

    public AnchorsManager setApplication(Application application) {
        this.application = application;
        return this;
    }

    public AnchorsManager debuggable(boolean debuggable) {
        this.debuggable = debuggable;
        return this;
    }

    /**
     * 扩展支持 https://github.com/YummyLau/Anchors/issues/7   暂停机制
     * 调用前须知：
     * 1. 请充分理解 anchor 的作用并明白，为何 application sleep 频繁等待代码块执行的原因
     * 2. 如果调用 requestBlockWhenFinish 则意味着任务链在 task 执行完毕之后会进入等待阶段，如果此时等待的 task 在[初始节点，Anchors]链中则可能导致界面卡主
     * 3. 在调用 requestBlockWhenFinish 设置等待任务的前提下务必保证 anchors 已经解锁 或者 任务链上没有 anchors。
     * @param task block目标task
     * @return
     */
    @Nullable
    public LockableAnchor requestBlockWhenFinish(Task task) {
        return requestBlockWhenFinishInner(task);
    }

    @Nullable
    LockableAnchor requestBlockWhenFinishInner(Task task) {
        if (task != null && !TextUtils.isEmpty(task.getId())) {
            LockableAnchor lockableAnchor = new LockableAnchor(AnchorsRuntime.getHandler());
            LockableTask lockableTask = new LockableTask(task, lockableAnchor);
            Utils.insertAfterTask(lockableTask,task);
            return lockableAnchor;
        }
        return null;
    }

    public AnchorsManager addAnchor(String taskId) {
        if (!TextUtils.isEmpty(taskId)) {
            anchorTaskIds.add(taskId);
        }
        return this;
    }

    public AnchorsManager addAnchors(String... taskIds) {
        if (taskIds != null && taskIds.length > 0) {
            for (String id : taskIds) {
                anchorTaskIds.add(id);
            }
        }
        return this;
    }


    void syncConfigInfoToRuntime(){
        AnchorsRuntime.clear();
        AnchorsRuntime.openDebug(debuggable);
        AnchorsRuntime.addAnchorTasks(anchorTaskIds);
        debuggable = false;
        anchorTaskIds.clear();
    }

    @MainThread
    public synchronized void start(@NonNull TaskFactory taskFactory, Task... tasks) {
        final Project.DEF_START_TASK startTask = new Project.DEF_START_TASK();
        List<Project> projects = taskFactory.initProjects();
        if(projects != null && !projects.isEmpty()) {
            for (Project project : projects) {
                project.dependOn(startTask);
            }
        }
        if(tasks != null && tasks.length > 0) {
            for (Task task : tasks) {
                task.dependOn(startTask);
            }
        }

        start(startTask);
    }

    @MainThread
    public synchronized void start(@NonNull Task task) {
        Utils.assertMainThread();
        if (task == null) {
            throw new RuntimeException("can no run a task that was null !");
        }
        isMainProcess = Utils.isMainProcess();
        syncConfigInfoToRuntime();
        Task startTask = task;
        if(startTask instanceof Project){
            startTask = ((Project)task).getStartTask();
        }
        AnchorsRuntime.traversalDependenciesAndInit(startTask);
        boolean logEnd = logStartWithAnchorsInfo();
        startTask.start();
        while (AnchorsRuntime.hasAnchorTasks()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (AnchorsRuntime.hasRunTasks()) {
                AnchorsRuntime.tryRunBlockRunnable();
            }
        }
        if (logEnd) {
            logEndWithAnchorsInfo();
        }
    }

    /**
     * 打印锚点信息
     *
     * @return
     */
    private static boolean logStartWithAnchorsInfo() {
        if (!AnchorsRuntime.debuggable()) {
            return false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        boolean hasAnchorTask = AnchorsRuntime.hasAnchorTasks();
        if (hasAnchorTask) {
            stringBuilder.append(Constants.HAS_ANCHOR);
            stringBuilder.append("( ");
            for (String taskId : AnchorsRuntime.getAnchorTasks()) {
                stringBuilder.append("\"" + taskId + "\" ");
            }
            stringBuilder.append(")");
        } else {
            stringBuilder.append(Constants.NO_ANCHOR);
        }
        Logger.d(Constants.ANCHORS_INFO_TAG, stringBuilder.toString());
        return hasAnchorTask;
    }

    /**
     * 打印锚点信息
     */
    private static void logEndWithAnchorsInfo() {
        if (!AnchorsRuntime.debuggable()) {
            return;
        }
        Logger.d(Constants.ANCHORS_INFO_TAG, Constants.ANCHOR_RELEASE);
    }
}
