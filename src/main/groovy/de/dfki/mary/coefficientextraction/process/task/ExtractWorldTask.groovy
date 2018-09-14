package de.dfki.mary.coefficientextraction.process.task

// Inject
import javax.inject.Inject;

// Worker import
import org.gradle.workers.*;

// Gradle task related
import org.gradle.api.Action;
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

// Extraction helper class
import de.dfki.mary.coefficientextraction.extraction.ExtractWorld

/**
 *  Definition of the task type to extract spectrum, f0 and aperiodicity using world vocoder
 *
 */
public class ExtractWorldTask extends DefaultTask {
    /** The worker */
    private final WorkerExecutor workerExecutor;

    /** The list of files to manipulate */
    @InputFile
    final RegularFileProperty list_basenames = newInputFile()

    /** The directory containing wav files */
    @InputDirectory
    final DirectoryProperty wav_dir = newInputDirectory()

    /** The directory containing the spectrum files */
    @OutputDirectory
    final DirectoryProperty sp_dir = newOutputDirectory()

    /** The directory containing the F0 files */
    @OutputDirectory
    final DirectoryProperty f0_dir = newOutputDirectory()

    /** The directory containing the aperiodicity files */
    @OutputDirectory
    final DirectoryProperty ap_dir = newOutputDirectory()

    /**
     *  The constructor which defines which worker executor is going to achieve the conversion job
     *
     *  @param workerExecutor the worker executor
     */
    @Inject
    public ExtractWorldTask(WorkerExecutor workerExecutor) {
        super();
        this.workerExecutor = workerExecutor;
    }

    /**
     *  The actual extraction method
     *
     */
    @TaskAction
    public void extract() {
        for (String basename: list_basenames.getAsFile().get().readLines()) {
            // FIXME: hardcoded extension
            File wav_file   = new File(wav_dir.getAsFile().get(), basename + ".wav");
            File sp_file = new File(sp_dir.getAsFile().get(), basename + ".sp");
            File f0_file = new File(f0_dir.getAsFile().get(), basename + ".f0");
            File ap_file = new File(ap_dir.getAsFile().get(), basename + ".ap");

            // Submit the execution
            workerExecutor.submit(ExtractWorldWorker.class,
                                  new Action<WorkerConfiguration>() {
                    @Override
                    public void execute(WorkerConfiguration config) {
                        config.setIsolationMode(IsolationMode.NONE);
                        config.params(wav_file, sp_file, f0_file, ap_file,
                                      project.configuration.user_configuration);
                    }
                });
        }
    }
}


/**
 *  Worker class which extract spectrum, f0 and aperiodicity using the vocoder World
 *
 */
class ExtractWorldWorker implements Runnable {
    /** The input SP file */
    private final File input_file;

    /** The generated SP file */
    private final File sp_output_file;

    /** The generated F0 file */
    private final File f0_output_file;

    /** The generated AP file */
    private final File ap_output_file;

    /** The configuration object */
    private final Object configuration;

    /**
     *  The contructor which initialize the worker
     *
     *  @param input_file the input wav file
     *  @param sp_output_file the output SP file
     *  @param f0_output_file the output F0 file
     *  @param ap_output_file the output AP file
     *  @param configuration the configuration object
     */
    @Inject
    public ExtractWorldWorker(File input_file, File sp_output_file, File f0_output_file, File ap_output_file,
                            Object configuration) {
	this.input_file = input_file;
	this.sp_output_file = sp_output_file;
	this.f0_output_file = f0_output_file;
	this.ap_output_file = ap_output_file;
        this.configuration = configuration
    }

    /**
     *  Run method which achieve the extraction/conversion
     *
     */
    @Override
    public void run() {

        // Define extractor
        def extractor = new ExtractWorld();

        // Prepare extractor configuration
        extractor.setFrameshift(configuration.signal.frameshift)


        // Define output files
        def extToFile = new Hashtable<String, String>()
        extToFile.put("sp".toString(), sp_output_file)
        extToFile.put("f0".toString(), f0_output_file)
        extToFile.put("ap".toString(), ap_output_file)
        extractor.setOutputFiles(extToFile)


        // Run extraction
        extractor.extract(input_file)
    }
}
