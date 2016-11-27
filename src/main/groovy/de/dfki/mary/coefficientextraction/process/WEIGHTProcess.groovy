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

class WEIGHTProcess implements ProcessInterface
{
    // FIXME: where filename is defined !
    public void addTasks(Project project)
    {
        project.task('extractWEIGHT') {
            def input_file = ""
            project.user_configuration.models.cmp.streams.each { stream ->
                if (stream.kind == "weight") {
                    input_file = (new File(DataFileFinder.getFilePath(stream.coeffDir))).toString() + "/" + project.basename + "." + stream.kind
                }
            }

            inputs.files input_file
            outputs.files "$project.buildDir/weight/" + project.basename + ".weight"

            doLast {
                (new File("$project.buildDir/weight")).mkdirs()

                def extractor = new ExtractWeight()

                def extToDir = new Hashtable<String, String>()
                extToDir.put("weight".toString(), "$project.buildDir/weight".toString())
                extractor.setDirectories(extToDir)

                extractor.extract(input_file)
            }
        }


        /**
         * extraction generic task
         */
        project.task('extract') {
            dependsOn.add("extractWEIGHT")
        }
    }
}