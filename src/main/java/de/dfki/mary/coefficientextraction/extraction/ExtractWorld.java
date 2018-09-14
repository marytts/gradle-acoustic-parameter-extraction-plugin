package de.dfki.mary.coefficientextraction.extraction;

// World wrapper
import jworld.*;

// Audio I/O
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;

// File part
import java.io.File;
import java.io.FileOutputStream;
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
 * Process to extract the acoustic parameters using the World vocoder
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class ExtractWorld extends ExtractBase
{
    /** The frame shift */
    private float frameshift;


    /**
     *  Default constructor which sets the frameshift to 5ms
     *
     */
    public ExtractWorld() throws Exception
    {
        setFrameshift(5);
    }

    /**
     *  Constructor parametrized by the frameshift
     *
     *  @param frameshift the frameshift used for the extraction
     */
    public ExtractWorld(float frameshift) throws Exception
    {
        setFrameshift(frameshift);
    }

    /**
     *  Accessor to set a new frameshift value
     *
     *  @param frameshift the new frameshift value
     */
    public void setFrameshift(float frameshift)
    {
        this.frameshift = frameshift;
    }


    /**
     *  Accessor to get the frameshift value
     *
     *  @return the frameshift value
     */
    public float getFrameshift()
    {
        return frameshift;
    }


    /**
     *  Extract the coefficients from the given input file
     *
     *  @param input_file the input wav file
     */
    public void extract(File input_file) throws Exception
    {
        // Check directories
        for(String ext : Arrays.asList("ap", "f0", "sp")) {
            if (!extToFile.containsKey(ext))
            {
                throw new Exception("extToFile does not contains \"" + ext + "\" associated output file path");
            }
        }

        // Read audio
        AudioInputStream ais = AudioSystem.getAudioInputStream(input_file);

        // Initialize world wrapper
        JWorldWrapper jww = new JWorldWrapper(ais);
        jww.setFramePeriod(getFrameshift());

        // Extract
        double[] f0 = jww.extractF0(true);
        double[][] sp = jww.extractSP();
        double[][] ap = jww.extractAP();

        // Save results in float!
        ByteBuffer bf;
        FileOutputStream os;

        // - F0
        bf = ByteBuffer.allocate(f0.length * Float.BYTES);
        bf.order(ByteOrder.LITTLE_ENDIAN);
        for (int t=0; t<f0.length; t++)
            bf.putFloat((float) f0[t]);
        bf.rewind();

        os = new FileOutputStream(extToFile.get("f0"));
        os.write(bf.array());

        // - SP
        bf = ByteBuffer.allocate(sp.length * sp[0].length * Float.BYTES);
        bf.order(ByteOrder.LITTLE_ENDIAN);
        for (int t=0; t<sp.length; t++)
            for (int d=0; d<sp[0].length; d++)
            bf.putFloat((float) sp[t][d]);
        bf.rewind();

        os = new FileOutputStream(extToFile.get("sp"));
        os.write(bf.array());

        // - AP
        bf = ByteBuffer.allocate(ap.length * ap[0].length * Float.BYTES);
        bf.order(ByteOrder.LITTLE_ENDIAN);
        for (int t=0; t<ap.length; t++)
            for (int d=0; d<ap[0].length; d++)
            bf.putFloat((float) ap[t][d]);
        bf.rewind();

        os = new FileOutputStream(extToFile.get("ap"));
        os.write(bf.array());
    }
}
