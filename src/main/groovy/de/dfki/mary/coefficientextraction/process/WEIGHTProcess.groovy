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
         * CMP generation task
         */
        project.task('generateCMP') {
            (new File("$project.buildDir/cmp")).mkdirs()
            outputs.files "$project.buildDir/cmp" + project.basename + ".cmp"

            def extToDir = new Hashtable<String, String>()
            extToDir.put("cmp".toString(), "$project.buildDir/cmp".toString())

            ["WEIGHT"].each  { kind ->
                dependsOn.add("extract" + kind.toUpperCase())
                extToDir.put(kind.toLowerCase().toString(),
                             (("$project.buildDir/" + kind.toLowerCase()).toString()))
            }

            doLast {

                def extractor = new ExtractCMP(System.getProperty("configuration"))
                extractor.setDirectories(extToDir)
                extractor.extract("$project.basename")
            }

        }
    }
}
