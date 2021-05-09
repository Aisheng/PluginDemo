package com.hanami.plugin2

import com.google.gson.Gson
import com.hanami.plugin2.tool.FileUtil

/**
 *
 * @author lidaisheng
 * @date 2021-05-08
 */
class TransformConfig {

    String configPath

    Map<String, String> replaceName;

    static TransformConfig parse(String configPath) {
        String jsonContent = FileUtil.read(configPath)
        Gson gson = new Gson()
        return gson.fromJson(jsonContent, TransformConfig.class)
    }

}
