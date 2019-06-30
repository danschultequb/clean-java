package qub;

public class QubClean
{
    public static void main(Console console)
    {
        PreCondition.assertNotNull(console, "console");

        final CommandLineParameters parameters = console.createCommandLineParameters();
        final CommandLineParameter<Folder> folderToCleanParameter = parameters.addPositionalFolder("folder", console)
            .setValueName("<folder-to-clean>")
            .setDescription("The folder to clean. Defaults to the current folder.");
        final CommandLineParameterVerbose verbose = parameters.addVerbose(console);
        final CommandLineParameterBoolean help = parameters.addHelp();

        if (help.getValue().await())
        {
            parameters.writeHelpLines(console, "qub-clean", "Used to clean build outputs from source code projects.").await();
            console.setExitCode(-1);
        }
        else
        {
            final Stopwatch stopwatch = console.getStopwatch();
            stopwatch.start();

            try
            {
                console.writeLine("Cleaning...").await();

                final Folder folderToClean = folderToCleanParameter.getValue().await();
                if (!folderToClean.exists().await())
                {
                    console.writeLine("The folder " + folderToClean + " doesn't exist.").await();
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
                            console.write("Deleting folder " + folderToDelete + "...").await();
                            try
                            {
                                folderToDelete.delete().await();
                                console.writeLine(" Done.").await();
                            }
                            catch (Throwable error)
                            {
                                console.writeLine(" Failed.").await();
                                console.writeLine(error.getMessage()).await();
                            }
                        }
                    }

                    if (foldersCleaned == 0)
                    {
                        console.writeLine("Found no folders to delete.").await();
                    }
                }
            }
            catch (Throwable error)
            {
                console.writeLine(error.getMessage()).await();
            }

            final Duration compilationDuration = stopwatch.stop().toSeconds();
            console.writeLine("Done (" + compilationDuration.toString("0.0") + ")").await();
        }
    }

    public static void main(String[] args)
    {
        Console.run(args, QubClean::main);
    }
}