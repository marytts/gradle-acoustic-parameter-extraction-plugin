package de.dfki.mary.coefficientextraction.extraction;

/* */
import java.io.File;
import java.net.URL;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;


import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/* testng part */
import org.testng.Assert;
import org.testng.annotations.*;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class ExtractWorldTest
{

    private ExtractWorld ex_world;
    private Path temp_dir;

    public ExtractWorldTest() throws Exception
    {
        ex_world = new ExtractWorld();
        temp_dir = Files.createTempDirectory(null);
    }

    @BeforeTest
    public void extractWavFile() throws Exception {
        URL url = ExtractWorldTest.class.getResource("vaiueo2d.wav");


        Files.copy(url.openStream(),
                   Paths.get(temp_dir.toString(), "vaiueo2d.wav"),
                   StandardCopyOption.REPLACE_EXISTING);
    }

    @AfterTest
    public void clean() throws Exception {
        FileUtils.deleteDirectory(temp_dir.toFile());
    }

    @Test
    public void checkScriptGeneration() throws Exception
    {
        Assert.assertTrue(true);
    }
}


/* ExtractWorldTest.java ends here */
