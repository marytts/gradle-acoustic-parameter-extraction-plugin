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
import de.dfki.mary.coefficientextraction.extraction.ExtractInterpolatedF0

/**
 *  Definition of the task type to convert f0 to log f0
 *
 */
public class ExtractInterpolatedF0Task extends DefaultTask {
    /** The worker */
    private final WorkerExecutor workerExecutor;

    /** The list of files to manipulate */
    @InputFile
    final RegularFileProperty list_basenames = newInputFile()

    /** The directory containing f0 files */
    @InputDirectory
    final DirectoryProperty lf0_dir = newInputDirectory()

    /** The directory containing the log f0 files */
    @OutputDirectory
    final DirectoryProperty interpolated_lf0_dir = newOutputDirectory()

    /**
     *  The constructor which defines which worker executor is going to achieve the conversion job
     *
     *  @param workerExecutor the worker executor
     */
    @Inject
    public ExtractInterpolatedF0Task(WorkerExecutor workerExecutor) {
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
            File lf0_file  = new File(lf0_dir.getAsFile().get(), basename + ".lf0");
            File interpolated_lf0_file = new File(interpolated_lf0_dir.getAsFile().get(), basename + ".lf0");

            // Submit the execution
            workerExecutor.submit(ExtractInterpolatedF0Worker.class,
                                  new Action<WorkerConfiguration>() {
                    @Override
                    public void execute(WorkerConfiguration config) {
                        config.setIsolationMode(IsolationMode.NONE);
                        config.params(lf0_file, interpolated_lf0_file, project.configuration.user_configuration);
                    }
                });
        }
    }
}


/**
 *  Worker class which achieve the f0 conversion to log f0
 *
 */
class ExtractInterpolatedF0Worker implements Runnable {
    /** The input F0 file */
    private final File input_file;

    /** The generated INTERPOLATED_LF0 file */
    private final File output_file;

    /** The configuration object */
    private final Object configuration;

    /**
     *  The contructor which initialize the worker
     *
     *  @param input_file the input F0 file
     *  @param output_file the output INTERPOLATED_LF0 file
     *  @param configuration the configuration object
     */
    @Inject
    public ExtractInterpolatedF0Worker(File input_file, File output_file, Object configuration) {
	this.input_file = input_file;
	this.output_file = output_file;
        this.configuration = configuration
    }

    /**
     *  Run method which achieve the extraction/conversion
     *
     */
    @Override
    public void run() {

        // Define extractor
        def extractor = new ExtractInterpolatedF0();

        // Define output files
        def extToFile = new Hashtable<String, String>()
        extToFile.put("interpolated_lf0".toString(), output_file)
        extractor.setOutputFiles(extToFile)

        // Run extraction
        extractor.extract(input_file)
    }
}
