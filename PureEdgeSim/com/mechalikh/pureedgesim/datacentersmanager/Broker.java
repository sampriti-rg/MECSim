package com.mechalikh.pureedgesim.datacentersmanager;

import com.mechalikh.pureedgesim.taskgenerator.Task;

import java.util.HashMap;

public class Broker {
    //messageId, task
    HashMap<String, Task> keyValue;
    public Broker(){
        keyValue = new HashMap<>();
    }
    public void addTask(String umi, Task task){
        keyValue.put(umi, task);
    }

    public Task getTask(Integer messageId){
        return keyValue.get(messageId);
    }
}