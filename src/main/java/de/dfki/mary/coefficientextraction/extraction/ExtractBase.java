package de.dfki.mary.coefficientextraction.extraction;

import java.io.File;
import java.util.Hashtable;

/**
 * Interface to provide an coefficient extraction method
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">slemaguer</a>
 */
public abstract class ExtractBase implements ExtractInterface
{
    protected Hashtable<String, File> extToFile;
    public void setOutputFiles(Hashtable<String, File> extToFile)
    {
        this.extToFile = extToFile;
    }
    public abstract void extract(File input_file) throws Exception;
}
