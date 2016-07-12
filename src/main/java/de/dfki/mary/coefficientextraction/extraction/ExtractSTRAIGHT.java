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
public class ExtractSTRAIGHT extends ExtractBase
{
    // magic number for turning off normalization (72089600 = 2200 * 32768)
    private static final int straight_magic = 72089600;
    private String straight_path;
    private int sample_rate;
    private float frameshift;
    private int mini_F0;
    private int maxi_F0;
    private boolean logF0Flag;

    public ExtractSTRAIGHT() throws Exception
    {
        throw new Exception("cannot be used: call \"new ExtractSTRAIGHT(String straight_path)\" instead");
    }

    /**
     * STRAIGHT parameter wrapper constructor. Default parameters are set according to the default values of the HTS demo
     *
     *   @param straight_path : the straight toolbox path needed
     */
    public ExtractSTRAIGHT(String straight_path)
    {
        // Demo parameters
        this.straight_path = straight_path;
        setSampleRate(48000);
        setFrameshift(5);
        setMinimumF0(110);
        setMaximumF0(280);
    }

    public void setSampleRate(int sample_rate)
    {
        this.sample_rate = sample_rate;
    }

    public void setFrameshift(float frameshift)
    {
        this.frameshift = frameshift;
    }

    public void setMinimumF0(int F0)
    {
        this.mini_F0 = F0;
    }

    public void setMaximumF0(int F0)
    {
        this.maxi_F0 = F0;
    }

    private double computeNormalisationRate(String input_file_name)
        throws Exception
    {
        double nb_values = 0;
        double mean = 0.0;
        double norm_coef = 0.0;

        RandomAccessFile aFile = new RandomAccessFile(input_file_name, "r");
        FileChannel inChannel = aFile.getChannel();

        // Skip header [FIXME: hard coded for wav files]
        ByteBuffer buffer = ByteBuffer.allocate(44);
        inChannel.read(buffer);
        buffer.clear();

        // Compute the variance of the data
        buffer = ByteBuffer.allocate(1024);
        buffer.order(ByteOrder.LITTLE_ENDIAN); // Data is in little_endian not big endian !
        while(inChannel.read(buffer) > 0)
        {
            buffer.flip();
            for (int i = 0; i < buffer.limit(); i+=2) // FIXME: i += 2 => if short size changes ?
            {
                double tmp = (new Short(buffer.getShort())).doubleValue();
                mean += tmp;
                nb_values += 1;
                norm_coef += tmp * tmp;
            }
            buffer.clear(); // do something with the data and clear/compact it.
        }
        inChannel.close();
        aFile.close();

        mean = mean / nb_values;
        norm_coef = norm_coef/nb_values - mean * mean; // Variance first !

        // Appl
        norm_coef = (1.0/Math.sqrt(norm_coef)) * straight_magic;

        return norm_coef;
    }

    private File generateScript(String input_file_name) throws Exception
    {

        // Normalisation coefficient computation
        double norm_coef = computeNormalisationRate(input_file_name);

        // Prepare filenames
        String[] tokens = (new File(input_file_name)).getName().split("\\.(?=[^\\.]+$)");
        String ap_output = extToDir.get("ap") + "/" + tokens[0] + ".ap";
        String sp_output = extToDir.get("sp") + "/" + tokens[0] + ".sp";
        String f0_output = extToDir.get("f0") + "/" + tokens[0] + ".f0";


        File script_file = File.createTempFile("extract", ".m");
        PrintWriter writer = new PrintWriter(script_file, "UTF-8");
        writer.println("path(path, '" + this.straight_path + "');");
        writer.println("");

        writer.println("prm.spectralUpdateInterval = " + this.frameshift + ";");
        writer.println("prm.F0frameUpdateInterval = " + this.frameshift + ";");
        writer.println("prm.F0searchLowerBound = " + this.mini_F0 + ";");
        writer.println("prm.F0searchUpperBound = " + this.maxi_F0 + ";");
        writer.println("");

        writer.println("[x, fs] = audioread('" + input_file_name +"');");
        writer.println("[f0, ap] = exstraightsource(x, fs, prm);");
        writer.println("[sp] = exstraightspec(x, f0, fs, prm);");
        writer.println("");

        writer.println("ap = ap';");
        writer.println("sp = sp';");
        writer.println("sp = sp * " + norm_coef + ";");
        writer.println("");

        writer.println("save '" + ap_output + "' ap -ascii;");
        writer.println("save '" + sp_output + "' sp -ascii;");
        writer.println("save '" + f0_output + "' f0 -ascii;");


        writer.close();

        return script_file;
    }


    public void extract(String input_file_name) throws Exception
    {
        File script_file = new File("not defined");
        Process p;

        // Check directories
        for(String ext : Arrays.asList("ap", "f0", "sp")) {
            if (!extToDir.containsKey(ext))
            {
                throw new Exception(" extToDir does not contains \"" + ext + "\" associated directory");
            }
        }
        if ((logF0Flag) && (!extToDir.containsKey("lf0")))
        {
            throw new Exception(" extToDir does not contains \"lf0\" associated directory");
        }

        // 1. generate the script
        script_file = generateScript(input_file_name);

        // 2. extraction
        String command = "matlab -nojvm -nodisplay -nosplash < \"" + script_file.getPath() + "\"";
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
            if (!(line.endsWith("/lib64/libc.so.6: not found"))) // FIXME: libc6 patch
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
