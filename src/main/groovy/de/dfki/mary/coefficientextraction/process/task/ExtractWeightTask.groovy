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
import de.dfki.mary.coefficientextraction.extraction.ExtractWeight

/**
 *  Definition of the task type to convert speech tools (MNGU) EMA formatted files to raw binary EMA files.
 *
 */
public class ExtractWeightTask extends DefaultTask {
    /** The worker */
    private final WorkerExecutor workerExecutor;

    /** The list of files to manipulate */
    @InputFile
    final RegularFileProperty list_basenames = newInputFile()

    /** The directory containing speech tools (MNGU) EMA formatted files */
    @InputDirectory
    final DirectoryProperty weight_js_dir = newInputDirectory()

    /** The directory which will contains the raw binary EMA generated files */
    @OutputDirectory
    final DirectoryProperty weight_dir = newOutputDirectory()

    /**
     *  The constructor which defines which worker executor is going to achieve the conversion job
     *
     *  @param workerExecutor the worker executor
     */
    @Inject
    public ExtractWeightTask(WorkerExecutor workerExecutor) {
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
            File weight_js_file   = new File(weight_js_dir.getAsFile().get(), basename + ".json")
            File weight_file = new File(weight_dir.getAsFile().get(), basename + ".weight");

            // Submit the execution
            workerExecutor.submit(ExtractWeightWorker.class,
                                  new Action<WorkerConfiguration>() {
                    @Override
                    public void execute(WorkerConfiguration config) {
                        config.setIsolationMode(IsolationMode.NONE);
                        config.params(weight_js_file, weight_file, project.configuration.user_configuration);
                    }
                });
        }
    }
}


/**
 *  Worker class which achieve the EMA conversion
 *
 */
class ExtractWeightWorker implements Runnable {
    /** The input weight JSON file */
    private final File input_file;

    /** The generated weight file */
    private final File output_file;

    /** The configuration object */
    private final Object configuration;

    /**
     *  The contructor which initialize the worker
     *
     *  @param input_file the input weight JSON file
     *  @param output_file the output weight file
     *  @param configuration the configuration object
     */
    @Inject
    public ExtractWeightWorker(File input_file, File output_file, Object configuration) {
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

        // Get extractor parameters
        def order = 12
        configuration.models.cmp.streams.each { stream ->
            if (stream.kind == "weight") {
                order = stream.order
            }
        }
        // Create extractor
        def extractor = new ExtractWeight(order)

        // Define output directories
        def extToFile = new Hashtable<String, String>()
        extToFile.put("weight".toString(), output_file)
        extractor.setOutputFiles(extToFile)

        // Extraction
        extractor.extract(input_file)
    }
}
