package qub;

public interface QubClean
{
    static void main(String[] args)
    {
        Console.run(args, QubClean::main);
    }

    static void main(Console console)
    {
        PreCondition.assertNotNull(console, "console");

        final CommandLineParameters parameters = console.createCommandLineParameters()
            .setApplicationName("qub-clean")
            .setApplicationDescription("Used to clean build outputs from source code projects.");
        final CommandLineParameter<Folder> folderToCleanParameter = parameters.addPositionalFolder("folder", console)
            .setValueName("<folder-to-clean>")
            .setDescription("The folder to clean. Defaults to the current folder.");
        final CommandLineParameterVerbose verboseParameter = parameters.addVerbose(console);
        final CommandLineParameterProfiler profiler = parameters.addProfiler(console, QubClean.class);
        final CommandLineParameterHelp help = parameters.addHelp();

        if (!help.showApplicationHelpLines(console).await())
        {
            profiler.await();

            console.showDuration(() ->
            {
                final QubCleanParameters qubCleanParameters = new QubCleanParameters(
                    folderToCleanParameter.getValue().await(),
                    console.getOutputCharacterWriteStream())
                    .setVerbose(verboseParameter.getVerboseCharacterWriteStream().await());
                QubClean.run(qubCleanParameters)
                    .catchError((Throwable error) -> console.writeLine(error.getMessage()).await())
                    .await();
            });
        }
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