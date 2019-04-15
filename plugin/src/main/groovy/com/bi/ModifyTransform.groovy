package com.bi

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.bi.util.Util
import javassist.ClassPool
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

import javax.xml.crypto.dsig.TransformException

class ModifyTransform extends Transform {

    def pool = ClassPool.default
    def project

    ModifyTransform(Project project) {
        this.project = project
    }

    @Override
    String getName() {
        return "ModifyTransform"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)

        project.android.bootClasspath.each {
            pool.appendClassPath(it.absolutePath)
        }

        transformInvocation.inputs.each {

            it.jarInputs.each {
                pool.insertClassPath(it.file.absolutePath)

                // 重命名输出文件（同目录copyFile会冲突）
                def jarName = it.name
                def md5Name = DigestUtils.md5Hex(it.file.getAbsolutePath())
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                }
                def dest = transformInvocation.outputProvider.getContentLocation(
                        jarName + md5Name, it.contentTypes, it.scopes, Format.JAR)
                FileUtils.copyFile(it.file, dest)
            }


            it.directoryInputs.each {
                def preFileName = it.file.absolutePath
                pool.insertClassPath(preFileName)
                Util.findTarget(it.file, preFileName, pool)

                // 获取output目录
                def dest = transformInvocation.outputProvider.getContentLocation(
                        it.name,
                        it.contentTypes,
                        it.scopes,
                        Format.DIRECTORY)

                println "copy directory: " + it.file.absolutePath
                println "dest directory: " + dest.absolutePath
                // 将input的目录复制到output指定目录
                FileUtils.copyDirectory(it.file, dest)
            }
        }
    }

}