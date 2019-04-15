package com.bi;

import com.android.build.api.transform.*;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.bi.util.Util;
import javassist.ClassPool;
import javassist.NotFoundException;
import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

public class MyPluginTransform extends Transform {

    Project mProject;
    ClassPool classPool = new ClassPool(ClassPool.getDefault());

    public MyPluginTransform(Project project) {
        this.mProject = project;
    }

    @Override
    public String getName() {
        return "MyPluginTransform";
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation);
        Util.fillPoll(mProject, classPool);
        Collection<TransformInput> inputs = transformInvocation.getInputs();
        try {
            for (TransformInput transformInput : inputs) {
                Collection<DirectoryInput> directoryInputs = transformInput.getDirectoryInputs();
                for (DirectoryInput directoryInput : directoryInputs) {
                    String preFileName = directoryInput.getFile().getAbsolutePath();
                    classPool.insertClassPath(preFileName);
                    Util.findTarget(directoryInput.getFile(), preFileName, classPool);
                    // 获取output目录
                    File dest = transformInvocation.getOutputProvider().getContentLocation(directoryInput.getName(), directoryInput.getContentTypes(), directoryInput.getScopes(), Format.DIRECTORY);
                    // 将input的目录复制到output指定目录
                    FileUtils.copyDirectory(directoryInput.getFile(), dest);
                }

                Collection<JarInput> jarInputs = transformInput.getJarInputs();
                for (JarInput jarInput : jarInputs) {
                    String preFileName = jarInput.getFile().getAbsolutePath();
                    classPool.insertClassPath(preFileName);
//                    String jarName = jarInput.getName();
//                    // 重命名输出文件（同目录copyFile会冲突）
//                    if (jarName.endsWith(".jar")) {
//                        jarName = jarName.substring(0, jarName.length() - 4);
//                    }
//                    String md5Name = DigestUtils.md5Hex(preFileName);
//                    File dest = transformInvocation.getOutputProvider().getContentLocation(
//                            jarName + md5Name, jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
                    File dest = transformInvocation.getOutputProvider().getContentLocation(
                            jarInput.getName(), jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
                    FileUtils.copyFile(jarInput.getFile(), dest);
                }
            }
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
    }

}
