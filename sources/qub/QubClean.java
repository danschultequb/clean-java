package qub;

public class QubClean
{
    public void main(Console console)
    {
        PreCondition.assertNotNull(console, "console");

        if (shouldShowUsage(console))
        {
            console.writeLine("Usage: qub-clean [[-folder=]<folder-path-to-clean>]").await();
            console.writeLine("  Used to clean build outputs from source code projects.").await();
            console.writeLine("  -folder: The folder to clean. This can be specified either with the -folder").await();
            console.writeLine("           argument name or without it.").await();
        }
        else
        {
            final Stopwatch stopwatch = console.getStopwatch();
            stopwatch.start();

            try
            {
                console.writeLine("Cleaning...").await();

                final Function1<String,Result<?>> verboseLog = getVerboseLog(console);

                final Folder folderToClean = getFolderToClean(console);
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
                        verboseLog.run("Checking if " + folderToDelete + " exists...").await();
                        if (!folderToDelete.exists().await())
                        {
                            verboseLog.run("Doesn't exist.").await();
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

    private static boolean shouldShowUsage(Console console)
    {
        PreCondition.assertNotNull(console, "console");

        return console.getCommandLine().contains(
            (CommandLineArgument argument) ->
            {
                final String argumentString = argument.toString();
                return argumentString.equals("/?") || argumentString.equals("-?");
            });
    }

    private static Path getFolderPathToClean(Console console)
    {
        PreCondition.assertNotNull(console, "console");

        Path result = null;
        final CommandLine commandLine = console.getCommandLine();
        if (commandLine.any())
        {
            CommandLineArgument folderArgument = commandLine.get("folder");
            if (folderArgument == null)
            {
                folderArgument = commandLine.getArguments()
                    .first((CommandLineArgument argument) -> argument.getName() == null);
            }
            if (folderArgument != null)
            {
                result = Path.parse(folderArgument.getValue());
            }
        }

        if (result == null)
        {
            result = console.getCurrentFolderPath();
        }

        if (!result.isRooted())
        {
            result = console.getCurrentFolderPath().resolve(result).await();
        }

        PostCondition.assertNotNull(result, "result");
        PostCondition.assertTrue(result.isRooted(), "result.isRooted()");

        return result;
    }

    private static Folder getFolderToClean(Console console)
    {
        PreCondition.assertNotNull(console, "console");

        final Folder result = console.getFileSystem().getFolder(getFolderPathToClean(console)).await();

        PostCondition.assertNotNull(result, "result");

        return result;
    }

    private static Function1<String,Result<?>> getVerboseLog(Console console)
    {
        PreCondition.assertNotNull(console, "console");

        Function1<String,Result<?>> result = (String message) -> Result.success();
        CommandLineArgument verboseArgument = console.getCommandLine().get("verbose");
        if (verboseArgument != null)
        {
            final String verboseArgumentValue = verboseArgument.getValue();
            if (Strings.isNullOrEmpty(verboseArgumentValue) || verboseArgumentValue.equals("true"))
            {
                result = (String message) -> console.writeLine("VERBOSE: " + message);
            }
        }

        PostCondition.assertNotNull(result, "result");

        return result;
    }

    public static void main(String[] args)
    {
        PreCondition.assertNotNull(args, "args");

        try (final Console console = new Console(args))
        {
            new QubClean().main(console);
        }
    }
}