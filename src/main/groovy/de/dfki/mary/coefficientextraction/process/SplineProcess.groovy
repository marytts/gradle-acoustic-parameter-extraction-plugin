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

class SplineProcess implements ProcessInterface
{
    // FIXME: where filename is defined !
    public void addTasks(Project project)
    {
        /**
         * Extracting straight classical coefficients (AP + SP + F0) and support of log f0
         *
         */
        project.task('extractSTRAIGHT') {
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
                    if (stream.kind ==  "spline") {
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

        project.task('extractLF0') {
            inputs.files "$project.buildDir/f0/" + project.basename + ".f0"
            outputs.files "$project.buildDir/lf0/" + project.basename + ".lf0"

            if (!(new File("$project.buildDir/lf0/" + project.basename + ".lf0")).exists()) {
                dependsOn.add("extractSTRAIGHT")
            }

            doLast {
                (new File("$project.buildDir/lf0")).mkdirs()
                def extractor = new ExtractLF0()

                project.configurationExtraction.user_configuration.models.cmp.streams.each { stream ->
                    if (stream.kind ==  "spline") {
                        extractor = new ExtractLF0(true, stream.parameters.lower_f0)
                    }
                }

                def extToDir = new Hashtable<String, String>()
                extToDir.put("lf0".toString(), "$project.buildDir/lf0".toString())
                extractor.setDirectories(extToDir)
                extractor.extract("$project.buildDir/f0/" + project.basename + ".f0")

            }
        }

        project.task('extractSPLINE', dependsOn:'extractLF0') {

            def lf0_file = new File("$project.buildDir/lf0/" + project.basename + ".lf0")
            inputs.files lf0_file

            def spline_file = new File("$project.buildDir/spline/" + project.basename + ".spline")

            outputs.files spline_file


            doLast {

                // 2. get spline file
                (new File("$project.buildDir/spline")).mkdirs()
                def extractor = new ExtractSpline(project.configurationExtraction.user_configuration.signal.frameshift * 10000, 3) // FIXME: take configuration into account !

                /*
                project.configurationExtraction.user_configuration.models.cmp.streams.each { stream ->
                    if (stream.kind ==  "spline") {
                        extractor = new ExtractSpline()
                    }
                }
                */

                def extToDir = new Hashtable<String, String>()
                extToDir.put("spline".toString(), "$project.buildDir/spline".toString())
                extToDir.put("projdir".toString(), project.getRootProject().projectDir)
                extToDir.put("lab".toString(), project.configurationExtraction.user_configuration.data.mono_lab_dir.toString())
                extractor.setDirectories(extToDir)
                extractor.extract(lf0_file.getPath())

            }
        }

        /**
         * extraction generic task
         */
        project.task('extract') {
            dependsOn.add("extractSPLINE")
        }
    }
}
