package de.dfki.mary.coefficientextraction.extraction;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Arrays;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

/**
 * Coefficients extraction based on WORLD
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class ExtractWorld extends ExtractBase
{
    private float frameshift;
    private static final float MAGIC_VALUE = 32768.0f;

    public ExtractWorld() throws Exception
    {
        setFrameshift(5);
    }

    public void setFrameshift(float frameshift)
    {
        this.frameshift = frameshift;
    }

    private void worldExtraction(String input_file_name, String f0_output, String sp_output, String ap_output)
        throws Exception
    {
        Process p;

        // 2. extraction
        String[] cmd = {"analysis", (new Float(this.frameshift)).toString(), input_file_name, f0_output, sp_output, ap_output};
        p = Runtime.getRuntime().exec(cmd);
        p.waitFor();


        BufferedReader reader =
            new BufferedReader(new InputStreamReader(p.getInputStream()));

        String line = "";
        // while ((line = reader.readLine())!= null) {
        //         System.out.println(line);
        // }

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

    private void f0Conversion(String input_file_name, String output_file_name) throws Exception
    {
        Process p;

        // 2. extraction
        String[] cmd = {"bash", "-c", "x2x +df " + input_file_name + " > " + output_file_name};
        p = Runtime.getRuntime().exec(cmd);
        p.waitFor();


        BufferedReader reader =
            new BufferedReader(new InputStreamReader(p.getInputStream()));

        String line = "";
        // while ((line = reader.readLine())!= null) {
        //         System.out.println(line);
        // }

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

    private void spectrumConversion(String input_file_name, String output_file_name) throws Exception
    {
        Process p;

        // 2. extraction
        String[] cmd = {"bash", "-c", "x2x +df " + input_file_name + " | sopr -R -m " + MAGIC_VALUE + " > "+ output_file_name};
        p = Runtime.getRuntime().exec(cmd);
        p.waitFor();


        BufferedReader reader =
            new BufferedReader(new InputStreamReader(p.getInputStream()));

        String line = "";
        // while ((line = reader.readLine())!= null) {
        //         System.out.println(line);
        // }

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
        // Check directories
        for(String ext : Arrays.asList("ap", "f0", "sp")) {
            if (!extToDir.containsKey(ext))
            {
                throw new Exception(" extToDir does not contains \"" + ext + "\" associated directory");
            }
        }

        String[] tokens = (new File(input_file_name)).getName().split("\\.(?=[^\\.]+$)");
        String ap_output = extToDir.get("ap") + "/" + tokens[0] + ".ap";
        String sp_output = extToDir.get("sp") + "/" + tokens[0] + ".sp";
        String f0_output = extToDir.get("f0") + "/" + tokens[0] + ".f0";

        String ap_tmp = extToDir.get("ap") + "/" + tokens[0] + ".tap";
        String sp_tmp = extToDir.get("sp") + "/" + tokens[0] + ".tsp";
        String f0_tmp = extToDir.get("f0") + "/" + tokens[0] + ".tf0";

        // Extraction
        worldExtraction(input_file_name, f0_tmp, sp_tmp, ap_tmp);

        // Conversion for a proper SPTK use
        f0Conversion(f0_tmp, f0_output);
        spectrumConversion(ap_tmp, ap_output);
        spectrumConversion(sp_tmp, sp_output);

        // Cleaning
        (new File(f0_tmp)).delete();
        (new File(ap_tmp)).delete();
        (new File(sp_tmp)).delete();
    }
}
