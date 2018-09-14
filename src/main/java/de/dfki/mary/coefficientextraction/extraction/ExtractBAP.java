package de.dfki.mary.coefficientextraction.extraction;


// IO
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.apache.commons.io.IOUtils;

import jsptk.JSPTKWrapper;
import java.util.Hashtable;

/**
 * Coefficients extraction based on STRAIGHT
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class ExtractBAP extends ExtractBase
{
    private double determinant_threshold;
    private int maximum_iteration;
    private double periodogram_noise_value;
    private int frame_length;
    private int input_format;
    private double samplerate;
    private double freqwarp;
    private int order;



    public double getDeterminantThreshold() {
        return determinant_threshold;
    }

    public void setDeterminantThreshold(double determinant_threshold) {
        this.determinant_threshold = determinant_threshold;
    }

    public int getMaximumIteration() {
        return maximum_iteration;
    }

    public void setMaximumIteration(int maximum_iteration) {
        this.maximum_iteration = maximum_iteration;
    }

    public double getPeriodogramNoiseValue() {
        return periodogram_noise_value;
    }

    public void setPeriodogramNoiseValue(double periodogram_noise_value) {
        this.periodogram_noise_value = periodogram_noise_value;
    }

    public ExtractBAP()
    {
        setSampleRate(48f);
        setFreqWarp(getSampleRate());
        setOrder((int) 24);
        setFrameLength(2048);
        setPeriodogramNoiseValue((double) 1.0E-08);
        setInputFormat(1);
    }

    private double getFreqWarp() {
        return freqwarp;
    }

    private void setFreqWarp(double samplerate)
    {

        if (samplerate == 8f)
        {
            freqwarp = 0.31f;
        }
        else if (samplerate == 10f)
        {
            freqwarp = 0.35f;
        }
        else if (samplerate == 12f)
        {
            freqwarp = 0.37f;
        }
        else if (samplerate == 16f)
        {
            freqwarp = 0.42f;
        }
        else if (samplerate == 22.5f)
        {
            freqwarp = 0.45f;
        }
        else if (samplerate == 32f)
        {
            freqwarp = 0.45f;
        }
        else if (samplerate == 44.1f)
        {
            freqwarp = 0.53f;
        }
        else if (samplerate == 48f)
        {
            freqwarp = 0.55f;
        }
        else
        {
            freqwarp = 0f; // FIXME: exception instead ?
        }
    }

    public int getOrder() {
        return this.order;
    }

    public void setOrder(int order)
    {
        this.order = order;
    }

    public void setMFCCLength(int length)
    {
        this.order = (int) (length - 1);
    }

    public int getFrameLength() {
        return frame_length;
    }

    public void setFrameLength(int frame_length) {
        this.frame_length = frame_length;
    }

    public int getInputFormat() {
        return input_format;
    }

    public void setInputFormat(int input_format) {
        this.input_format = input_format;
    }


    public double getSampleRate() {
        return this.samplerate;
    }

    public void setSampleRate(double samplerate) {
        this.samplerate = samplerate;
        setFreqWarp(this.samplerate);
    }

    public void extract(File input_file) throws Exception
    {
        Process p;

        // 1. Generate full command
        String command = "cat " + input_file.toString() + " |";
        command += 	"mcep -a " + freqwarp + " -m " + order + " -l 2048 -e 1.0E-08 -j 0 -f 0.0 -q 1 > " + extToFile.get("bap").toString();

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

    // {
    //     // Load ap into double array
    //     byte[] bytes = IOUtils.toByteArray(new FileInputStream(input_file));
    //     ByteBuffer in_bf = ByteBuffer.allocate(bytes.length);
    //     in_bf.order(ByteOrder.LITTLE_ENDIAN);
    //     in_bf.put(bytes);
    //     in_bf.rewind();

    //     int length = (getFrameLength() / 2) + 1; // FIXME:
    //     DoubleBuffer doubleBuffer = in_bf.asDoubleBuffer();
    //     double[][] ap = new double[length][doubleBuffer.remaining()/length];
    //     for(int t=0; t<length; t++){
    //         doubleBuffer.get(ap[t]);
    //     }

    //     // Compute bap using mcep
    //     double[][] bap = JSPTKWrapper.mcep(ap, getOrder(),
    //                                        getFreqWarp(), 2, getMaximumIteration(), // FIXME: hardcoded
    //                                        0.001f, 1, getPeriodogramNoiseValue(),   // FIXME: hardcoded
    //                                        getDeterminantThreshold(), getInputFormat());

    //     // Generate byte buffer
    //     ByteBuffer out_bf = ByteBuffer.allocate(bap.length * bap[0].length * Float.BYTES);
    //     out_bf.order(ByteOrder.LITTLE_ENDIAN);
    //     for (int t=0; t<bap.length; t++)
    //         for (int d=0; d<bap[0].length; d++)
    //         out_bf.putFloat((float) ap[t][d]);
    //     out_bf.rewind();

    //     // Save into file
    //     FileOutputStream os = new FileOutputStream(extToFile.get("bap"));
    //     os.write(out_bf.array());

    // }
}
