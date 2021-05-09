package com.hanami.plugin2

import com.hanami.plugin2.tool.Logger
import javassist.ClassPool
import javassist.CtClass

/**
 *
 * @author lidaisheng
 * @date 2021-05-08
 */

class ClassHandler {

    private static final String[] injectMethods = ["onRequestPermissionsResult", "onResume", "onPause"]

    private static ClassHandler instance

    private TransformConfig config

    private ClassHandler() {
        mPathSet = new HashSet<>()

    }

    public static ClassHandler getInstance() {
        if(instance == null) {
            instance = new ClassHandler()
        }
        return instance
    }

    void initConfig(TransformConfig config) {
        this.config = config
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
                        //handleCtClass(ctClass, path)
                        if(!ctClass.name.contains("\$")) {
                            handleClass(ctClass)
                        }
                    }
                }
            }
        }
    }


    /**
     * 替换类名的过程
     * 1、如果是本类，需要替换类名，然后找其是不是有内部类，如果有的话，要处理其内部类，而且要处理本类中对内部内的引用
     * 2、如果是其它类，需要判断是不是有对该类的引用，如果有的话，需要将引用替换成新的类名
     * 3、在遍历所有的class文件的时候， 不处理带有$符号的文件，因为是内部类才有这个，内部类的处理，在处理本类的时候一并处理掉
     */
    void handleClass(CtClass ctClass, String path) {
        if(ctClass.isFrozen()) {
            ctClass.defrost()
        }
        Map<String, String> innerClassNameMap = new HashMap<>()
        for(String key : config.replaceName.keySet()) {
            if(ctClass.getRefClasses() != null && ctClass.getRefClasses().contains(key)) {
                if(ctClass.name == key) {
                    CtClass[] renameInnerClasses = ctClass.getNestedClasses()
                    if(renameInnerClasses != null) {
                        for(int i = 0; i < renameInnerClasses.length; i++) {
                            String innerName = renameInnerClasses[i].name
                            String newInnerName = innerName.replace(key, config.replaceName.get(key))
                            innerClassNameMap.put(innerName, newInnerName)
                            renameInnerClasses[i].replaceClassName(innerName, newInnerName)
                            ctClass.replaceClassName(innerName, newInnerName)
                        }
                    }
                }
                ctClass.replaceClassName(key, config.replaceName.get(key))
            }
        }
        config.replaceName.putAll(innerClassNameMap)
        //处理内部类
        CtClass[] ctClasses = ctClass.getNestedClasses()
        for(int i = 0; i < ctClasses.size(); i++) {
            handleClass(ctClasses[i], path)
        }
        if(ctClass.isModified()) {
            ctClass.writeFile(path)
        } else {
            ctClass.detach()
        }
    }

//    void handleCtClass(CtClass ctClass, String path) {
//        Logger.i("handle ct class --> " + ctClass.name)
//        if(ctClass.isFrozen()) {
//            Logger.i("ct class is frozen --> " + ctClass.name)
//            ctClass.defrost()
//        }
//        if(ctClass.getName().contains("Activity")) {
//            injectMethods.each {
//                String method ->
//                    CtMethod ctMethod = ctClass.getDeclaredMethod(method)
//                    if(ctMethod != null) {
//                        String insertValue = "com.hanami.plugin.LogUtils.log(\"" + ctClass.name + "." + ctMethod.name + " invoke" + "\");"
//                        Logger.i("insert value is " + insertValue)
//                        ctMethod.insertBefore(insertValue)
//                    } else {
//                        Logger.i("ct method is null")
//                    }
//            }
//            ctClass.writeFile(path)
//            ctClass.detach()
//        } else {
//            Logger.i("class is not Activity")
//        }
//    }


}
