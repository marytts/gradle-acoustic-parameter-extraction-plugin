package de.dfki.mary.coefficientextraction

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project

import de.dfki.mary.coefficientextraction.export.*
import de.dfki.mary.coefficientextraction.process.*

class CoefficientExtractionPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        // project.plugins.apply JavaPlugin
        // project.plugins.apply MavenPlugin

        project.sourceCompatibility = JavaVersion.VERSION_1_8

        project.ext {
            basename = project.name
        }

        project.status = project.version.endsWith('SNAPSHOT') ? 'integration' : 'release'

        project.afterEvaluate {
            project.task("configurationExtraction") {
                dependsOn "configuration"

                ext.nb_proc = project.configuration.hasProperty("nb_proc") ? project.configuration.nb_proc : 1
                ext.user_configuration = project.configuration.hasProperty("user_configuration") ? project.configuration.user_configuration : null
            }


            def kinds = [
            "ema":          new EMAProcess(),
            "straight":     new STRAIGHTProcess(),
            "straightdnn" : new STRAIGHTDNNProcess(),
            "spline":       new SplineProcess(),
            "world":        new WorldProcess(),
            "straightema":  new STRAIGHTEMAProcess(),
            "straightemadnn":  new STRAIGHTEMADNNProcess(),
            "weight":       new WeightProcess()
            ];

            if (project.configurationExtraction.user_configuration != null)
            {
                kinds[project.configurationExtraction.user_configuration.settings.extraction.kind].addTasks(project)
            }
        }
    }
}
