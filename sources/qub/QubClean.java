package qub;

public interface QubClean
{
    static Result<Void> run(Folder folderToClean, CharacterWriteStream outputStream, VerboseCharacterWriteStream verbose)
    {
        PreCondition.assertNotNull(folderToClean, "folderToClean");
        PreCondition.assertNotNull(outputStream, "outputStream");
        PreCondition.assertNotNull(verbose, "verbose");

        return Result.create(() ->
        {
            final IndentedCharacterWriteStream output = new IndentedCharacterWriteStream(outputStream);

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

    static void main(Console console)
    {
        PreCondition.assertNotNull(console, "console");

        final CommandLineParameters parameters = console.createCommandLineParameters();
        final CommandLineParameter<Folder> folderToCleanParameter = parameters.addPositionalFolder("folder", console)
            .setValueName("<folder-to-clean>")
            .setDescription("The folder to clean. Defaults to the current folder.");
        final CommandLineParameterVerbose verboseParameter = parameters.addVerbose(console);
        final CommandLineParameterProfiler profiler = parameters.addProfiler(console, QubClean.class);
        final CommandLineParameterBoolean help = parameters.addHelp();

        if (help.getValue().await())
        {
            parameters.writeHelpLines(console, "qub-clean", "Used to clean build outputs from source code projects.").await();
            console.setExitCode(-1);
        }
        else
        {
            profiler.await();

            final Stopwatch stopwatch = console.getStopwatch();
            stopwatch.start();

            final Folder folderToClean = folderToCleanParameter.getValue().await();
            final CharacterWriteStream output = console.getOutputCharacterWriteStream();
            final VerboseCharacterWriteStream verbose = verboseParameter.getVerboseCharacterWriteStream().await();
            QubClean.run(folderToClean, output, verbose)
                .catchError((Throwable error) -> console.writeLine(error.getMessage()).await())
                .await();

            final Duration compilationDuration = stopwatch.stop().toSeconds();
            console.writeLine("Done (" + compilationDuration.toString("0.0") + ")").await();
        }
    }

    static void main(String[] args)
    {
        Console.run(args, QubClean::main);
    }
}