package com.effective.android.anchors;

import android.app.Application;
import android.os.Build;
import android.os.Trace;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static com.effective.android.anchors.AnchorsRuntime.*;

/**
 * created by yummylau on 2019/03/11
 */
public abstract class Task implements Runnable, Comparable<Task> {

    @TaskState
    private int mState = TaskState.IDLE;           //状态
    private String mId;                            //mId,唯一存在
    private boolean isAsyncTask;                   //是否是异步存在
    private int mPriority;                         //优先级，数值越低，优先级越低
    private long mExecuteTime;
    public Process process;                         // 指定在哪个进程初始化


    public static final int DEFAULT_PRIORITY = 0;
    private List<Task> behindTasks = new ArrayList<>();                                //被依赖者
    private volatile Set<Task> dependTasks = new HashSet<>();                                   //依赖者
    private List<TaskListener> taskListeners = new ArrayList<>();                      //监听器
    private TaskListener logTaskListeners = new LogTaskListener();


    public Task(String id) {
        this(id, false, Process.MAIN);
    }

    public Task(String id, boolean async) {
        this(id, async, Process.MAIN);
    }

    public Task(boolean async, Process process) {
        this(null, async, process);
    }

    public Task(String id, boolean async, Process process) {
        this.mId = TextUtils.isEmpty(id) ? this.getClass().getSimpleName() : id;
        this.isAsyncTask = async;
        this.process = process;
        this.mPriority = DEFAULT_PRIORITY;
        if (TextUtils.isEmpty(mId)) {
            throw new IllegalArgumentException("task's mId can't be empty");
        }
    }

    public long getExecuteTime() {
        return mExecuteTime;
    }

    protected void setExecuteTime(long mExecuteTime) {
        this.mExecuteTime = mExecuteTime;
    }

    public String getId() {
        return mId;
    }

    public Task setPriority(int priority) {
        this.mPriority = priority;
        return this;
    }

    public int getPriority() {
        return mPriority;
    }

    public boolean isAsyncTask() {
        return isAsyncTask;
    }

    public int getState() {
        return mState;
    }

    protected void setState(@TaskState int state) {
        this.mState = state;
    }

    public void addTaskListener(TaskListener taskListener) {
        if (taskListener != null && !taskListeners.contains(taskListener)) {
            taskListeners.add(taskListener);
        }
    }

    protected synchronized void start() {
        if (mState != TaskState.IDLE) {
            throw new RuntimeException("can no run task " + getId() + " again!");
        }
        toStart();
        setExecuteTime(System.currentTimeMillis());
        executeTask(this);
    }

    @Override
    public void run() {
        if (debuggable() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Trace.beginSection(mId);
        }
        toRunning();
        if(!isIgnoreExecute()) {
            run(mId, AnchorsManager.instance().application);
        }
        toFinish();
        notifyBehindTasks();
        recycle();
        if (debuggable() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Trace.endSection();
        }
    }

    protected abstract void run(String name, Application application);

    void toStart() {
        setState(TaskState.START);
        setStateInfo(this);
        if (debuggable()) {
            logTaskListeners.onStart(this);
        }
        for (TaskListener listener : taskListeners) {
            listener.onStart(this);
        }
    }

    void toRunning() {
        setState(TaskState.RUNNING);
        setStateInfo(this);
        setThreadName(this, Thread.currentThread().getName());
        if (debuggable()) {
            logTaskListeners.onRunning(this);
        }
        for (TaskListener listener : taskListeners) {
            listener.onRunning(this);
        }

    }

    void toFinish() {
        setState(TaskState.FINISHED);
        setStateInfo(this);
        removeAnchorTask(mId);
        if (debuggable()) {
            logTaskListeners.onFinish(this);
        }
        for (TaskListener listener : taskListeners) {
            listener.onFinish(this);
        }
    }

    public Set<String> getDependTaskName() {
        Set<String> result = new HashSet<>();
        for (Task task : dependTasks) {
            result.add(task.mId);
        }
        return result;
    }

    public List<Task> getBehindTasks() {
        return behindTasks;
    }

