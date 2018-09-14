package de.dfki.mary.coefficientextraction.process

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

import static groovyx.gpars.GParsPool.runForkJoin
import static groovyx.gpars.GParsPool.withPoo

import de.dfki.mary.coefficientextraction.DataFileFinder
import de.dfki.mary.coefficientextraction.extraction.*

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.xml.*

import de.dfki.mary.coefficientextraction.process.task.ExtractEMATask;


/**
 *  Classe which defines the process to adapt EMA data from the speech tools (MNGU) format to a raw binary EMA file
 *
 */
class EMAProcess implements ProcessInterface
{
    /**
     *  Method to add the sequence of tasks needed to achieve the process
     *
     *  In this case, the sequence is composed by only the ExtractEMATask type task
     *
     *  @param project the project which is applies the process
     */
    @Override
    public void addTasks(Project project)
    {
        project.task('extract', type: ExtractEMATask) {
            // Current task depends on the configurationExtraction entry point
            dependsOn "configurationExtraction"

            // Adapt directories
            orig_ema_dir = project.configuration.ema_dir
            ema_dir = new File("${project.buildDir}/ema/")

            // Define list_basenames
            list_basenames = project.configuration.list_basenames
        }
    }
}
