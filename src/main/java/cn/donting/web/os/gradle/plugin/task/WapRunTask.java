package cn.donting.web.os.gradle.plugin.task;

import cn.donting.web.os.gradle.plugin.WapExtension;
import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WapRunTask extends JavaExec {

    private String jarName = "{name}-{osCoreVersion}.jar";
    private String osLaunchName = "web-os-launch";
    private String osCoreName = "web-os-core";
    private String osWapLaunchName = "web-os-wap-launch";
    private String osCoreVersion;

    private File coreDir;
    private WapExtension pe;

    @Override
    public void exec() {
        pe = getProject().getExtensions().findByType(WapExtension.class);
        osCoreVersion = pe.getOsCoreVersion().getOrNull();
        SourceSetContainer sourceSets = getProject().getExtensions().getByType(SourceSetContainer.class);
        SourceSet main = sourceSets.getByName("main");
        if (osCoreVersion == null) {
            FileCollection runtimeClasspath = main.getRuntimeClasspath();
            for (File file : runtimeClasspath) {
                String name = file.getName();
                if (name.startsWith("web-os-api-")) {
                    osCoreVersion = name.substring("web-os-api-".length(), name.length() - 4);
                    break;
                }
            }
            FileCollection compileClasspath = main.getCompileClasspath();
            for (File file : compileClasspath) {
                String name = file.getName();
                if (name.startsWith("web-os-api-")) {
                    osCoreVersion = name.substring("web-os-api-".length(), name.length() - 4);
                    break;
                }
            }
        }
        File singleFile = main.getResources().getSourceDirectories().getSingleFile();
        File wapInfoFile = new File(singleFile, "wap.info.json");
        System.out.println("wapInfoFile:"+wapInfoFile);
        if(!wapInfoFile.exists()){
            throw new RuntimeException("缺少依赖wap.info.json");
        }
        String json = FileUtil.readUtf8String(wapInfoFile);
        JSONObject jsonObject = JSONUtil.parseObj(json);
        String wapId = jsonObject.getStr("id");
        if(osCoreVersion==null){
            System.err.println("未依赖web-os-api，也没有指定 osCoreVersion");
            throw new RuntimeException("未依赖web-os-api，也没有指定 osCoreVersion");
        }
        File projectDir = getProject().getProjectDir();
        coreDir = pe.getOsCoreDir().getOrElse(new File(projectDir, ".web-os-dev"));
        System.out.println("coreDir:" + coreDir.getPath());

        try {
            checkOsCoreSupport();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<String> args = null;
        args = creatArgs();
        ListProperty<String> stringListProperty = pe.getRunArgs();
        List<String> runArgs = stringListProperty.getOrElse(new ArrayList<>());
        args.addAll(runArgs);
        args.add("--startMode=WapRun");
        args.add("--devWapId="+wapId);
        ArrayList<String> jvmArgs = new ArrayList<>();
        try {
            args.add("-user.dir=" + coreDir.getCanonicalPath());
        } catch (IOException e) {
            System.out.println("coreDir获取失败：" + coreDir);
            throw new RuntimeException(e);
        }
        System.out.println("os-core 启动参数：" + Arrays.toString(args.toArray()));
        getAllJvmArgs().addAll(jvmArgs);

        setArgs(args);

        String osLaunchName = jarName.replaceAll("\\{name}", this.osLaunchName).replaceAll("\\{osCoreVersion}", osCoreVersion);

        File file = new File(coreDir, osLaunchName);
        classpath(file.toURI().getPath());
        super.exec();

        throw new RuntimeException("未找到 web-os-launch-*.jar：");
    }


    private void checkOsCoreSupport() throws IOException {
        System.out.println("checkOsCoreSupport");
        if (!coreDir.exists()) {
            coreDir.mkdirs();
            downloadOsCoreSupport(pe);
            return;
        }
        File[] files = coreDir.listFiles();
        for (File file : files) {
            if (file.getName().startsWith("os-core-") && file.getName().endsWith(".jar")) {
                return;
            }
        }
        downloadOsCoreSupport(pe);
    }

    public List<String> creatArgs() {
        ArrayList<String> args = new ArrayList<>();
        args.add("--spring.profiles.active=dev");
        args.add("--spring.output.ansi.enabled=always");
        if (pe.getPageNotFoundForwardHost().getOrNull() != null) {
            args.add("--cn.donting.web.os.dev.pageNotFoundForwardHost=" + pe.getPageNotFoundForwardHost().getOrNull());
        }
        if (pe.getPageNotFoundForwardWapId().getOrNull() != null) {
            args.add("--cn.donting.web.os.dev.pageNotFoundForwardWapId=" + pe.getPageNotFoundForwardWapId().getOrNull());
        }
        return args;
    }


    private void downloadOsCoreSupport(WapExtension pe) throws IOException {
        String osCoreDownloadUrl = pe.getOsCoreDownloadURL().getOrElse("http://159.75.132.132:8080/api/public/dl/HxEergKR/{osCoreVersion}/{name}-{osCoreVersion}.jar?");

        String osCoreJarUrl = osCoreDownloadUrl.replaceAll("\\{osCoreVersion}", osCoreVersion).replaceAll("\\{name}", "web-os-core");
        String osCoreJarName = jarName.replaceAll("\\{osCoreVersion}", osCoreVersion).replaceAll("\\{name}", "web-os-core");

        String osWapLaunchUrl = osCoreDownloadUrl.replaceAll("\\{osCoreVersion}", osCoreVersion).replaceAll("\\{name}", "web-os-launch");
        String osWapLaunchName = jarName.replaceAll("\\{osCoreVersion}", osCoreVersion).replaceAll("\\{name}", "web-os-launch");

        String osLaunchJarUrl = osCoreDownloadUrl.replaceAll("\\{osCoreVersion}", osCoreVersion).replaceAll("\\{name}", "web-os-wap-launch");
        String osLaunchJarName = jarName.replaceAll("\\{osCoreVersion}", osCoreVersion).replaceAll("\\{name}", "web-os-wap-launch");
        try {
            downloadFileWithProgress(osCoreJarUrl, new File(coreDir, osCoreJarName));
            downloadFileWithProgress(osWapLaunchUrl, new File(coreDir, osWapLaunchName));
            downloadFileWithProgress(osLaunchJarUrl, new File(coreDir, osLaunchJarName));
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }

    }

    public static void downloadFileWithProgress(String fileUrl, File targetFileName) throws IOException {
        if (targetFileName.exists()) {
            return;
        }
        System.out.println("download：" + fileUrl);
        URL url = new URL(fileUrl);
        URLConnection connection = url.openConnection();
        int totalFileSize = connection.getContentLength();
        int downloadedFileSize = 0;
        long time = System.currentTimeMillis();
        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(targetFileName)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                downloadedFileSize += bytesRead;
                double progress = (double) downloadedFileSize / totalFileSize * 100;
                if (System.currentTimeMillis() - time > 2000) {
                    System.out.printf("Download progress: %.2f%%\n", progress);
                    time = System.currentTimeMillis();
                }
            }
            System.out.printf("Download progress: %.2f%%\n", 100f);
        }
        System.out.println("Download completed.");
    }
}