    public void removeDepend(Task originTask) {
        if (dependTasks.contains(originTask)) {
            dependTasks.remove(originTask);
        }
    }

    public void updateBehind(Task updateTask, Task originTask) {
        if (behindTasks.contains(originTask)) {
            behindTasks.remove(originTask);
        }
        behindTasks.add(updateTask);
    }

    /**
     * 后置触发, 和 {@link Task#dependOn(Task)} 方向相反，都可以设置依赖关系
     *
     * @param task
     */
    protected void behind(@NonNull Task task) {
        if (task != null && task != this) {
            if (task instanceof Project) {
                task = ((Project) task).getStartTask();
            }
            behindTasks.add(task);
            task.dependOn(this);
        }
    }

    protected void removeBehind(@NonNull Task task) {
        if (task != null && task != this) {
            if (task instanceof Project) {
                task = ((Project) task).getStartTask();
            }
            behindTasks.remove(task);
            task.removeDependence(this);
        }
    }

    /**
     * 前置条件, 和 {@link Task#behind(Task)} 方向相反，都可以设置依赖关系
     *
     * @param task
     */
    public void dependOn(@NonNull Task task) {
        if (task != null && task != this) {
            if (task instanceof Project) {
                task = ((Project) task).getEndTask();
            }
            dependTasks.add(task);
            //防止外部所有直接调用dependOn无法构建完整图
            if (!task.behindTasks.contains(this)) {
                task.behindTasks.add(this);
            }
        }
    }

    protected void removeDependence(@NonNull Task task) {
        if (task != null && task != this) {
            if (task instanceof Project) {
                task = ((Project) task).getEndTask();
            }
            dependTasks.remove(task);
            if (task.behindTasks.contains(this)) {
                task.behindTasks.remove(this);
            }
        }
    }

    @Override
    public int compareTo(@NonNull Task o) {
        return Utils.compareTask(this, o);
    }


    /**
     * 通知后置者自己已经完成了
     */
    void notifyBehindTasks() {

        if (this instanceof LockableTask) {
            if (!((LockableTask) this).successToUnlock()) {
                return;
            }
        }

        if (!behindTasks.isEmpty()) {

            if (behindTasks.size() > 1) {
                Collections.sort(behindTasks, getTaskComparator());
            }

            //遍历记下来的任务，通知它们说存在的前置已经完成
            for (Task task : behindTasks) {
                task.dependTaskFinish(this);
            }
        }
    }

    public Set<Task> getDependTasks() {
        return dependTasks;
    }

    /**
     * 依赖的任务已经完成
     * 比如 B -> A (B 依赖 A), A 完成之后调用该方法通知 B "A依赖已经完成了"
     * 当且仅当 B 的所有依赖都已经完成了, B 开始执行
     *
     * @param dependTask
     */
    synchronized void dependTaskFinish(Task dependTask) {

        if (dependTasks.isEmpty()) {
            return;
        }
        dependTasks.remove(dependTask);

        //所有前置任务都已经完成了
        if (dependTasks.isEmpty()) {
            start();
        }
    }

    public static void doJob(long millis) {
        long nowTime = System.currentTimeMillis();
        while (System.currentTimeMillis() < nowTime + millis) {
            //程序阻塞指定时间
            int min = 10;
            int max = 99;
            Random random = new Random();
            int num = random.nextInt(max) % (max - min + 1) + min;
        }
    }


    void recycle() {
        getTaskRuntimeInfo(mId).clearTask();
        dependTasks.clear();
        behindTasks.clear();
        taskListeners.clear();
        logTaskListeners = null;
    }

    public boolean isForMainProcess() {
        return Process.MAIN == process || isForAllProcess();
    }

    public boolean isNotForMainProcess() {
        return Process.OTHER == process || isForAllProcess();
    }

    private boolean isForAllProcess() {
        return Process.ALL == process;
    }

    /**
     * 是否忽略该初始化
     */
    public boolean isIgnoreExecute() {
        boolean execute = (AnchorsManager.instance().isMainProcess && isForMainProcess()) ||
                (!AnchorsManager.instance().isMainProcess && isNotForMainProcess());
        return !execute;
    }
}
