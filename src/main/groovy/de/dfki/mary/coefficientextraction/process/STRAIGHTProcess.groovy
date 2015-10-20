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

class STRAIGHTProcess implements ProcessInterface
{
    // FIXME: where filename is defined !
    public void addTasks(Project project)
    {
        project.task('extractSTRAIGHT') {
            inputs.files project.input_file
            outputs.files "$project.buildDir/f0/" + project.basename + ".f0", "$project.buildDir/ap/" + project.basename + ".ap", "$project.buildDir/sp/" + project.basename + ".sp"
            doLast {
                (new File("$project.buildDir/ap")).mkdirs()
                (new File("$project.buildDir/sp")).mkdirs()
                (new File("$project.buildDir/f0")).mkdirs()
                (new File("$project.buildDir/lf0")).mkdirs()
        
                def extractor = new ExtractSTRAIGHT(project.user_configuration.path.straight)

                // **
                project.user_configuration.models.cmp.streams.each { stream ->
                    if (stream.kind ==  "lf0") {
                        if (stream.parameters.lower_f0){
                            extractor.setMinimumF0(stream.parameters.lower_f0)
                        }
                        if (stream.parameters.upper_f0){
                            extractor.setMaximumF0(stream.parameters.upper_f0)
                        }
                    }
                }
                extractor.setFrameshift(project.user_configuration.signal.frameshift)
                extractor.setSampleRate(project.user_configuration.signal.samplerate)
                                
                // **
        
                def extToDir = new Hashtable<String, String>()
                extToDir.put("ap".toString(), "$project.buildDir/ap".toString())
                extToDir.put("sp".toString(), "$project.buildDir/sp".toString())
                extToDir.put("f0".toString(), "$project.buildDir/f0".toString())
                extToDir.put("lf0".toString(), "$project.buildDir/lf0".toString())
                extractor.setDirectories(extToDir)

                extractor.extract(project.input_file)
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
                project.user_configuration.models.cmp.streams.each { stream ->
                    if (stream.kind ==  "bap") {
                        if (stream.order){
                            extractor.setOrder(stream.order.shortValue())
                        }
                    }
                }
                extractor.setSampleRate(project.user_configuration.signal.samplerate)
                             
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
                project.user_configuration.models.cmp.streams.each { stream ->
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
                extractor.setSampleRate(project.user_configuration.signal.samplerate)
                                
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
                
                project.user_configuration.models.cmp.streams.each { stream ->
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
                
                extractor.setWindowScriptPath(project.getParent().getProjectDir().toString() + "/utils/window.pl")
                extractor.setAddHTKHeaderScriptPath(project.getParent().getProjectDir().toString() + "/utils/addhtkheader.pl")
                extractor.setDirectories(extToDir)
                
                extractor.extract("$project.basename")
            }
            
        }
    }
}

