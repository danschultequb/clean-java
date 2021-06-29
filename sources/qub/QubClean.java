package qub;

public interface QubClean
{
    String applicationName = "qub-clean";
    String applicationDescription = "Used to clean build outputs from source code projects.";

    static void main(String[] args)
    {
        DesktopProcess.run(args, QubClean::run);
    }

    static void run(DesktopProcess process)
    {
        PreCondition.assertNotNull(process, "process");

        final CommandLineActions actions = process.createCommandLineActions()
            .setApplicationName(QubClean.applicationName)
            .setApplicationDescription(QubClean.applicationDescription);

        actions.addAction("run", QubClean::runAction)
            .setDescription("Clean build outputs from source code projects.")
            .setDefaultAction();
        actions.addAction(CommandLineLogsAction::addAction);

        actions.run();
    }

    static void runAction(DesktopProcess process, CommandLineAction action)
    {
        PreCondition.assertNotNull(process, "process");
        PreCondition.assertNotNull(action, "action");

        final CommandLineParameters parameters = action.createCommandLineParameters(process);
        final CommandLineParameter<Folder> folderToCleanParameter = parameters.addPositionalFolder("folder", process)
            .setValueName("<folder-to-clean>")
            .setDescription("The folder to clean. Defaults to the current folder.");
        final CommandLineParameterVerbose verboseParameter = parameters.addVerbose(process);
        final CommandLineParameterProfiler profiler = parameters.addProfiler(process, QubClean.class);
        final CommandLineParameterHelp help = parameters.addHelp();

        if (!help.showApplicationHelpLines(process).await())
        {
            profiler.await();

            final Folder folderToClean = folderToCleanParameter.getValue().await();
            final Folder qubCleanDataFolder = process.getQubProjectDataFolder().await();

            final LogStreams logStreams = CommandLineLogsAction.getLogStreamsFromDataFolder(qubCleanDataFolder, process.getOutputWriteStream(), verboseParameter.getVerboseCharacterToByteWriteStream().await());
            try (final Disposable logStream = logStreams.getLogStream())
            {
                final IndentedCharacterWriteStream output = IndentedCharacterWriteStream.create(logStreams.getOutput());
                final VerboseCharacterToByteWriteStream verbose = logStreams.getVerbose();

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
            }
        }
    }
}