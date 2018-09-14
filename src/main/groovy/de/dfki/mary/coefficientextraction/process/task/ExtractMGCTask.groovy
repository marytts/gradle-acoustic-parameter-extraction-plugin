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
import de.dfki.mary.coefficientextraction.extraction.ExtractMGC

/**
 *  Definition of the task type to convert spectrum to MGC
 *
 */
public class ExtractMGCTask extends DefaultTask {
    /** The worker */
    private final WorkerExecutor workerExecutor;

    /** The list of files to manipulate */
    @InputFile
    final RegularFileProperty list_basenames = newInputFile()

    /** The directory containing sp files */
    @InputDirectory
    final DirectoryProperty sp_dir = newInputDirectory()

    /** The directory containing the MGC files */
    @OutputDirectory
    final DirectoryProperty mgc_dir = newOutputDirectory()

    /**
     *  The constructor which defines which worker executor is going to achieve the conversion job
     *
     *  @param workerExecutor the worker executor
     */
    @Inject
    public ExtractMGCTask(WorkerExecutor workerExecutor) {
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
            File sp_file  = new File(sp_dir.getAsFile().get(), basename + ".sp");
            File mgc_file = new File(mgc_dir.getAsFile().get(), basename + ".mgc");

            // Submit the execution
            workerExecutor.submit(ExtractMGCWorker.class,
                                  new Action<WorkerConfiguration>() {
                    @Override
                    public void execute(WorkerConfiguration config) {
                        config.setIsolationMode(IsolationMode.NONE);
                        config.params(sp_file, mgc_file, project.configuration.user_configuration);
                    }
                });
        }
    }
}


/**
 *  Worker class which achieve the spectrum conversion to MGC
 *
 */
class ExtractMGCWorker implements Runnable {
    /** The input SP file */
    private final File input_file;

    /** The generated MGC file */
    private final File output_file;

    /** The configuration object */
    private final Object configuration;

    /**
     *  The contructor which initialize the worker
     *
     *  @param input_file the input SP file
     *  @param output_file the output MGC file
     *  @param configuration the configuration object
     */
    @Inject
    public ExtractMGCWorker(File input_file, File output_file, Object configuration) {
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
        def extractor = new ExtractMGC();

        // Prepare extractor configuration
        configuration.models.cmp.streams.each { stream ->
            if (stream.kind ==  "mgc") {
                if (stream.order){
                    extractor.setOrder(stream.order.shortValue())
                }
                if (stream.gamma){
                    extractor.setGamma(stream.parameters.gamma)
                }
                if (stream.use_lngain){
                    extractor.set(stream.parameters.use_lngain)
                }
            }
        }
        extractor.setSampleRate(configuration.signal.samplerate)


        // Define output files
        def extToFile = new Hashtable<String, String>()
        extToFile.put("mgc".toString(), output_file)
        extractor.setOutputFiles(extToFile)


        // Run extraction
        extractor.extract(input_file)
    }
}
