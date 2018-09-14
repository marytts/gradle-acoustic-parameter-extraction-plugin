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
 *  Extract weight coefficients
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class ExtractWeight extends ExtractBase
{
    /** The dimension of the weight vector */
    private int weight_dim;

    /**
     *  Constructor
     */
    public ExtractWeight() {
        setWeightOrder(12);
    }

    /**
     *  Constructor
     */
    public ExtractWeight(int order) {
        setWeightOrder(order);
    }

    /**
     *  Accessor to set the order of the weight vector
     *
     */
    public void setWeightOrder(int order) {
        weight_dim = order+1;
    }

    /**
     *  Extraction method
     *
     *  @param input_file the input file to extract the weights from
     */
    public void extract(File input_file)
        throws Exception
    {

        // JSON Part
        JSONParser parser = new JSONParser();
        JSONArray frames = (JSONArray) parser.parse(new FileReader(input_file));

        // Prepare byte buffer
        ByteBuffer bf = ByteBuffer.allocate(frames.size() * weight_dim * Float.BYTES);
        bf.order(ByteOrder.LITTLE_ENDIAN);

        // Convert JSON weights to raw values
        for (Object frame: frames)
        {
            JSONArray weights = (JSONArray) ((JSONObject) frame).get("phonemeWeights");
            for (Object weight: weights)
            {
                bf.putFloat(((Double) weight).floatValue());
            }
        }

        // Save the weight file
        Files.write(extToFile.get("weight").toPath(), bf.array());
    }
}
