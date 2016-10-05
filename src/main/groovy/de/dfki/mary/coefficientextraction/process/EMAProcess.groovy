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
import static groovyx.gpars.GParsPool.withPool

import de.dfki.mary.coefficientextraction.DataFileFinder
import de.dfki.mary.coefficientextraction.extraction.*

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.xml.*

class EMAProcess implements ProcessInterface
{
    // FIXME: where filename is defined !
    public void addTasks(Project project)
    {
        project.task('extractEMA') {
            inputs.files project.input_file
            outputs.files "$project.buildDir/ema/" + project.basename + ".ema"

            doLast {
                (new File("$project.buildDir/ema")).mkdirs()

                def extractor = new ExtractEMA()

                def extToDir = new Hashtable<String, String>()
                extToDir.put("ema".toString(), "$project.buildDir/ema".toString())
                extractor.setDirectories(extToDir)

                extractor.extract(project.input_file)
            }
        }

        /**
         * CMP generation task
         */
        project.task('generateCMP') {
            (new File("$project.buildDir/cmp")).mkdirs()
            outputs.files "$project.buildDir/cmp" + project.basename + ".cmp"

            def extToDir = new Hashtable<String, String>()
            extToDir.put("cmp".toString(), "$project.buildDir/cmp".toString())

            project.user_configuration.models.cmp.streams.each { stream ->
                dependsOn.add("extract" + stream.kind.toUpperCase())
                extToDir.put(stream.kind.toLowerCase().toString(),
                             ("$project.buildDir/" + stream.kind.toLowerCase()).toString())
            }

            doLast {

                def extractor = new ExtractCMP(System.getProperty("configuration"))
                extractor.setDirectories(extToDir)
                extractor.extract("$project.basename")
            }

        }
    }
}
