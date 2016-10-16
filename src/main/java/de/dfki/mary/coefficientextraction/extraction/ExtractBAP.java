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
public class ExtractBAP extends ExtractBase
{
    private float samplerate;
    private float freqwarp;
    private short order;

    public ExtractBAP()
    {
        setSampleRatekHz(48f);
        setOrder((short) 24);
    }


    private void setFreqWarp(float sampleratekHz)
    {

        if (sampleratekHz == 8f)
        {
            freqwarp = 0.31f;
        }
        else if (sampleratekHz == 10f)
        {
            freqwarp = 0.35f;
        }
        else if (sampleratekHz == 12f)
        {
            freqwarp = 0.37f;
        }
        else if (sampleratekHz == 16f)
        {
            freqwarp = 0.42f;
        }
        else if (sampleratekHz == 22.5f)
        {
            freqwarp = 0.45f;
        }
        else if (sampleratekHz == 32f)
        {
            freqwarp = 0.45f;
        }
        else if (sampleratekHz == 44.1f)
        {
            freqwarp = 0.53f;
        }
        else if (sampleratekHz == 48f)
        {
            freqwarp = 0.55f;
        }
        else
        {
            freqwarp = 0f; // FIXME: exception instead ?
        }
    }

    public void setOrder(short order)
    {
        this.order = order;
    }

    public void setLength(short length)
    {
        this.order = (short) (length - 1);
    }

    public void setSampleRate(float samplerate)
    {
        setSampleRatekHz((float)(samplerate * 0.001));
    }

    public void setSampleRatekHz(float samplerate)
    {
        this.samplerate = samplerate;
        setFreqWarp(this.samplerate);
    }

    public void extract(String input_file_name) throws Exception
    {

        String[] tokens = (new File(input_file_name)).getName().split("\\.(?=[^\\.]+$)");
        String output_file_name = extToDir.get("bap") + "/" + tokens[0] + ".bap";

        Process p;

        // 1. Generate full command
        String command = "cat " + input_file_name + " |";
        command += 	"mgcep -a " + freqwarp + " -m " + order + " -l 2048 -e 1.0E-08 -j 0 -f 0.0 -q 1 > " + output_file_name;

        // 2. extraction
        String[] cmd = {"bash", "-c", command};
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
}
