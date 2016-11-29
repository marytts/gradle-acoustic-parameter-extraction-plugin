package de.dfki.mary.coefficientextraction.extraction;

import java.util.ArrayList;
import java.io.File;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.StringTokenizer;
import java.nio.file.StandardOpenOption;

import de.dfki.mary.coefficientextraction.extraction.ema.HeadCorrection;

/**
 * Extract EMA coefficients and only coefficients from the full EMA file
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class ExtractEMA extends ExtractBase
{
    private static final int DEFAULT_VECTOR_SIZE = 21;
    private int[] channels;
    private int idx_offset;
    private int vector_size;
    private static final int FLOAT_SIZE = Float.SIZE/8; // Float size in nb bytes...

    public ExtractEMA(int[] channels) {
        setChannels(channels);
        idx_offset = 2; // FIXME: bad name
    }

    public void setChannels(int[] channels)
    {
        this.channels = channels;
        vector_size = channels.length * 3;
    }

    public void extract(String input_file_name)
        throws Exception
    {
        boolean end_header = false;
        File emaFile = new File(input_file_name);
        ArrayList<ArrayList<Float>> frames = new ArrayList<ArrayList<Float>>();
        int size = 0;

        String[] cmd = {"ch_track", "-otype",  "est", emaFile.toString()};
        Process p = Runtime.getRuntime ().exec (cmd);
        // p.waitFor();

        BufferedReader reader =
            new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while ((line = reader.readLine())!= null)
        {
            if ((!end_header) && (line.equals("EST_Header_End")))
            {
                end_header = true;
            }
            else if (end_header)
            {
                ArrayList<Float> cur_frame = new ArrayList<Float>();
                StringTokenizer st = new StringTokenizer(line);
                int i=0;
                while (st.hasMoreTokens()) {
                    String it = st.nextToken();
                    if (i >= idx_offset)
                    {
                        if ((it.equals("nan")) ||  (it.equals("-nan")))
                        {
                            cur_frame.add(Float.NaN);
                        }
                        else
                        {
                            cur_frame.add((float)Double.parseDouble(it));
                        }
                    }
                    i++;
                }

                size += cur_frame.size();
                frames.add(cur_frame);
            }
        }


        BufferedReader err_reader =
            new BufferedReader(new InputStreamReader(p.getErrorStream()));
        String error = "";
        while ((line = err_reader.readLine())!= null)
        {
            error += line;
        }
        if (!error.isEmpty())
            throw new Exception(error);

        // Floats to bytes
        ByteBuffer bf = ByteBuffer.allocate(frames.size()*vector_size*FLOAT_SIZE);
        bf.order(ByteOrder.LITTLE_ENDIAN);
        for (int i=0; i<frames.size(); i++)
        {
            // Load frame
            ArrayList<Float> frame = frames.get(i);
            for (int j=0; j<frame.size(); j++)
            {
                Float sample = frame.get(j);

                // If nan => interpolation
                if (Float.isNaN(sample))
                {
                    Float val = 0.0f;
                    int nb = 0;
                    int offset = 1;

                    while (((i-offset) >= 0) &&
                           (Float.isNaN(frames.get(i-offset).get(j))))
                    {
                        offset++;
                    }
                    if ((i-offset) >= 0) {
                        val += (frames.get(i-offset)).get(j);
                        nb++;
                    }


                    offset = 1; // Reset offset to 1
                    while (((i+offset) <= (frames.size()-1)) &&
                           (Float.isNaN(frames.get(i+offset).get(j))))
                    {
                        offset++;
                    }

                    if ((i+offset) <= (frames.size()-1))
                    {
                        val += (frames.get(i+offset)).get(j);
                        nb++;
                    }

                    sample = val / (float) nb;

                    frame.set(j, sample);
                }
            }

            // Extract desired channels
            for (int c=0; c<channels.length;c++)
                for (int j=0; j<3; j++)
                    bf.putFloat( frame.get(channels[c]+j));
        }

        // Output
        File output_file = new File(extToDir.get("ema"), (new File(input_file_name)).getName());
        Files.write(output_file.toPath(), bf.array());
    }
}
