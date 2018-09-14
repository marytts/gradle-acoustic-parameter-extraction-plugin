package de.dfki.mary.coefficientextraction.process

import org.gradle.api.Project

/**
 *  Interface to define an extraction process
 *
 *  @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public interface ProcessInterface
{
    /**
     *  Method to add the task to the given project
     *
     *  @param project the project which needs the coefficient extraction
     */
    public void addTasks(Project project);
}
