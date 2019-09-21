package qub;

public class QubClean
{
    private final Folder folderToClean;
    private final IndentedCharacterWriteStream output;
    private final VerboseCharacterWriteStream verbose;

    public QubClean(Folder folderToClean, CharacterWriteStream output, VerboseCharacterWriteStream verbose)
    {
        PreCondition.assertNotNull(folderToClean, "folderToClean");
        PreCondition.assertNotNull(output, "output");
        PreCondition.assertNotNull(verbose, "verbose");

        this.folderToClean = folderToClean;
        this.output = new IndentedCharacterWriteStream(output);
        this.verbose = verbose;
    }

    public Result<Void> run()
    {
        return Result.create(() ->
        {
            output.writeLine("Cleaning...").await();

            if (!this.folderToClean.exists().await())
            {
                output.writeLine("The folder " + this.folderToClean + " doesn't exist.").await();
            }
            else
            {
                int foldersCleaned = 0;
                for (final String folderNameToClean : Iterable.create("outputs", "out", "target", "output", "dist"))
                {
                    final Folder folderToDelete = this.folderToClean.getFolder(folderNameToClean).await();
                    this.verbose.writeLine("Checking if " + folderToDelete + " exists...").await();
                    if (!folderToDelete.exists().await())
                    {
                        this.verbose.writeLine("Doesn't exist.").await();
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

    public static void main(Console console)
    {
        PreCondition.assertNotNull(console, "console");

        final CommandLineParameters parameters = console.createCommandLineParameters();
        final CommandLineParameter<Folder> folderToCleanParameter = parameters.addPositionalFolder("folder", console)
            .setValueName("<folder-to-clean>")
            .setDescription("The folder to clean. Defaults to the current folder.");
        final CommandLineParameterVerbose verbose = parameters.addVerbose(console);
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

            final QubClean qubClean = new QubClean(
                folderToCleanParameter.getValue().await(),
                console.getOutputCharacterWriteStream(),
                verbose.getVerboseCharacterWriteStream().await());
            qubClean.run()
                .catchError((Throwable error) -> console.writeLine(error.getMessage()).await())
                .await();

            final Duration compilationDuration = stopwatch.stop().toSeconds();
            console.writeLine("Done (" + compilationDuration.toString("0.0") + ")").await();
        }
    }

    public static void main(String[] args)
    {
        Console.run(args, QubClean::main);
    }
}