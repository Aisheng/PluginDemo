package com.hanami.plugin.core;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lidaisheng
 * @date 2021-05-08
 */
public class UserManager {

    private List<User> users;
    private Handler mHandler;

    private static UserManager instance;

    private UserManager() {
        users = new ArrayList<>();
        mHandler = new Handler(Looper.getMainLooper());
    }

    public static UserManager getInstance() {
        if(instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    public void addUser(User user) {
        users.add(user);
    }

    public User find(String name) {
        for (User user : users) {
            if(user.getName().equals(name)) {
                return user;
            }
        }
        return null;
    }

    public void work() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                for (User user : users) {
                    System.out.println(user.getAge());
                }
            }
        });
    }

}
