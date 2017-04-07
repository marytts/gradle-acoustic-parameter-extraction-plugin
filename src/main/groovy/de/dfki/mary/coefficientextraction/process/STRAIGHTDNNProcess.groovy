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

class STRAIGHTDNNProcess implements ProcessInterface
{
    // FIXME: where filename is defined !
    public void addTasks(Project project)
    {
        project.task('extractSTRAIGHT') {
            dependsOn.add("configurationExtraction")
            inputs.files project.configurationExtraction.input_file
            outputs.files "$project.buildDir/f0/" + project.basename + ".f0", "$project.buildDir/ap/" + project.basename + ".ap", "$project.buildDir/sp/" + project.basename + ".sp"

            doLast {
                (new File("$project.buildDir/ap")).mkdirs()
                (new File("$project.buildDir/sp")).mkdirs()
                (new File("$project.buildDir/f0")).mkdirs()
                (new File("$project.buildDir/lf0")).mkdirs()

                def extractor = new ExtractSTRAIGHT(project.configurationExtraction.user_configuration.path.straight)

                // **
                project.configurationExtraction.user_configuration.models.cmp.streams.each { stream ->
                    if (stream.kind ==  "lf0") {
                        if (stream.parameters.lower_f0){
                            extractor.setMinimumF0(stream.parameters.lower_f0)
                        }
                        if (stream.parameters.upper_f0){
                            extractor.setMaximumF0(stream.parameters.upper_f0)
                        }
                    }
                }
                extractor.setFrameshift(project.configurationExtraction.user_configuration.signal.frameshift)
                extractor.setSampleRate(project.configurationExtraction.user_configuration.signal.samplerate)

                // **

                def extToDir = new Hashtable<String, String>()
                extToDir.put("ap".toString(), "$project.buildDir/ap".toString())
                extToDir.put("sp".toString(), "$project.buildDir/sp".toString())
                extToDir.put("f0".toString(), "$project.buildDir/f0".toString())
                extToDir.put("lf0".toString(), "$project.buildDir/lf0".toString())
                extractor.setDirectories(extToDir)

                extractor.extract(project.configurationExtraction.input_file)
            }
        }

        /**
         * Extract aperiodicty per band based on the aperiodicity extracted by STRAIGHT
         *
         */
        project.task('extractBAP') {
            inputs.files "$project.buildDir/ap/" + project.basename + ".ap"
            outputs.files "$project.buildDir/bap/" + project.basename + ".bap"
            if (!(new File("$project.buildDir/bap/" + project.basename + ".bap")).exists()) {
                dependsOn.add("extractSTRAIGHT")
            }

            doLast {
                (new File("$project.buildDir/bap")).mkdirs()

                def extractor = new ExtractBAP()

                // **
                project.configurationExtraction.user_configuration.models.cmp.streams.each { stream ->
                    if (stream.kind ==  "bap") {
                        if (stream.order){
                            extractor.setOrder(stream.order.shortValue())
                        }
                    }
                }
                extractor.setSampleRate(project.configurationExtraction.user_configuration.signal.samplerate)

                def extToDir = new Hashtable<String, String>()
                extToDir.put("bap".toString(), "$project.buildDir/bap".toString())
                extractor.setDirectories(extToDir)

                extractor.extract("$project.buildDir/ap/" + project.basename + ".ap")
            }
        }

        /**
         *  Extract MGC coefficients based on the spectrum extracted by STRAIGHT
         *
         */
        project.task('extractMGC') {
            inputs.files "$project.buildDir/sp/" + project.basename + ".sp"
            outputs.files "$project.buildDir/mgc/" + project.basename + ".mgc"
            if (!(new File("$project.buildDir/mgc/" + project.basename + ".mgc")).exists()) {
                dependsOn.add("extractSTRAIGHT")
            }

            doLast {
                (new File("$project.buildDir/mgc")).mkdirs()

                def extractor = new ExtractMGC()

                // **
                project.configurationExtraction.user_configuration.models.cmp.streams.each { stream ->
                    if (stream.kind ==  "mgc") {
                        if (stream.order){
                            extractor.setOrder(stream.order.shortValue())
                        }
                        if (stream.gamma){
                            extractor.setGamma(stream.parameters.gamma)
                        }
                        if (stream.use_lngain){
                            extractor.set(stream.parameters.use_lngain)
                        }
                    }
                }
                extractor.setSampleRate(project.configurationExtraction.user_configuration.signal.samplerate)

                // **
                def extToDir = new Hashtable<String, String>()
                extToDir.put("mgc".toString(), "$project.buildDir/mgc".toString())
                extractor.setDirectories(extToDir)

                extractor.extract("$project.buildDir/sp/" + project.basename + ".sp")
            }
        }

        project.task('extractLF0'){
            inputs.files "$project.buildDir/f0/" + project.basename + ".f0"
            outputs.files "$project.buildDir/lf0/" + project.basename + ".lf0"

            if (!(new File("$project.buildDir/lf0/" + project.basename + ".lf0")).exists()) {
                dependsOn.add("extractSTRAIGHT")
            }

            doLast {
                (new File("$project.buildDir/lf0")).mkdirs()
                def extractor = new ExtractLF0()

                project.configurationExtraction.user_configuration.models.cmp.streams.each { stream ->
                    if (stream.kind ==  "lf0") {
                        if (stream.parameters.interpolate) {
                            extractor = new ExtractLF0(true, stream.parameters.lower_f0)
                        }
                    }
                }

                def extToDir = new Hashtable<String, String>()
                extToDir.put("lf0".toString(), "$project.buildDir/lf0".toString())
                extractor.setDirectories(extToDir)
                extractor.extract("$project.buildDir/f0/" + project.basename + ".f0")

            }
        }


        project.task('extractVUV'){
            inputs.files "$project.buildDir/lf0/" + project.basename + ".lf0"
            outputs.files "$project.buildDir/vuv/" + project.basename + ".vuv"

            dependsOn.add("extractLF0")

            doLast {
                (new File("$project.buildDir/vuv")).mkdirs()
                def extractor = new ExtractVUV()

                def extToDir = new Hashtable<String, String>()
                extToDir.put("vuv".toString(), "$project.buildDir/vuv".toString())
                extractor.setDirectories(extToDir)
                extractor.extract("$project.buildDir/lf0/" + project.basename + ".lf0")

            }
        }

        project.task('extractInterpolatedF0'){
            inputs.files "$project.buildDir/lf0/" + project.basename + ".lf0"
            outputs.files "$project.buildDir/interpolated_lf0/" + project.basename + ".lf0"

            dependsOn.add("extractLF0")

            doLast {
                (new File("$project.buildDir/interpolated_lf0")).mkdirs()
                def extractor = new ExtractInterpolatedF0()

                def extToDir = new Hashtable<String, String>()
                extToDir.put("interpolated_lf0".toString(), "$project.buildDir/interpolated_lf0".toString())
                extractor.setDirectories(extToDir)
                extractor.extract("$project.buildDir/lf0/" + project.basename + ".lf0")

            }
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
