package cn.donting.web.os.gradle.plugin.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class WevClasspathTask extends DefaultTask {
    @TaskAction
    public void runTask() throws Exception {
        Project project = getProject();
        try {
            generateClasspathFile(project);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void generateClasspathFile(Project project) throws Exception {
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        SourceSet main = sourceSets.getByName("main");
        FileCollection runtimeClasspath = main.getRuntimeClasspath();
        File mainClassDir = null;
        List<URL> classpathURls = new ArrayList<>();
        StringBuilder classpathFile = new StringBuilder();
        for (File file : runtimeClasspath) {
            if (mainClassDir == null) {
                mainClassDir = file;
            }
            classpathURls.add(file.toURI().toURL());
            classpathFile.append("\n").append(file.getPath());
        }
        URLClassLoader urlClassLoader = new URLClassLoader(classpathURls.toArray(new URL[classpathURls.size()]));
        Class<? extends Annotation> annotation = (Class<? extends Annotation>) urlClassLoader.loadClass("org.springframework.boot.autoconfigure.SpringBootApplication");

        List<String> classes = getClasses(mainClassDir);
        String mainClass = null;
        for (String className : classes) {
            Class<?> aClass1 = urlClassLoader.loadClass(className);
            Annotation annotation1 = aClass1.getAnnotation(annotation);
            if (annotation1 != null) {
                mainClass = className;
            }
        }
        if (mainClass != null) {
            classpathFile.insert(0, mainClass);
        } else {
            throw new RuntimeException("未找到org.springframework.boot.autoconfigure.SpringBootApplication 标识的启动类");
        }
        File rootDir = project.getProjectDir();
        File file = new File(rootDir, project.getName()+".wev");
        if(!file.exists()){
            file.createNewFile();
        }
        Files.write(file.toPath(),classpathFile.toString().getBytes());
    }


    public static List<String> getClasses(File mainDir) throws IOException {
        LinkedList<String> classpath = new LinkedList<>();
        List<String> classes = new ArrayList<>();
        Files.walkFileTree(mainDir.toPath(), new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (dir.toFile().equals(mainDir)) {
                    return FileVisitResult.CONTINUE;
                }
                if (classpath.size() == 0) {
                    classpath.add(dir.toFile().getName());
                } else {
                    String last = classpath.getLast();
                    classpath.add(last + "." + dir.toFile().getName());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toFile().getName().endsWith(".class")) {
                    String name = file.toFile().getName();
                    String replace = name.replace(".class", "");
                    String last = classpath.getLast();
                    classes.add(last + "." + replace);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                exc.printStackTrace();
                return FileVisitResult.CONTINUE;
            }


            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (classpath.size() > 0) {
                    classpath.removeLast();
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return classes;
    }


}
