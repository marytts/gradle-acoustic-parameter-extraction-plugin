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

    public void extract(String input_file_name) throws Exception
    {
        boolean end_header = false;
        File emaFile = new File(input_file_name);
        ArrayList<ArrayList<Double>> frames = new ArrayList<ArrayList<Double>>();
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
                ArrayList<Double> cur_frame = new ArrayList<Double>();
                StringTokenizer st = new StringTokenizer(line);
                int i=0;
                while (st.hasMoreTokens()) {
                    String it = st.nextToken();
                    if (i >= 2)
                        cur_frame.add(Double.parseDouble(it));
                    i++;
                }

                size += cur_frame.size();
                frames.add(cur_frame);
            }
        }

        // Doubles to bytes
        ByteBuffer bf = ByteBuffer.allocate(size*Double.SIZE/8); // FIXME: why this fucking 8 !
        bf.order(ByteOrder.LITTLE_ENDIAN);
        System.out.println("sizedouble = " + Double.SIZE/8);
        for (ArrayList<Double> frame: frames)
        {
            for (Double sample: frame)
            {
                bf.putDouble(sample);
            }
        }

        // Output
        String[] tokens_filename = (new File(input_file_name)).getName().split("\\.(?=[^\\.]+$)");
        File output_file = new File(extToDir.get("ema"), tokens_filename[0]);
        Files.write(output_file.toPath(), bf.array());

    }
}
