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
public class ExtractMGC extends ExtractBase
{
    private float samplerate;
    private float freqwarp;
    private float gamma;
    private short order;
    private boolean lngain_flag;
    private boolean spectrum_flag;

    public ExtractMGC()
    {
        setSampleRatekHz(48f);
        setOrder((short) 49);
        setGamma(0);
        setLogGainFlag(true);
        spectrum_flag = true;
    }

    public ExtractMGC(boolean spectrum_flag)
    {
        setSampleRatekHz(48f);
        setOrder((short) 49);
        setGamma(0);
        setLogGainFlag(true);
        spectrum_flag = false;
    }

    private void setFreqWarp(float sampleratekHz)
    {

        if (sampleratekHz == 8)
        {
            freqwarp = 0.31f;
        }
        else if (sampleratekHz == 10)
        {
            freqwarp = 0.35f;
        }
        else if (sampleratekHz == 12)
        {
            freqwarp = 0.37f;
        }
        else if (sampleratekHz == 16)
        {
            freqwarp = 0.42f;
        }
        else if (sampleratekHz == 22.5)
        {
            freqwarp = 0.45f;
        }
        else if (sampleratekHz == 32)
        {
            freqwarp = 0.45f;
        }
        else if (sampleratekHz == 44.1)
        {
            freqwarp = 0.53f;
        }
        else if (sampleratekHz == 48)
        {
            freqwarp = 0.55f;
        }
        else
        {
            freqwarp = 0f; // FIXME: exception instead ?
        }
    }

    public void setGamma(float gamma)
    {
        this.gamma = gamma;
    }

    public void setOrder(short order)
    {
        this.order = order;
    }

    public void setLogGainFlag(boolean lngain_flag)
    {
        this.lngain_flag = lngain_flag;
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

    /**
     *
     *
     */
    public void extractFromSpectrum(String input_file_name, String output_file_name)
        throws Exception
    {
        Process p;

        // 1. Generate full command
        String command = "cat " + input_file_name + " |";
        if (gamma == 0)
        {
            command += 	"mcep -a " + freqwarp + " -m " + order + " -l 2048 -e 1.0E-08 -j 0 -f 0.0 -q 3 > " + output_file_name;
        }
        else
        {
            String logGainOpt = "";
            if (lngain_flag)
            {
                logGainOpt = " -l";
            }
            command += 	"mcep -a " + freqwarp + " -m " + order + " -l 2048 -e 1.0E-08 -j 0 -f 0.0 -q 3 -o 4 | ";
            command +=  "lpc2lsp -m " + order + logGainOpt + " -n 2048 -d 1.0E-08 -p 8 > " + output_file_name;
        }


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


    public void extractFromWav(String input_file_name, String output_file_name) throws Exception
    {
        throw new Exception("Method extractFromWav not implemented yet");
    }

    public void extract(File input_file) throws Exception
    {
        if (spectrum_flag)
        {
            extractFromSpectrum(input_file.toString(), extToFile.get("mgc").toString());
        }
        else
        {
            extractFromWav(input_file.toString(), extToFile.get("mgc").toString());
        }
    }
}
