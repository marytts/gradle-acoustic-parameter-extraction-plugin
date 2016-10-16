package de.dfki.mary.coefficientextraction.extraction;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.util.Hashtable;

/**
 * Coefficients extraction based on STRAIGHT
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class ExtractLF0 extends ExtractBase
{
    /**
        */
    private static final String template_string =
       "minf0 = log(%f);" + System.getProperty("line.separator") + // % Minimal f0 value
       "%%  Loading" + System.getProperty("line.separator") +
       "f = fopen('%s', 'rb');" + System.getProperty("line.separator") + // Input f0 filepath
       "if (f<3)" + System.getProperty("line.separator") +
       "    error('cannot open %s');" + System.getProperty("line.separator") +  // Input f0 filepath
       "end" + System.getProperty("line.separator") +
       "f0 = fread(f, inf, 'float');" + System.getProperty("line.separator") +
       "fclose(f);" + System.getProperty("line.separator") +
       "" + System.getProperty("line.separator") +

       "" + System.getProperty("line.separator") +
       "%%" + System.getProperty("line.separator") +
       "%% Linear F0 Interpolation" + System.getProperty("line.separator") +
       "%%" + System.getProperty("line.separator") +
       "    f0A = f0;" + System.getProperty("line.separator") +
       "    xf0A = 1:length(f0A);" + System.getProperty("line.separator") +
       "    I = find(f0A>minf0);" + System.getProperty("line.separator") +
       "" + System.getProperty("line.separator") +
       "    if(~isempty(I))" + System.getProperty("line.separator") +
       "        I = find(f0A>minf0);" + System.getProperty("line.separator") +
       "        f0A = f0(I);" + System.getProperty("line.separator") +
       "        f0Ai = interp1(I, f0A, 1:length(f0));" + System.getProperty("line.separator") +
       "" + System.getProperty("line.separator") +
       "        f0Ai(1:I(1)) = f0(I(1)).*ones(I(1), 1);" + System.getProperty("line.separator") +
       "        f0Ai(I(size(I, 1))+1:size(f0,1)) = f0(I(size(I, 1))).*ones(size(f0,1)-I(size(I, 1)), 1);" + System.getProperty("line.separator") +
       "" + System.getProperty("line.separator") +
       "    else" + System.getProperty("line.separator") +
       "        f0Ai = f0;" + System.getProperty("line.separator") +
       "    end" + System.getProperty("line.separator") +
       "" + System.getProperty("line.separator") +
       "%% Saving" + System.getProperty("line.separator") +
        "f = fopen('%s', 'wb');" + System.getProperty("line.separator") + // Output f0 filepath
       "if (f < 3)" + System.getProperty("line.separator") +
       "    error('cannot open %s');" + System.getProperty("line.separator") + // Output f0 filepath
       "end" + System.getProperty("line.separator") +
       "fwrite(f, f0Ai, 'float');" + System.getProperty("line.separator") +
       "fclose(f);" + System.getProperty("line.separator");

    private boolean is_interpolated;
    private float min_f0;

    public ExtractLF0()
    {
        is_interpolated = false;
        this.min_f0 = 0;
    }

    public ExtractLF0(boolean is_interpolated, float min_f0)
    {
        this.is_interpolated = is_interpolated;
        this.min_f0 = min_f0;
    }

    private void interpolateF0(String input_file_name, String output_file_name)
        throws Exception
    {

        File script_temp = File.createTempFile("interp_f0", ".m");
        PrintWriter writer = new PrintWriter(script_temp.getAbsolutePath(), "UTF-8");
        writer.println(String.format(template_string, min_f0,
                                     input_file_name, input_file_name,
                                     output_file_name, output_file_name));
        writer.close();


        String command = "matlab -nojvm -nodisplay -nosplash < " + script_temp.getAbsolutePath();
        String[] cmd = {"bash", "-c", command};
        Process p = Runtime.getRuntime().exec(cmd);
        p.waitFor();

        BufferedReader reader =
            new BufferedReader(new InputStreamReader(p.getInputStream()));

        String line = "";
        // while ((line = reader.readLine())!= null) {
        //         System.out.println(line);
        // }

        // Error stream => throw exception if not empty
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
        script_temp.delete();
    }

    private void generateLogF0(String input_file_name, String output_file_name)
        throws Exception
    {

        // 2. extraction
        String command = "cat " + input_file_name + " | ";
        command += "sopr -magic 0.0 -LN -MAGIC -1.0E+10 > " + output_file_name;

        String[] cmd = {"bash", "-c", command};
        Process p = Runtime.getRuntime().exec(cmd);
        p.waitFor();


        BufferedReader reader =
            new BufferedReader(new InputStreamReader(p.getInputStream()));

        String line = "";
        // while ((line = reader.readLine())!= null) {
        //         System.out.println(line);
        // }


        // Error stream => throw exception if not empty
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

    public void extract(String input_file_name) throws Exception
    {
        // Extract logF0 if wanted
        String[] tokens = (new File(input_file_name)).getName().split("\\.(?=[^\\.]+$)");
        String output_file_name = extToDir.get("lf0") + "/" + tokens[0] + ".lf0";
        if (is_interpolated)
        {
            File temp = File.createTempFile("temp-inter", ".f0");
            generateLogF0(input_file_name, temp.getAbsolutePath());
            interpolateF0(temp.getAbsolutePath(), output_file_name);

            temp.delete();
        }
        else
        {
            generateLogF0(input_file_name, output_file_name);
        }
    }
}
