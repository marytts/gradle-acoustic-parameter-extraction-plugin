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

    private void generateLogF0(String input_file_name, String output_file_name)
        throws Exception
    {

        // 2. extraction
        String command = "cat " + input_file_name + " | ";
        command += "sopr -magic -1.0E+10 -m 0.0 -a 1.0 -MAGIC 0.0 > " + output_file_name;

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

    public void extract(String input_file_name) throws Exception
    {
        // Extract logF0 if wanted
        String[] tokens = (new File(input_file_name)).getName().split("\\.(?=[^\\.]+$)");
        String output_file_name = extToDir.get("vuv") + "/" + tokens[0] + ".vuv";
    }
}
