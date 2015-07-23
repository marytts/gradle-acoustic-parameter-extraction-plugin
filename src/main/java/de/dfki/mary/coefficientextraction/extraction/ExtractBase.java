package de.dfki.mary.coefficientextraction.extraction;

import java.util.Hashtable;

/**
 * Interface to provide an coefficient extraction method
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">slemaguer</a>
 */
public abstract class ExtractBase
{
        protected Hashtable<String, String> extToDir;
        public void setDirectories(Hashtable<String, String> extToDir)
        {
                this.extToDir = extToDir; 
        }
        public abstract void extract(String input_file_name) throws Exception;
}
