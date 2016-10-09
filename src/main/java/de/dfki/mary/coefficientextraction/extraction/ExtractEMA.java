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

/**
 * Extract EMA coefficients and only coefficients from the full EMA file
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class ExtractEMA extends ExtractBase
{
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
                    if (i >= 2)
                    {
                        if ((it == "nan") ||  (it == "-nan"))
                        {
                            cur_frame.add((float) Math.log(Double.parseDouble(it)));
                        }
                        else
                        {
                            cur_frame.add(Float.NaN);
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
        while ((line = err_reader.readLine())!= null)
        {
            System.out.println("error = " + line);
        }

        // Floats to bytes
        ByteBuffer bf = ByteBuffer.allocate(size*Float.SIZE/4);
        bf.order(ByteOrder.LITTLE_ENDIAN);
        for (int i=0; i<frames.size(); i++)
        {
            ArrayList<Float> frame = frames.get(i);
            for (int j=0; j<frame.size(); j++)
            {
                Float sample = frame.get(j);

                // If nan => linear interpolation
                if (Float.isNaN(sample))
                {
                    Float val = 0.0f;
                    int nb = 0;
                    if (i>0)
                    {
                        val += (frames.get(i-1)).get(j);
                        nb++;
                    }

                    if (i<(frames.size()-1))
                    {
                        val += (frames.get(i+1)).get(j);
                        nb++;
                    }

                    sample = val / (float) nb;
                }

                // Add float
                bf.putFloat(sample);
            }
        }

        // Output
        File output_file = new File(extToDir.get("ema"), (new File(input_file_name)).getName());
        Files.write(output_file.toPath(), bf.array());

    }
}
