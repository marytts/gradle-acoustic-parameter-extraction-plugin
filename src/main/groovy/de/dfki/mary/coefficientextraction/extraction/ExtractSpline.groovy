package de.dfki.mary.coefficientextraction.extraction;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.util.Hashtable;

import groovy.text.SimpleTemplateEngine;

/**
 * Coefficients extraction based on STRAIGHT
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class ExtractSpline extends ExtractBase
{
    private String template = '''
% Adding path
addpath(\'${PROJDIR}/src/matlab/\')
addpath(\'${PROJDIR}/src/matlab/splines\')


% Loading interpolated f0
f = fopen(\'${F0_FILENAME}\', \'rb\');
if (f < 3)
    error(\'cannot open ${F0_FILENAME}\');
end
f0 = fread(f, inf, \'float\');
fclose(f);


% Levels definition - FIXME: Syllable for the moment
levels_definition = {};
levels_definition{1}.frameshift = ${FRAMESHIFT_SYL}; 
levels_definition{1}.segments = load_segments(\'${LAB_FILENAME_SYL}\', ${FRAMESHIFT});

% Compute params
params = compute_level_spline(f0, levels_definition, 0);

%%  Save params
f = fopen(\'${PARAMS_FILENAME_SYL}\', \'wb\');
if (f < 3)
    error(\'cannot open ${PARAMS_FILENAME_SYL}\');
end
for i=1:size(params, 2)
    fwrite(f, params{1, i}, \'float\'); % FIXME: syl => first level !
end
fclose(f);
'''
    def frameshift;
    def frameshift_syl;
    
    public ExtractSpline()
    {
        frameshift = 5000
        frameshift_syl = 3
    }

    public ExtractSpline(int frameshift, int frameshift_syl)
    {
        this.frameshift = frameshift;
        this.frameshift_syl = frameshift_syl;
    }

    public void extract(File input_file) throws Exception
    {
        // Extract logF0 if wanted
        String[] tokens = input_file.getName().split("\\.(?=[^\\.]+\$)");
        String output_file_name = extToDir.get("spline") + "/" + tokens[0] + ".spline";

        // Generate script file
        File script_temp = File.createTempFile("script-spline", ".m");
        def simple = new SimpleTemplateEngine()
        def bindings = [
            "FRAMESHIFT_SYL": frameshift_syl,
            "FRAMESHIFT" : frameshift,
            "LAB_FILENAME_SYL":extToDir.get("lab") + "/" + tokens[0] + ".lab",
            "PARAMS_FILENAME_SYL": output_file_name,
            "F0_FILENAME": input_file.getName(),
            "PROJDIR": extToDir.get("projdir")
        ]
        def output = simple.createTemplate(template).make(bindings).toString()
        script_temp.write(output)

        // Extract

        String command = "matlab -nojvm -nodisplay -nosplash < " + script_temp.getAbsolutePath();
        String[] cmd = ["bash", "-c", command];
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
        
        // Delete useless script file
        script_temp.delete();
    }
}
