package com.hanami.plugin2.tool

/**
 *
 * @author lidaisheng
 * @date 2021-05-08
 */
class FileUtil {

    static String read(String filePath) {
        BufferedReader br = null
        String line = null
        StringBuffer buf = new StringBuffer()

        try {
            // 根据文件路径创建缓冲输入流
            br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"))

            // 循环读取文件的每一行, 对需要修改的行进行修改, 放入缓冲对象中
            while ((line = br.readLine()) != null) {
                buf.append(line)
                buf.append(System.getProperty("line.separator"))
            }
        } catch (Exception e) {
            e.printStackTrace()
        } finally {
            // 关闭流
            if (br != null) {
                try {
                    br.close()
                } catch (IOException e) {
                    e.printStackTrace()
                    br = null
                }
            }
        }

        return buf.toString()
    }

}
