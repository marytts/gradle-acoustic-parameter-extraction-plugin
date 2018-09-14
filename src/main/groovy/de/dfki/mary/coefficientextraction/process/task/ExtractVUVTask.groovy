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
import de.dfki.mary.coefficientextraction.extraction.ExtractVUV

/**
 *  Definition of the task type to convert lf0 to log lf0
 *
 */
public class ExtractVUVTask extends DefaultTask {
    /** The worker */
    private final WorkerExecutor workerExecutor;

    /** The list of files to manipulate */
    @InputFile
    final RegularFileProperty list_basenames = newInputFile()

    /** The directory containing lf0 files */
    @InputDirectory
    final DirectoryProperty lf0_dir = newInputDirectory()

    /** The directory containing the log lf0 files */
    @OutputDirectory
    final DirectoryProperty vuv_dir = newOutputDirectory()

    /**
     *  The constructor which defines which worker executor is going to achieve the conversion job
     *
     *  @param workerExecutor the worker executor
     */
    @Inject
    public ExtractVUVTask(WorkerExecutor workerExecutor) {
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
            File vuv_file = new File(vuv_dir.getAsFile().get(), basename + ".vuv");

            // Submit the execution
            workerExecutor.submit(ExtractVUVWorker.class,
                                  new Action<WorkerConfiguration>() {
                    @Override
                    public void execute(WorkerConfiguration config) {
                        config.setIsolationMode(IsolationMode.NONE);
                        config.params(lf0_file, vuv_file, project.configuration.user_configuration);
                    }
                });
        }
    }
}


/**
 *  Worker class which achieve the lf0 conversion to log lf0
 *
 */
class ExtractVUVWorker implements Runnable {
    /** The input LF0 file */
    private final File input_file;

    /** The generated VUV file */
    private final File output_file;

    /** The configuration object */
    private final Object configuration;

    /**
     *  The contructor which initialize the worker
     *
     *  @param input_file the input LF0 file
     *  @param output_file the output VUV file
     *  @param configuration the configuration object
     */
    @Inject
    public ExtractVUVWorker(File input_file, File output_file, Object configuration) {
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
        def extractor = new ExtractVUV();

        // Define output files
        def extToFile = new Hashtable<String, String>()
        extToFile.put("vuv".toString(), output_file)
        extractor.setOutputFiles(extToFile)

        // Run extraction
        extractor.extract(input_file)
    }
}
