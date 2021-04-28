package com.hanami.hook;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

/**
 * @author lidaisheng
 * @date 2021-04-28
 */
public class HookPluginManager {

    private static final String TAG = HookPluginManager.class.getName();

    public void hookAmsAction(final Application app) throws Exception {
        Class mIActivityManagerClass = Class.forName("android.app.IActivityManager");

        //通过ActivityManagerNative.getDefault拿到IActivityManager实例
        Class mActivityManagerNativeClass2 = Class.forName("android.app.ActivityManagerNative");
        final Object mIActivityManager = mActivityManagerNativeClass2.getMethod("getDefault").invoke(null);

        //动态代理
        Object mIActivityManagerProxy = Proxy.newProxyInstance(Application.class.getClassLoader()
                , new Class[]{mIActivityManagerClass}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if("startActivity".equals(method.getName())) {
                    Log.i(TAG, "拦截到IActivityManager的startActivity方法");
                    Intent intent = new Intent(app, ProxyActivity.class);
                    intent.putExtra("actionIntent", (Intent)args[2]);
                    args[2] = intent;
                }
                method.invoke(mIActivityManager, args);

                return null;
            }
        });


        //通过ActivityManagerNative拿到gDefault变量（对象）
        Field gDefaultField = mActivityManagerNativeClass2.getDeclaredField("gDefault");
        gDefaultField.setAccessible(true);
        Object gDefault = gDefaultField.get(null);

        //替换点
        Class mSingletonClass = Class.forName("android.util.Singleton");
        //替换此字段 mInstance
        Field mInstanceField = mSingletonClass.getDeclaredField("mInstance");
        mInstanceField.setAccessible(true);
        //替换
        mInstanceField.set(gDefault, mIActivityManagerProxy);

    }

    public void hookLaunchActivity() throws Exception {


        /**
         * 获取ActivityThread中的 H 类实例 mH
         *
         * 先通过 ActivityThread 的静态方法 currentActivityThread 拿到当前的 ActivityThread 实例
         *
         * 然后通过获取mH
         *
         */
        Class mActivityThreadClass = Class.forName("android.app.ActivityThread");
        Object mActivityThread = mActivityThreadClass.getMethod("currentActivityThread").invoke(null);
        Field mHField = mActivityThreadClass.getDeclaredField("mH");
        mHField.setAccessible(true);
        Handler mH = (Handler) mHField.get(mActivityThread);

        Field mHandlerCallbackField = Handler.class.getDeclaredField("mCallback");
        mHandlerCallbackField.setAccessible(true);
        mHandlerCallbackField.set(mH, new MyHandlerCallback(mH));

    }


    private void mergeDex(Application application) throws Exception {

        //第一步找到宿主的dexElements
        PathClassLoader pathClassLoader = (PathClassLoader) application.getClassLoader();
        Class mBaseDexClassLoaderClass = Class.forName("dalvik.system.BaseDexClassLoader");
        Field pathListField = mBaseDexClassLoaderClass.getDeclaredField("pathList");
        pathListField.setAccessible(true);
        Object mDexPathList = pathListField.get(pathClassLoader);
        Field dexElementsField = mDexPathList.getClass().getDeclaredField("dexElements");
        dexElementsField.setAccessible(true);
        //本质是 Element[] dexElements
        Object dexElements = dexElementsField.get(mDexPathList);

        //第二步 通过插件apk生成 插件的dexElement
        String pluginPath = application.getDir("plugin", Context.MODE_PRIVATE).getAbsolutePath() + File.separator + "plugin.apk";
        File pluginFile = new File(pluginPath);
        if(!pluginFile.exists()) {
            Log.e(TAG, "did not found the plugin file, error!");
            return;
        }
        File fileDir = application.getDir("pluginDir", Context.MODE_PRIVATE);
        DexClassLoader dexClassLoader = new DexClassLoader(pluginPath, fileDir.getAbsolutePath(), null, application.getClassLoader());

        //使用反射拿到DexClassLoader中的DexElement
        Class mBaseDexClassLoaderClassPlugin = Class.forName("dalvik.system.BaseDexClassLoader");
        Field pathListFieldPlugin = mBaseDexClassLoaderClassPlugin.getDeclaredField("pathList");
        pathListFieldPlugin.setAccessible(true);
        Object mDexPathListPlugin = pathListFieldPlugin.get(dexClassLoader);
        Field dexElementsFieldPlugin = mDexPathListPlugin.getClass().getDeclaredField("dexElements");
        dexElementsFieldPlugin.setAccessible(true);
        Object dexElementsPlugin = dexElementsFieldPlugin.get(mDexPathListPlugin);

        //合并
        int pluginDexLen = Array.getLength(dexElementsPlugin);
        int hostDexLen = Array.getLength(dexElements);
        int mergeLen = pluginDexLen + hostDexLen;

        //创建新的DexElement数组
        Object newDexElements = Array.newInstance(dexElements.getClass().getComponentType(), mergeLen);
        for(int index = 0; index < mergeLen; index++) {
            if(index < hostDexLen) {
                Array.set(newDexElements, index, Array.get(dexElements, index));
            } else {
                Array.set(newDexElements, index, Array.get(dexElementsPlugin, index - hostDexLen));
            }
        }

        //使用新的dexElement替换掉宿主中旧的
        dexElementsField.set(mDexPathList, newDexElements);

        //todo 处理加载的插件中的布局，和占位式一致
        //doPluginLayoutLoad()
    }



    static class MyHandlerCallback implements Handler.Callback {

        private static final int LAUNCH_ACTIVITY = 100;

        private Handler mH;

        public MyHandlerCallback(Handler handler) {
            this.mH = handler;
        }

        @Override
        public boolean handleMessage(Message msg)  {
            switch (msg.what) {
                /**
                 * 收到LAUNCH_ACTIVITY消息，将intent中携带的ProxyActivity替换成真正要启动的Activity
                 */
                case LAUNCH_ACTIVITY:
                    try {
                        /**
                         * 1、从obj的intent中拿出真实的actionIntent,
                         * 2、把真实的actionIntent赋值给obj的intent
                         */
                        Object obj = msg.obj;
                        Field intentField = obj.getClass().getDeclaredField("intent");
                        intentField.setAccessible(true);
                        Intent intent = (Intent) intentField.get(obj);
                        Intent actionIntent = intent.getParcelableExtra("actionIntent");
                        if(actionIntent != null) {
                            intentField.set(obj, actionIntent);
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, "hook error!");
                        ex.printStackTrace();
                    }
                    break;
                default:
                    break;
            }
            mH.handleMessage(msg);
            return true;
        }
    }




}
