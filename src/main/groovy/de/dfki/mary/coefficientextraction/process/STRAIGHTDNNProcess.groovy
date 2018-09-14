package de.dfki.mary.coefficientextraction.process

/* Gradle imports */
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.MavenPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.bundling.Zip

/* Helpers import */
import de.dfki.mary.coefficientextraction.process.task.ExtractVUVTask;
import de.dfki.mary.coefficientextraction.process.task.ExtractInterpolatedF0Task;

class STRAIGHTDNNProcess extends STRAIGHTProcess
{
    // FIXME: where filename is defined !
    public void addTasks(Project project)
    {
        addGenericTasks(project);

        /**
         *  Task which extract the voice/unvoice mask from the lf0
         *
         */
        project.task('extractVUV', type:ExtractVUVTask){
            description "Task which extract the voice/unvoice mask from the lf0"
            dependsOn.add("extractLF0")

            // Define directories
            lf0_dir = project.extractLF0.lf0_dir
            vuv_dir = new File("$project.buildDir/vuv/")

            // Define list_basenames
            list_basenames = project.configuration.list_basenames
        }

        /**
         *  Task which generates an interpolated F0 from the log f0
         *
         */
        project.task('extractInterpolatedF0', type: ExtractInterpolatedF0Task) {
            description "Task which interpolate the f0"
            dependsOn.add("extractLF0")

            // Define directories
            lf0_dir = project.extractLF0.lf0_dir
            interpolated_lf0_dir = new File("$project.buildDir/interpolated_lf0/")

            // Define list_basenames
            list_basenames = project.configuration.list_basenames
        }

        /**
         * extraction generic task
         */
        project.task('extract') {
            dependsOn.add("extractMGC")
            dependsOn.add("extractLF0")
            dependsOn.add("extractVUV")
            dependsOn.add("extractInterpolatedF0")
            dependsOn.add("extractBAP")
        }
    }
}
