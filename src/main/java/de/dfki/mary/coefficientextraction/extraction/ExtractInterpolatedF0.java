package de.dfki.mary.coefficientextraction.extraction;



import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.util.Hashtable;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.FileReader;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Locale;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Coefficients extraction based on STRAIGHT
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class ExtractInterpolatedF0 extends ExtractBase
{
    private static final float LOGF0 = -1.0e+10f;

    public ExtractInterpolatedF0()
    {
    }

    private void linearInterpolation(String input_file_name, String output_file_name)
        throws Exception
    {
        // Load byte array of data
        Path p_input = FileSystems.getDefault().getPath("", input_file_name);
        byte[] data_bytes = Files.readAllBytes(p_input);
        ByteBuffer buffer = ByteBuffer.wrap(data_bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Compute size
        int T = data_bytes.length / 4; // FIXME: float size hardcoded

        // Generate vector C
        float[] input_data = new float[T];
        for (int i=0; i<T; i++)
        {
            input_data[i] = buffer.getFloat();
            if (Float.isNaN(input_data[i]))
            {
                throw new Exception(input_file_name + " contains nan values! ");
            }
        }

        float previous = LOGF0;

        for (int t=0; t<T; t++)
        {
            if (input_data[t] == LOGF0)
            {
                int shift = t + 1;
                while ((shift < T) && (input_data[shift] == LOGF0))
                {
                    shift++;
                }

                // Last
                if (shift == T)
                {
                    if (previous == LOGF0)
                        throw new Exception("only unvoiced F0, nonsense !");

                    for (int t2=t; t2<T; t2++)
                        input_data[t2] = previous;

                    // It is useless to continue the loop after
                    break;
                }
                // First
                else if (previous == LOGF0)
                {
                    float next = input_data[shift];
                    for (; t<shift; t++)
                        input_data[t] = next;

                }
                // Normal case: inner unvoiced section
                else
                {
                    float next = input_data[shift];
                    float step = (next - previous) / (shift - t + 1); // y = a.x + (b=0)
                    int cur_t = t;
                    for (; t<shift; t++)
                        input_data[t] = input_data[t-1] + step;
                }
            }

            previous = input_data[t];
        }

        // Saving interpolated F0
        ByteBuffer output_buffer = ByteBuffer.allocate(data_bytes.length);
        output_buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (int v=0; v<input_data.length;v++){
            output_buffer.putFloat(input_data[v]);
        }

        Path path = Paths.get(output_file_name);
        byte[] output_data_bytes = output_buffer.array();
        Files.write(path, output_data_bytes);
    }

    public void extract(String input_file_name) throws Exception
    {
        // Extract logF0 if wanted
        String[] tokens = (new File(input_file_name)).getName().split("\\.(?=[^\\.]+$)");
        String output_file_name = extToDir.get("interpolated_lf0") + "/" + tokens[0] + ".lf0";

        linearInterpolation(input_file_name, output_file_name);

    }
}
