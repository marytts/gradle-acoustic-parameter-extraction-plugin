package de.dfki.mary.coefficientextraction.extraction;

import java.io.File;
import java.util.Hashtable;

/**
 * Interface to provide an coefficient extraction method
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">slemaguer</a>
 */
public interface ExtractInterface
{
    public void setOutputFiles(Hashtable<String, File> extToFile);
    public void extract(File input_file) throws Exception;
}
