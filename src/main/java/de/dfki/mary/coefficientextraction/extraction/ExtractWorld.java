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
 * Coefficients extraction based on STRAIGHT
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class Extractorld extends ExtractBase
{
    private int sample_rate;
    private float frameshift;

    public ExtractWorld() throws Exception
    {
        setSampleRate(48000);
        setFrameshift(5);
    }

    public void setSampleRate(int sample_rate)
    {
        this.sample_rate = sample_rate;
    }

    public void setFrameshift(float frameshift)
    {
        this.frameshift = frameshift;
    }

    public void extract(String input_file_name) throws Exception
    {
        Process p;

        // Check directories
        for(String ext : Arrays.asList("ap", "f0", "sp")) {
            if (!extToDir.containsKey(ext))
            {
                throw new Exception(" extToDir does not contains \"" + ext + "\" associated directory");
            }
        }


        // Prepare filenames
        String[] tokens = (new File(input_file_name)).getName().split("\\.(?=[^\\.]+$)");
        String ap_output = extToDir.get("ap") + "/" + tokens[0] + ".ap";
        String sp_output = extToDir.get("sp") + "/" + tokens[0] + ".sp";
        String f0_output = extToDir.get("f0") + "/" + tokens[0] + ".f0";

        // 1. generate the script
        script_file = generateScript(input_file_name);

        // 2. extraction
        String[] cmd = {"world_analysis", input_file_name, f0_output, sp_output, ap_output};
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

        // 3. clean
        script_file.delete();
    }
}
