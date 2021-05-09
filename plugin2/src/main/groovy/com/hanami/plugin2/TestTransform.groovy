package com.hanami.plugin2

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import com.hanami.plugin2.tool.Logger
import org.gradle.api.Project

/**
 *
 * @author lidaisheng
 * @date 2021-05-08
 */

class TestTransform extends Transform {


    Project mProject

    TestTransform(Project project) {
        mProject = project
    }

    /**
     * transform的名称
     * transformClassesWithMyClassTransformForDebug 运行时的名字
     * transformClassesWith + getName() + For + Debug或Release
     *
     * @return String
     */
    @Override
    String getName() {
        return "TestTransform"
    }

    /**
     * 需要处理的数据类型，有两种枚举类型
     * CLASSES和RESOURCES，CLASSES代表处理的java的class文件，RESOURCES代表要处理java的资源
     *
     * @return
     */
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }


    /**
     * 指Transform要操作内容的范围，官方文档Scope有7种类型：
     * EXTERNAL_LIBRARIES        只有外部库
     * PROJECT                   只有项目内容
     * PROJECT_LOCAL_DEPS        只有项目的本地依赖(本地jar)
     * PROVIDED_ONLY             只提供本地或远程依赖项
     * SUB_PROJECTS              只有子项目。
     * SUB_PROJECTS_LOCAL_DEPS   只有子项目的本地依赖项(本地jar)。
     * TESTED_CODE               由当前变量(包括依赖项)测试的代码
     *
     * Returns the scope(s) of the Transform. This indicates which scopes the transform consumes.
     */
    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    /**
     * 指明当前Transform是否支持增量编译
     * If it does, then the TransformInput may contain a list of changed/removed/added files, unless
     * something else triggers a non incremental run.
     */
    @Override
    boolean isIncremental() {
        return false
    }


    /**
     * Transform中的核心方法
     * transformInvocation.getInputs() 中是传过来的输入流，其中有两种格式，一种是jar包格式一种是目录格式。
     * transformInvocation.getOutputProvider() 获取到输出目录，最后将修改的文件复制到输出目录，这一步必须做不然编译会报错
     *
     * @param transformInvocation
     * @throws TransformException
     * @throws InterruptedException
     * @throws IOException
     */
    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
        Logger.i("transform start invoke")

        Collection inputs = transformInvocation.getInputs()
        TransformOutputProvider outputProvider = transformInvocation.outputProvider

        inputs.each {
            TransformInput input ->
                input.directoryInputs.each {
                    Logger.i("input dirs " + it.file.absolutePath)
                    ClassHandler.getInstance().appendClassPath(it.file.absolutePath)
                }

                ClassHandler.getInstance().handle()

                input.jarInputs.each {
                  //  Logger.i("jar files " + it.file.absolutePath)
                }

                input.directoryInputs.each {
                    DirectoryInput directoryInput ->
                        def dest = outputProvider.getContentLocation(directoryInput.name,
                                directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                        FileUtils.copyDirectory(directoryInput.file, dest)
                }

                //jar包
                input.jarInputs.each {
                    JarInput jarInput ->
//                        def jarName = jarInput.name
//                        def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
//                        if (jarName.endsWith(".jar")) {
//                            jarName = jarName.substring(0, jarName.length() - 4)
//                        }
                        def dest = outputProvider.getContentLocation(jarInput.name,
                                jarInput.contentTypes, jarInput.scopes, Format.JAR)

                        FileUtils.copyFile(jarInput.file, dest)
                }
        }
    }
}
