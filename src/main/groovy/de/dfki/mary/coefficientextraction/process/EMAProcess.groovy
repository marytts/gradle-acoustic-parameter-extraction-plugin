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
            def input_file = ""
            def channel_list = []
            project.user_configuration.models.cmp.streams.each { stream ->
                if (stream.kind == "ema") {
                    input_file = (new File(DataFileFinder.getFilePath(stream.coeffDir))).toString() + "/" + project.basename + ".ema"

                    // Check if channels are given
                    if ((stream["parameters"]) && (stream.parameters["channel_ids"])) {
                        channel_list = stream.parameters["channel_ids"]
                    }

                }
            }
            if (input_file.isEmpty()) {
                throw new Exception("no ema to extract, so why being here ?")
            }
            inputs.files input_file
            outputs.files "$project.buildDir/ema/" + project.basename + ".ema"

            doLast {
                (new File("$project.buildDir/ema")).mkdirs()

                int[] channels;
                if (channel_list)
                {
                    channels = new int[channel_list.size()]
                    channel_list.eachWithIndex{c,i ->
                        channels[i] = c.intValue()
                    }
                }
                else
                {
                    channels = [0, 8, 16, 24, 32, 64, 72];
                }
                def extractor = new ExtractEMA(channels)

                def extToDir = new Hashtable<String, String>()
                extToDir.put("ema".toString(), "$project.buildDir/ema".toString())
                extractor.setDirectories(extToDir)

                extractor.extract(input_file)
            }
        }

        /**
         * extraction generic task
         */
        project.task('extract') {
            dependsOn.add("extractEMA")
        }
    }
}