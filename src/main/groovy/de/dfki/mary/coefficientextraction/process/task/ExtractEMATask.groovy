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
import de.dfki.mary.coefficientextraction.extraction.ExtractEMA

/**
 *  Definition of the task type to convert speech tools (MNGU) EMA formatted files to raw binary EMA files.
 *
 */
public class ExtractEMATask extends DefaultTask {
    /** The worker */
    private final WorkerExecutor workerExecutor;

    /** The list of files to manipulate */
    @InputFile
    final RegularFileProperty list_basenames = newInputFile()

    /** The directory containing speech tools (MNGU) EMA formatted files */
    @InputDirectory
    final DirectoryProperty orig_ema_dir = newInputDirectory()

    /** The directory which will contains the raw binary EMA generated files */
    @OutputDirectory
    final DirectoryProperty ema_dir = newOutputDirectory()

    /**
     *  The constructor which defines which worker executor is going to achieve the conversion job
     *
     *  @param workerExecutor the worker executor
     */
    @Inject
    public ExtractEMATask(WorkerExecutor workerExecutor) {
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
            File orig_ema_file   = new File(orig_ema_dir.getAsFile().get(), basename + ".ema")
            File output_ema_file = new File(ema_dir.getAsFile().get(), orig_ema_file.getName());

            // Submit the execution
            workerExecutor.submit(ExtractEMAWorker.class,
                                  new Action<WorkerConfiguration>() {
                    @Override
                    public void execute(WorkerConfiguration config) {
                        config.setIsolationMode(IsolationMode.NONE);
                        config.params(orig_ema_file, output_ema_file, project.configuration.user_configuration);
                    }
                });
        }
    }
}


/**
 *  Worker class which achieve the EMA conversion
 *
 */
class ExtractEMAWorker implements Runnable {
    /** The input EMA file */
    private final File input_file;

    /** The generated EMA file */
    private final File output_file;

    /** The configuration object */
    private final Object configuration;

    /**
     *  The contructor which initialize the worker
     *
     *  @param input_file the input EMA file
     *  @param output_file the output EMA file
     *  @param configuration the configuration object
     */
    @Inject
    public ExtractEMAWorker(File input_file, File output_file, Object configuration) {
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
        def channel_list = []
        configuration.models.cmp.streams.each { stream ->
            if (stream.kind == "ema") {
                // Check if channels are given
                if ((stream["parameters"]) && (stream.parameters["channel_ids"])) {
                    channel_list = stream.parameters["channel_ids"]
                }

            }
        }

        int[] channels;
        if (channel_list) {
            channels = new int[channel_list.size()]
            channel_list.eachWithIndex{c,i ->
                channels[i] = c.intValue()
            }
        } else {
            channels = [0, 8, 16, 24, 32, 64, 72];
        }

        // Create extractor
        def extractor = new ExtractEMA(channels)

        // Define output directories
        def extToFile = new Hashtable<String, String>()
        extToFile.put("ema".toString(), output_file)
        extractor.setOutputFiles(extToFile)

        // Extraction
        extractor.extract(input_file)
    }
}
