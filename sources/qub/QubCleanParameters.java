package qub;

/**
 * Parameters that can be passed to the qub-clean application.
 */
public class QubCleanParameters
{
    private final Folder folderToClean;
    private final CharacterWriteStream output;

    private VerboseCharacterWriteStream verbose;

    /**
     * Create a new QubCleanParameters object.
     * @param folderToClean The folder to clean.
     * @param output The output stream that messages will be written to.
     */
    public QubCleanParameters(Folder folderToClean, CharacterWriteStream output)
    {
        PreCondition.assertNotNull(folderToClean, "folderToClean");
        PreCondition.assertNotNull(output, "output");

        this.folderToClean = folderToClean;
        this.output = output;

        this.verbose = new VerboseCharacterWriteStream(false, output);
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
    public CharacterWriteStream getOutput()
    {
        return this.output;
    }

    /**
     * Get the stream that will be used to write verbose messages.
     * @return The stream that will be used to write verbose messages.
     */
    public VerboseCharacterWriteStream getVerbose()
    {
        return this.verbose;
    }

    /**
     * Set the stream that will be used to write verbose messages.
     * @param verbose The stream that will be used to write verbose messages.
     * @return This object for method chaining.
     */
    public QubCleanParameters setVerbose(VerboseCharacterWriteStream verbose)
    {
        PreCondition.assertNotNull(verbose, "verbose");

        this.verbose = verbose;

        return this;
    }
}
