package qub;

/**
 * Parameters that can be passed to the qub-clean application.
 */
public class QubCleanRunParameters
{
    private final Folder folderToClean;
    private final CharacterToByteWriteStream output;
    private final Folder dataFolder;

    private VerboseCharacterToByteWriteStream verbose;

    /**
     * Create a new QubCleanParameters object.
     * @param folderToClean The folder to clean.
     * @param output The output stream that messages will be written to.
     */
    private QubCleanRunParameters(Folder folderToClean, CharacterToByteWriteStream output, Folder dataFolder)
    {
        PreCondition.assertNotNull(folderToClean, "folderToClean");
        PreCondition.assertNotNull(output, "output");
        PreCondition.assertNotNull(dataFolder, "dataFolder");

        this.folderToClean = folderToClean;
        this.output = output;
        this.dataFolder = dataFolder;

        this.verbose = VerboseCharacterToByteWriteStream.create(output).setIsVerbose(false);
    }

    public static QubCleanRunParameters create(Folder folderToClean, CharacterToByteWriteStream output, Folder dataFolder)
    {
        return new QubCleanRunParameters(folderToClean, output, dataFolder);
    }

    /**
     * Get the folder to clean.
     * @return The folder to clean.
     */
    public Folder getFolderToClean()
    {
        return this.folderToClean;
    }

    /**
     * Get the output stream where messages will be written.
     * @return The output stream where messages will be written.
     */
    public CharacterToByteWriteStream getOutput()
    {
        return this.output;
    }

    public Folder getDataFolder()
    {
        return this.dataFolder;
    }

    /**
     * Get the stream that will be used to write verbose messages.
     * @return The stream that will be used to write verbose messages.
     */
    public VerboseCharacterToByteWriteStream getVerbose()
    {
        return this.verbose;
    }

    /**
     * Set the stream that will be used to write verbose messages.
     * @param verbose The stream that will be used to write verbose messages.
     * @return This object for method chaining.
     */
    public QubCleanRunParameters setVerbose(VerboseCharacterToByteWriteStream verbose)
    {
        PreCondition.assertNotNull(verbose, "verbose");

        this.verbose = verbose;

        return this;
    }
}
