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
import static java.nio.file.StandardCopyOption.*;



import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import org.ejml.simple.SimpleMatrix;

/**
 * Generation of CMP HTS observation file
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class ExtractCMP extends ExtractBase
{
    private static final float IGNORE_VALUE = (float) -1.0e+10;
    private JSONObject config;


    public ExtractCMP() throws Exception
    {
        throw new Exception("cannot be used: call \"new ExtractCMP(String config_path)\" instead");
    }

    public ExtractCMP(String config_path) throws Exception
    {
        loadConfig(config_path);
    }

    public void loadConfig(String config_path) throws Exception
    {
        JSONParser parser = new JSONParser();
        config = (JSONObject) parser.parse(new FileReader(config_path));
    }


    /**
     *  Compute the equation O = W.C to get the observations
     *
     */
    private static void applyWindows(String input_file_name, String output_file_name,
                                     int dim, ArrayList<String> window_file_names,
                                     boolean is_msd)
        throws Exception
    {
        // Load byte array of data
        Path p_input = FileSystems.getDefault().getPath("", input_file_name);
        byte[] data_bytes = Files.readAllBytes(p_input);
        ByteBuffer buffer = ByteBuffer.wrap(data_bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Compute size
        int T = data_bytes.length / (dim*4);
        int input_data_size = T*dim;
        int n_win = window_file_names.size();

        // Generate vector C
        Float[] input_data = new Float[input_data_size];
        for (int i=0; i<input_data_size; i++)
        {
            input_data[i] = buffer.getFloat();
        }

        // Allocate vector O
        Float[] output_data = new Float[input_data_size * window_file_names.size()];

        // Load window and apply it
        ArrayList<Float[]> win_matrix = new ArrayList<Float[]>();
        int i_win = 0;
        for (String win_filename: window_file_names)
        {
            // Read the file
            BufferedReader reader =
                new BufferedReader(new InputStreamReader(new FileInputStream(win_filename)));
            String line = reader.readLine();
            String[] elts = line.split(" ");

            // Extract window elements
            int size = Integer.parseInt(elts[0]);
            if ((size % 2) != 1)
            {
                throw new Exception("Size need to be odd !");
            }

            Float[] win = new Float[size];
            for (int f=1; f<=size; f++)
                win[f-1] = Float.parseFloat(elts[f]);

            // Check boundary flags
            boolean[] check_bound = new boolean[size+1];
            for (int j=0; j<=size; j++)
                check_bound[j] = true;

            for (int j=1; j<=size; j++)
            {
                if (win[j-1] != 0.0)
                    break;
                check_bound[j] = false;
            }
            for (int j=size; j>=1; j--)
            {
                if (win[j-1] != 0.0)
                    break;
                check_bound[j] = false;
            }

            // Calcul i-th coefficients
            int nlr = (size - 1) / 2;
            for (int t=0; t<T; t++)
            {
                for (int d=0; d<dim; d++)
                {
                    boolean boundary = false;
                    for (int k=-nlr; k<=nlr; k++)
                    {
                        if (check_bound[k+nlr+1])
                        {
                            int l = 0;
                            if ((t + k) < 0)
                            {
                                l = 0;
                            }
                            else if ((t + l) >= T)
                            {
                                l = T - 1;
                            }
                            else
                            {
                                l = t + k;
                            }

                            if ((is_msd) && (input_data[l*dim+d] == ExtractCMP.IGNORE_VALUE))
                                boundary = true;
                        }
                    }

                    // Normal case window
                    if (!boundary)
                    {
                        output_data[t*n_win*dim + dim * i_win + d] = (float) 0.0;

                        for (int k=-nlr; k<=nlr; k++)
                        {
                            if ((t + k) < 0)
                            {
                                output_data[t*n_win*dim + dim * i_win + d] += win[k+nlr] * input_data[d];
                            }
                            else if ((t + k) >= T)
                            {
                                output_data[t*n_win*dim + dim * i_win + d] += win[k+nlr] * input_data[(T-1*dim)+d];
                            }
                            else
                            {
                                output_data[t*n_win*dim + dim * i_win + d] += win[k+nlr] * input_data[((t+k)*dim)+d];
                            }
                        }

                    }
                    // Boundary adaptation
                    else
                    {
                        output_data[t*n_win*dim + dim * i_win + d] = ExtractCMP.IGNORE_VALUE;
                    }

                }
            }

            // Now go the next windows
            i_win++;
        }

        // Saving O
        ByteBuffer output_buffer = ByteBuffer.allocate(n_win*data_bytes.length);
        output_buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (int v=0; v<output_data.length;v++){
            output_buffer.putFloat(output_data[v]);
        }

        Path path = Paths.get(output_file_name);
        byte[] output_data_bytes = output_buffer.array();
        Files.write(path, output_data_bytes);
    }

    /**
     *  Add an HTK header to the input cmp file
     *
     *   @param input_file_name the input cmp file path
     *   @param output_file_name the output cmp file with header path
     *   @param frameshift the used frameshift in HTK format
     *   @param framesize the number of coefficients for one frame
     *   @param HTK_feature_type the HTK feature type information
     */
    public static void addHTKHeader(String input_file_name, String output_file_name,
                                     long frameshift, short framesize, short HTK_feature_type)
        throws IOException
    {
        File output = new File(output_file_name);
        Path p_input = FileSystems.getDefault().getPath("", input_file_name);
        byte[] data = Files.readAllBytes(p_input);

        // Prepare buffer
        long nb_frames = data.length / framesize;
        ByteBuffer buffer = ByteBuffer.allocate((Integer.SIZE + Short.SIZE)/4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);


        // Generate header
        buffer.putInt((int) nb_frames);
        buffer.putInt((int) frameshift);

        buffer.putShort(framesize);
        buffer.putShort(HTK_feature_type);

        // Generate the output data
        byte[] header = buffer.array();
        byte[] output_data = new byte[header.length + data.length];
        System.arraycopy(header, 0, output_data, 0, header.length);
        System.arraycopy(data, 0, output_data, header.length, data.length);

        Path path = Paths.get(output_file_name);
        Files.write(path, output_data);
    }

    /**
     *  Merge frame by frame two files
     *
     *    @param file1_name the first file
     *    @param file2_name the second file
     *    @param output_file_name the produced file
     *    @param file1_name dimension of vectors contained by the first file
     *    @param file2_name dimension of vectors contained by the second file
     */
    private void merge(String file1_name, String file2_name, String output_file_name, long file1_vecsize, long file2_vecsize)
        throws Exception
    {
        Process p;

        // 1. Generate full command
        String command = "merge +f -s 0 -l " + file2_vecsize + " -L " + file1_vecsize + " " +
            file1_name + " < " + file2_name + " > " + output_file_name;

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


    /**
     *  Simple method to deal with project directories
     *
     *    @param winfiles : the original window filenames from the configuration
     *    @returns the window filenames adapted to the project architecture
     */
    private ArrayList<String> adaptWinFilePathes(ArrayList<String> winfiles) {
        JSONObject data = (JSONObject) config.get("data");
        try {
            String project_dir = (String) data.get("project_dir");
            ArrayList<String> result_winfiles = new ArrayList<String>();
            for (String win: winfiles) {
                if (win.startsWith("/")) {
                    result_winfiles.add(win);
                } else {
                    result_winfiles.add(project_dir + "/" + win);
                }
            }
            return result_winfiles;
        } catch (Exception e) { // FIXME: check which exception !
            return winfiles;
        }
    }


    /**
     * Extraction method => generate the cmp file
     *
     *   @param basename: the basename of the utterance file analyzed. Each specific filename is
     *   built using the kind officients (extensio)
     */
    public void extract(String basename) throws Exception
    {
        // Preparation of temp stuffs
        String tmp_filename = File.createTempFile("0000" + basename, "").getPath();


        // Loading signal config informations
        JSONObject signal = (JSONObject) config.get("signal");
        long samplerate = ((Long) signal.get("samplerate"));
        long frameshift = ((Long) signal.get("frameshift")) * 10000;

        // Loading stream informations
        JSONObject models = (JSONObject) config.get("models");
        JSONObject cmp = (JSONObject) models.get("cmp");
        JSONArray streams = (JSONArray) cmp.get("streams");

        long total_vecsize = 0;
        ArrayList<Hashtable<String, String>> internal_streams = new ArrayList<Hashtable<String, String>>();
        for (Object cur_elt : streams)
        {
            JSONObject cur_stream = (JSONObject) cur_elt;
            Hashtable<String, String> cur_internal_stream = new Hashtable<String, String>();

            // MSD Information
            Boolean is_msd = (Boolean) cur_stream.get("is_msd");

            // First get the dimension of the current stream (static only)
            int vecsize = ((Long) cur_stream.get("order")).intValue() + 1;
            cur_internal_stream.put("dim", Long.toString(vecsize));

            // Add the tmp observation file and apply the windows to get it

            ArrayList<String> winfiles = new ArrayList<String>();
            for (Object cur_win : (JSONArray) cur_stream.get("winfiles"))
            {
                winfiles.add((String) cur_win);
            }
            winfiles = adaptWinFilePathes(winfiles);
            int nwin = winfiles.size();
            String kind = (String) cur_stream.get("kind");
            String cur_dir = (String) extToDir.get(kind);
            applyWindows(cur_dir + "/" + basename + "." + kind, tmp_filename + "." + kind,
                         (int)vecsize, winfiles, is_msd);
            cur_internal_stream.put("kind", kind);
            cur_internal_stream.put("obs_file_name", tmp_filename + "." + kind);


            // Add the dynamic to get the final vecsize
            vecsize *= nwin;
            cur_internal_stream.put("vecsize", Long.toString(vecsize));

            // Add a tmp element to save the current size of the vector
            cur_internal_stream.put("cur_total_nsize", Long.toString(total_vecsize));
            total_vecsize += vecsize;


            // Add to the list
            internal_streams.add(cur_internal_stream);
        }

        // Merging
        Hashtable<String, String> cur_stream = internal_streams.get(0);

        String previous_state_filename = tmp_filename + ".tmp_cmp";
        String cur_state_filename = tmp_filename + ".tmp2_cmp";

        // Copy first stream to the tmp previous file
        Files.copy(Paths.get(internal_streams.get(0).get("obs_file_name")), Paths.get(previous_state_filename), REPLACE_EXISTING);
        (new File(internal_streams.get(0).get("obs_file_name"))).delete();

        for (int i=1; i<internal_streams.size(); i++)
        {
            // Merge previous state + current stream
            cur_stream = internal_streams.get(i);
            merge(previous_state_filename, cur_stream.get("obs_file_name"), cur_state_filename,
                  Long.parseLong(cur_stream.get("cur_total_nsize")),
                  Long.parseLong(cur_stream.get("vecsize")));


            // Swich (string to save time and space)
            String tmp = cur_state_filename;
            cur_state_filename = previous_state_filename;
            previous_state_filename = tmp;

            // Delete coef file
            (new File(cur_stream.get("obs_file_name"))).delete();
        }

        // Add header
        short nb_bytes_frame = (new Integer(4 * ((new Long(total_vecsize)).intValue()))).shortValue();
        addHTKHeader(previous_state_filename, extToDir.get("cmp") + "/" + basename + ".cmp",
                     frameshift, nb_bytes_frame, (short) 9);

        // Delete temps files
        (new File(previous_state_filename)).delete();
        (new File(cur_state_filename)).delete();
        (new File(tmp_filename)).delete();
    }
}
