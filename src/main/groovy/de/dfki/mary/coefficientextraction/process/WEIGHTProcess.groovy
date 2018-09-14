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
import de.dfki.mary.coefficientextraction.process.task.ExtractWeightTask;
import de.dfki.mary.coefficientextraction.process.task.ExtractMGCTask;
import de.dfki.mary.coefficientextraction.process.task.ExtractLF0Task;
import de.dfki.mary.coefficientextraction.process.task.ExtractBAPTask;


/**
 *  Class which defines the process to extract the coefficients using the weight vocoder
 */
class WeightProcess implements ProcessInterface
{

    /**
     *  Method to add the task to the given project.
     *
     *
     *  @param project the project which needs the coefficient extraction
     */
    @Override
    public void addTasks(Project project)
    {
        /**
         *  The first task to add is the vocoder parameter from the wav using weight.
         *
         *  This task will generate the spectrum (.sp), the f0 (.f0) and the aperiodicity (.ap).
         */
        project.task('extractWeight', type: ExtractWeightTask) {
            dependsOn.add("configurationExtraction")

            // Define directories
            weight_js_dir = project.configuration.weight_dir
            weight_dir = new File("$project.buildDir/weight/")

            // Define list_basenames
            list_basenames = project.configuration.list_basenames
        }


        /**
         *  Generic extraction task which is the entry point of the process
         *
         */
        project.task('extract') {
            description "Entry task for tongue motion weights extraction"
            dependsOn.add("extractWeight")
        }
    }
}
