package cn.donting.web.os.gradle.plugin.task;

import cn.donting.web.os.gradle.plugin.WapExtension;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.JavaExec;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WapRunTask extends JavaExec {

    private String jarName = "{name}-{osCoreVersion}.jar";
    private String osLaunchName="os-launch";
    private String osCoreName="os-core";
    private String osWapLaunchName="os-wap-launch";
    private String osCoreVersion;

    private File coreDir;
    private WapExtension pe;

    @Override
    public void exec() {
        pe = getProject().getExtensions().findByType(WapExtension.class);
        osCoreVersion = pe.getOsCoreVersion().getOrElse("0.0.11-SNAPSHOT");


        File projectDir = getProject().getProjectDir();
        coreDir = pe.getOsCoreDir().getOrElse(projectDir);
        System.out.println("coreDir:" + coreDir.getPath());

        checkOsCoreSupport();

        List<String> args = null;
        try {
            args = creatArgs();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        ListProperty<String> stringListProperty = pe.getRunArgs();
        List<String> runArgs = stringListProperty.getOrElse(new ArrayList<>());
        args.addAll(runArgs);
        System.out.println("os-core 启动参数：" + Arrays.toString(runArgs.toArray()));
        setArgs(args);

        String osLaunchName= jarName.replaceAll("\\{name}", this.osLaunchName).replaceAll("\\{osCoreVersion}", osCoreVersion);

        File file = new File(coreDir, osLaunchName);
        classpath(file.toURI().getPath());
        super.exec();

        throw new RuntimeException("未找到 os-launch-*.jar：");
    }


    private void checkOsCoreSupport() {
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

    public List<String> creatArgs() throws Exception {
        ArrayList<String> args = new ArrayList<>();
        args.add("--spring.profiles.active=dev");
        args.add("--spring.output.ansi.enabled=always");
        if(pe.getPageNotFoundForwardHost().getOrNull()!=null){
            args.add("--cn.donting.web.os.dev.pageNotFoundForwardHost="+pe.getPageNotFoundForwardHost().getOrNull());
        }
        if(pe.getPageNotFoundForwardWapId().getOrNull()!=null){
            args.add("--cn.donting.web.os.dev.pageNotFoundForwardWapId="+pe.getPageNotFoundForwardWapId().getOrNull());
        }
        return args;
    }


    private void downloadOsCoreSupport(WapExtension pe) {
        String osCoreDownloadUrl = pe.getOsCoreDownloadURL().getOrElse("http://159.75.132.132:8080/api/public/dl/HxEergKR/{osCoreVersion}/{name}-{osCoreVersion}.jar?");

        String osCoreJarUrl = osCoreDownloadUrl.replaceAll("\\{osCoreVersion}", osCoreVersion).replaceAll("\\{name}", "os-core");
        String osCoreJarName = jarName.replaceAll("\\{osCoreVersion}", osCoreVersion).replaceAll("\\{name}", "os-core");

        String osWapLaunchUrl = osCoreDownloadUrl.replaceAll("\\{osCoreVersion}", osCoreVersion).replaceAll("\\{name}", "os-launch");
        String osWapLaunchName = jarName.replaceAll("\\{osCoreVersion}", osCoreVersion).replaceAll("\\{name}", "os-launch");

        String osLaunchJarUrl = osCoreDownloadUrl.replaceAll("\\{osCoreVersion}", osCoreVersion).replaceAll("\\{name}", "os-wap-launch");
        String osLaunchJarName = jarName.replaceAll("\\{osCoreVersion}", osCoreVersion).replaceAll("\\{name}", "os-wap-launch");
        try {
            downloadFileWithProgress(osCoreJarUrl, new File(coreDir, osCoreJarName));
            downloadFileWithProgress(osWapLaunchUrl, new File(coreDir, osWapLaunchName));
            downloadFileWithProgress(osLaunchJarUrl, new File(coreDir, osLaunchJarName));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public static void downloadFileWithProgress(String fileUrl, File targetFileName) throws IOException {
        if (targetFileName.exists()) {
            return;
        }
        System.out.println("download：" + fileUrl);
        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
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
