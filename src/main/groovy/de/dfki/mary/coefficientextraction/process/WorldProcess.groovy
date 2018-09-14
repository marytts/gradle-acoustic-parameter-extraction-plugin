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
import de.dfki.mary.coefficientextraction.process.task.ExtractWorldTask;
import de.dfki.mary.coefficientextraction.process.task.ExtractMGCTask;
import de.dfki.mary.coefficientextraction.process.task.ExtractLF0Task;
import de.dfki.mary.coefficientextraction.process.task.ExtractBAPTask;

/**
 *  Class which defines the process to extract the coefficients using the world vocoder
 */
class WorldProcess implements ProcessInterface
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
         *  The first task to add is the vocoder parameter from the wav using world.
         *
         *  This task will generate the spectrum (.sp), the f0 (.f0) and the aperiodicity (.ap).
         */
        project.task('extractWorld', type: ExtractWorldTask) {
            dependsOn.add("configurationExtraction")

            // Define directories
            wav_dir = project.configuration.wav_dir
            sp_dir = new File("$project.buildDir/sp/")
            f0_dir = new File("$project.buildDir/f0/")
            ap_dir = new File("$project.buildDir/ap/")

            // Define list_basenames
            list_basenames = project.configuration.list_basenames
        }

        /**
         *  This task generate the bap file from the ap file.
         *
         */
        project.task('extractBAP', type: ExtractBAPTask) {
            description "Task which converts ap to bap file"

            // Define directories
            ap_dir = project.extractWorld.ap_dir
            bap_dir = new File("$project.buildDir/bap/")

            // Define list_basenames
            list_basenames = project.configuration.list_basenames
        }


        /**
         *  This task generate the mgc file from the sp file.
         *
         */
        project.task('extractMGC', type: ExtractMGCTask) {
            description "Task which converts sp to mgc file"

            // Define directories
            sp_dir = project.extractWorld.sp_dir
            mgc_dir = new File("$project.buildDir/mgc/")

            // Define list_basenames
            list_basenames = project.configuration.list_basenames
        }

        /**
         *  This task generate the log f0 file from the f0 file.
         *
         */
        project.task('extractLF0', type: ExtractLF0Task) {
            description "Task which converts f0 to lf0 file"

            // Define directories
            f0_dir = project.extractWorld.f0_dir
            lf0_dir = new File("$project.buildDir/lf0/")

            // Define list_basenames
            list_basenames = project.configuration.list_basenames
        }


        /**
         *  Generic extraction task which is the entry point of the process
         *
         */
        project.task('extract') {
            description "Entry task for World based acoustic coefficient extraction"
            dependsOn.add("extractMGC")
            dependsOn.add("extractLF0")
            dependsOn.add("extractBAP")
        }
    }
}
