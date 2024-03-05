package cn.donting.web.os.gradle.plugin;

import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import java.io.File;

public class WapExtension {
    private final ListProperty<String> runArgs;
    private final Property<File> osCoreDir;
    private final Property<String> osCoreVersion;
    private final Property<String> osCoreDownloadURL;
    private final Property<String> pageNotFoundForwardHost;
    private final Property<Boolean> pageNotFoundForwardWapId;

    public WapExtension(Project project) {
        this.runArgs = project.getObjects().listProperty(String.class);
        this.osCoreDir = project.getObjects().property(File.class);
        this.osCoreVersion = project.getObjects().property(String.class);
        this.osCoreDownloadURL = project.getObjects().property(String.class);
        this.pageNotFoundForwardHost = project.getObjects().property(String.class);
        this.pageNotFoundForwardWapId = project.getObjects().property(Boolean.class);
    }

    public ListProperty<String> getRunArgs() {
        return runArgs;
    }

    public Property<File> getOsCoreDir() {
        return osCoreDir;
    }

    public Property<String> getOsCoreVersion() {
        return osCoreVersion;
    }

    public Property<String> getOsCoreDownloadURL() {
        return osCoreDownloadURL;
    }


    public Property<String> getPageNotFoundForwardHost() {
        return pageNotFoundForwardHost;
    }

    public Property<Boolean> getPageNotFoundForwardWapId() {
        return pageNotFoundForwardWapId;
    }
}
