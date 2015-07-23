package de.dfki.mary.coefficientextraction.extraction;

import java.util.Hashtable;

/**
 * Interface to provide an coefficient extraction method
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">slemaguer</a>
 */
public interface ExtractInterface
{
        public void setDirectories(Hashtable<String, String> extToDir);
        public void extract(String input_file_name) throws Exception;
}
