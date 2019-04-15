package com.bi.util

import com.android.SdkConstants
import javassist.ClassPool
import javassist.CtClass
import javassist.CtField
import javassist.CtMethod
import org.gradle.api.Project

class Util {

    private static final def CLICK_LISTENER = "android.view.View\$OnClickListener"

    static void fillPoll(Project project, ClassPool pool){
        project.android.bootClasspath.each {
            pool.appendClassPath(it.absolutePath)
        }
    }

    static void findTarget(File dir, String fileName, ClassPool pool) {
        if (dir.isDirectory()) {
            dir.listFiles().each {
                findTarget(it, fileName, pool)
            }
        } else {
            modify(dir, fileName, pool)
        }
    }

    private static void modify(File dir, String fileName, ClassPool pool) {
        def filePath = dir.absolutePath

        if (!filePath.endsWith(SdkConstants.DOT_CLASS)) {
            return
        }
        if (filePath.contains('R$') || filePath.contains('R.class')
                || filePath.contains("BuildConfig.class")) {
            return
        }

        def className = filePath.replace(fileName, "")
                .replace("\\", ".")
                .replace("/", ".")
        def name = className.replace(SdkConstants.DOT_CLASS, "")
                .substring(1)

        CtClass ctClass = pool.get(name)
        CtClass[] interfaces = ctClass.getInterfaces()
        if (interfaces.contains(pool.get(CLICK_LISTENER))) {
            if (name.contains("\$")) {
                println "class is inner class：" + ctClass.name
                println "CtClass: " + ctClass
                CtClass outer = pool.get(name.substring(0, name.indexOf("\$")))

                CtField field = ctClass.getFields().find {
                    return it.type == outer
                }
                if (field != null) {
                    println "fieldStr: " + field.name
                    def body = "android.widget.Toast.makeText(" + field.name + "," +
                            "\"my javassist test\", android.widget.Toast.LENGTH_SHORT).show();"
                    addCode(ctClass, body, fileName, pool)
                }
            } else {
                println "class is outer class: " + ctClass.name
                //更改onClick函数
                def body = "android.widget.Toast.makeText(\$1.getContext(), \"my javassist test\", android.widget.Toast.LENGTH_SHORT).show();"
                addCode(ctClass, body, fileName, pool)
            }
        }
    }

    private static void addCode(CtClass ctClass, String body, String fileName, ClassPool pool) {
        ctClass.defrost()
        CtMethod method = ctClass.getDeclaredMethod("onClick", pool.get("android.view.View"))
        method.insertAfter(body)

        ctClass.writeFile(fileName)
        ctClass.detach()
        println "write file: " + fileName + "\\" + ctClass.name
        println "modify method: " + method.name + " succeed"
    }
}