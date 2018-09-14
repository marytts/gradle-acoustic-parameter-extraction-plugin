package de.dfki.mary.coefficientextraction.extraction;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.util.Hashtable;

/**
 * Coefficients extraction based on STRAIGHT
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class ExtractVUV extends ExtractBase
{
    public ExtractVUV()
    {
    }

    public void extract(File input_file) throws Exception
    {
        // 2. extraction
        String command = "cat " + input_file.toString() + " | ";
        command += "sopr -magic -1.0E+10 -m 0.0 -a 1.0 -MAGIC 0.0 > " + extToFile.get("vuv").toString();

        String[] cmd = {"bash", "-c", command};
        Process p = Runtime.getRuntime().exec(cmd);
        p.waitFor();


        BufferedReader reader =
            new BufferedReader(new InputStreamReader(p.getInputStream()));

        String line = "";
        // while ((line = reader.readLine())!= null) {
        //         System.out.println(line);
        // }


        // Error stream => throw exception if not empty
        StringBuilder sb = new StringBuilder();
        reader =
            new BufferedReader(new InputStreamReader(p.getErrorStream()));

        line = "";
        while ((line = reader.readLine())!= null) {
            sb.append(line + "\n");
        }
        if (!sb.toString().isEmpty())
        {
            throw new Exception(sb.toString());
        }
    }
}
