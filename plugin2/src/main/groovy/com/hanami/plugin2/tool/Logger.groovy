package com.hanami.plugin2.tool

/**
 *
 * @author lidaisheng
 * @date 2021-05-08
 */
class Logger {

    private static final String TAG = "GRADLE_TAG"

    static void i(String msg){
        println TAG + " info --> " + msg
    }

    static void e(String errorMsg) {
        println TAG + " error --> " + errorMsg
    }

}
