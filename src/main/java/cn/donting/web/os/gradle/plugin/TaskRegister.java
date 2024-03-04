package cn.donting.web.os.gradle.plugin;
import cn.donting.web.os.gradle.plugin.task.WapRunTask;
import cn.donting.web.os.gradle.plugin.task.WevClasspathTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class TaskRegister implements Plugin<Project> {
    private Project project;

    @Override
    public void apply(Project target) {
        this.project=target;
        registerWapExtension();
        WevClasspathTask wevClasspath = project.getTasks().create("wevClasspath", WevClasspathTask.class, (plugin) -> {

        });
        wevClasspath.setGroup("web-os");
        wevClasspath.dependsOn("classes");
        WapRunTask pluginRun = project.getTasks().create("wapRun", WapRunTask.class, (plugin) -> {

        });
        pluginRun.setGroup("web-os");
        pluginRun.dependsOn("wevClasspath");
    }

    private void registerWapExtension() {
        project.getExtensions().create("wapRun", WapExtension.class, project);
    }

}
