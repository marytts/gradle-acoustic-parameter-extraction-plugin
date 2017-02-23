package de.dfki.mary.coefficientextraction.extraction;

import java.util.ArrayList;
import java.util.StringTokenizer;

import java.io.File;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;

/**
 * Extract EMA coefficients and only coefficients from the full EMA file
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class ExtractWeight extends ExtractBase
{

    private int order_weight;
    private static final int FLOAT_SIZE = Float.SIZE/8; // Float size in nb bytes...


    public ExtractWeight() {
        setWeightOrder(12);
    }

    public void setWeightOrder(int order) {
        order_weight = order;
    }

    public void extract(String input_file_name)
        throws Exception
    {

        JSONParser parser = new JSONParser();
        JSONArray frames = (JSONArray) parser.parse(new FileReader(input_file_name));

        ByteBuffer bf = ByteBuffer.allocate(frames.size()*(order_weight+1)*FLOAT_SIZE);
        bf.order(ByteOrder.LITTLE_ENDIAN);

        for (Object frame: frames)
        {
            JSONArray weights = (JSONArray) ((JSONObject) frame).get("phonemeWeights");
            for (Object weight: weights)
            {
                bf.putFloat(((Double)weight).floatValue());
            }
        }

        // Output
        File output_file = new File(extToDir.get("weight"), (new File(input_file_name)).getName());
        Files.write(output_file.toPath(), bf.array());
    }
}
