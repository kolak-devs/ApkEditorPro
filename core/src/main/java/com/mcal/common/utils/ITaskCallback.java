package com.mcal.common.utils;

public interface ITaskCallback {

    void setTaskStepInfo(TaskStepInfo stepInfo);

    // Progress from 0 to 1
    void setTaskProgress(float progress);

    void taskSucceed();

    void taskFailed(String errMessage);

    // Call this function when not a genuine version
    void taskWarning(String message);

    class TaskStepInfo {
        public int stepIndex = 0;
        public int stepTotal;
        public String stepDescription;
    }
}
