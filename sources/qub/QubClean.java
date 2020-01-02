package qub;

public interface QubClean
{
    static void main(String[] args)
    {
        Process.run(args, QubClean::getParameters, QubClean::run);
    }

    /**
     * Get the QubCleanParameters object from the provided Console and its command line arguments.
     * @param process The Process to populate the QubCleanParameters object from.
     * @return The QubCleanParameters object or null if a help argument was provided.
     */
    static QubCleanParameters getParameters(Process process)
    {
        PreCondition.assertNotNull(process, "process");

        final CommandLineParameters parameters = process.createCommandLineParameters()
            .setApplicationName("qub-clean")
            .setApplicationDescription("Used to clean build outputs from source code projects.");
        final CommandLineParameter<Folder> folderToCleanParameter = parameters.addPositionalFolder("folder", process)
            .setValueName("<folder-to-clean>")
            .setDescription("The folder to clean. Defaults to the current folder.");
        final CommandLineParameterVerbose verboseParameter = parameters.addVerbose(process);
        final CommandLineParameterProfiler profiler = parameters.addProfiler(process, QubClean.class);
        final CommandLineParameterHelp help = parameters.addHelp();

        QubCleanParameters result = null;
        if (!help.showApplicationHelpLines(process).await())
        {
            profiler.await();

            final Folder folderToClean = folderToCleanParameter.getValue().await();
            final CharacterWriteStream output = process.getOutputCharacterWriteStream();
            final VerboseCharacterWriteStream verbose = verboseParameter.getVerboseCharacterWriteStream().await();
            result = new QubCleanParameters(folderToClean, output)
                .setVerbose(verbose);
        }

        return result;
    }

    static Result<Void> run(QubCleanParameters parameters)
    {
        PreCondition.assertNotNull(parameters, "parameters");

        return Result.create(() ->
        {
            final Folder folderToClean = parameters.getFolderToClean();
            final IndentedCharacterWriteStream output = new IndentedCharacterWriteStream(parameters.getOutput());
            final VerboseCharacterWriteStream verbose = parameters.getVerbose();

            output.writeLine("Cleaning...").await();

            if (!folderToClean.exists().await())
            {
                output.writeLine("The folder " + folderToClean + " doesn't exist.").await();
            }
            else
            {
                int foldersCleaned = 0;
                for (final String folderNameToClean : Iterable.create("outputs", "out", "target", "output", "dist"))
                {
                    final Folder folderToDelete = folderToClean.getFolder(folderNameToClean).await();
                    verbose.writeLine("Checking if " + folderToDelete + " exists...").await();
                    if (!folderToDelete.exists().await())
                    {
                        verbose.writeLine("Doesn't exist.").await();
                    }
                    else
                    {
                        ++foldersCleaned;
                        output.write("Deleting folder " + folderToDelete + "...").await();
                        folderToDelete.delete()
                            .then(() ->
                            {
                                output.writeLine(" Done.").await();
                            })
                            .catchError((Throwable error) ->
                            {
                                output.writeLine(" Failed.").await();
                                output.indent(() -> output.writeLine(error.getMessage()).await());
                            })
                            .await();
                    }
                }

                if (foldersCleaned == 0)
                {
                    output.writeLine("Found no folders to delete.").await();
                }
            }
        });
    }
}