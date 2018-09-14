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
import de.dfki.mary.coefficientextraction.extraction.ExtractBAP

/**
 *  Definition of the task type to convert aperiodicity to aperiodicity per band
 *
 */
public class ExtractBAPTask extends DefaultTask {
    /** The worker */
    private final WorkerExecutor workerExecutor;

    /** The list of files to manipulate */
    @InputFile
    final RegularFileProperty list_basenames = newInputFile()

    /** The directory containing aperiodicity files */
    @InputDirectory
    final DirectoryProperty ap_dir = newInputDirectory()

    /** The directory containing the aperiodicity per band files */
    @OutputDirectory
    final DirectoryProperty bap_dir = newOutputDirectory()

    /**
     *  The constructor which defines which worker executor is going to achieve the conversion job
     *
     *  @param workerExecutor the worker executor
     */
    @Inject
    public ExtractBAPTask(WorkerExecutor workerExecutor) {
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
            File ap_file  = new File(ap_dir.getAsFile().get(), basename + ".ap")
            File bap_file = new File(bap_dir.getAsFile().get(), basename + ".bap")

            // Submit the execution
            workerExecutor.submit(ExtractBAPWorker.class,
                                  new Action<WorkerConfiguration>() {
                    @Override
                    public void execute(WorkerConfiguration config) {
                        config.setIsolationMode(IsolationMode.NONE);
                        config.params(ap_file, bap_file, project.configuration.user_configuration);
                    }
                });
        }
    }
}


/**
 *  Worker class which achieve the aperiodicity conversion to aperiodicity per band
 *
 */
class ExtractBAPWorker implements Runnable {
    /** The input AP file */
    private final File input_file;

    /** The generated BAP file */
    private final File output_file;

    /** The configuration object */
    private final Object configuration;

    /**
     *  The contructor which initialize the worker
     *
     *  @param input_file the input AP file
     *  @param output_file the output BAP file
     *  @param configuration the configuration object
     */
    @Inject
    public ExtractBAPWorker(File input_file, File output_file, Object configuration) {
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
        def extractor = new ExtractBAP()

        // Prepare extractor configuration
        configuration.models.cmp.streams.each { stream ->
            if (stream.kind ==  "bap") {
                if (stream.order){
                    extractor.setOrder(stream.order.shortValue())
                }
            }
        }
        extractor.setSampleRate(configuration.signal.samplerate)


        // Define output files
        def extToFile = new Hashtable<String, String>()
        extToFile.put("bap".toString(), output_file)
        extractor.setOutputFiles(extToFile)


        // Run extraction
        extractor.extract(input_file)
    }
}
