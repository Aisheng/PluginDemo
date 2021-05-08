package com.hanami.plugin2

import com.hanami.plugin2.tool.Logger
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod

/**
 *
 * @author lidaisheng* @date 2021-05-08
 */

class ClassHandler {

    private static final String[] injectMethods = ["onResume", "onPause"]

    private static ClassHandler instance

    private ClassHandler() {
        mPathSet = new HashSet<>()

    }

    public static ClassHandler getInstance() {
        if(instance == null) {
            instance = new ClassHandler()
        }
        return instance
    }


    private static final ClassPool pool = ClassPool.getDefault()
    Set<String> mPathSet


    void appendClassPath(String path) {
        pool.appendClassPath(path)
        mPathSet.add(path)
    }

    void handle() {
        if(mPathSet.size() > 0) {
            for(String path : mPathSet) {
                File file = new File(path)
                //eachFileRecurse 是递归遍历目录及子目录下的文件的
                file.eachFileRecurse {
                    if(it.name.endsWith(".class") && !it.name.startsWith("R")) {
                        Logger.i("class path is " + "${it.absolutePath}")
                        String className = it.path.replace(path + File.separator, "")
                                .replace(File.separator,".")
                                .replace(".class", "")
                        Logger.i("class name is " + className)
                        CtClass ctClass = pool.get(className)
                        handleCtClass(ctClass, path)
                    }
                }
            }
        }
    }


    void handleCtClass(CtClass ctClass, String path) {
        Logger.i("handle ct class --> " + ctClass.name)
        //printSuper(ctClass)
        if(ctClass.isFrozen()) {
            Logger.i("ct class is frozen --> " + ctClass.name)
            ctClass.defrost()
        }
        if(ctClass.getName().contains("Activity")) {
            injectMethods.each {
                String method ->
                    CtMethod ctMethod = ctClass.getDeclaredMethod(method)
                    if(ctMethod != null) {
                        String insertValue = "com.hanami.plugin.LogUtils.log(\"" + ctClass.name + "." + ctMethod.name + " invoke" + "\");"
                        Logger.i("insert value is " + insertValue)
                        ctMethod.insertAfter(insertValue)
                    } else {
                        Logger.i("ct method is null")
                    }
            }
            ctClass.writeFile(path)
        } else {
            Logger.i("class is not Activity")
        }
    }

    void printSuper(CtClass ctClass) {
        if (ctClass.getSuperclass() != null) {
            Logger.i("super class is " + ctClass.getSuperclass().name)
            printSuper(ctClass.getSuperclass())
        }
    }


}
