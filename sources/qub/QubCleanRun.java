package qub;

public interface QubCleanRun
{
    String actionName = "run";
    String actionDescription = "Clean build outputs from source code projects.";

    static void addAction(CommandLineActions actions)
    {
        PreCondition.assertNotNull(actions, "actions");

        final String fullActionName = actions.getFullActionName(QubCleanRun.actionName);
        actions.addAction(QubCleanRun.actionName,
            (DesktopProcess process) -> QubCleanRun.getParameters(process, fullActionName),
            QubCleanRun::run)
            .setDescription(QubCleanRun.actionDescription)
            .setDefaultAction();
    }

    /**
     * Get the QubCleanParameters object from the provided Console and its command line arguments.
     * @param process The QubProcess to populate the QubCleanParameters object from.
     * @return The QubCleanParameters object or null if a help argument was provided.
     */
    static QubCleanRunParameters getParameters(DesktopProcess process, String fullActionName)
    {
        PreCondition.assertNotNull(process, "process");
        PreCondition.assertNotNullAndNotEmpty(fullActionName, "fullActionName");

        final CommandLineParameters parameters = process.createCommandLineParameters()
            .setApplicationName(fullActionName)
            .setApplicationDescription(QubCleanRun.actionDescription);
        final CommandLineParameter<Folder> folderToCleanParameter = parameters.addPositionalFolder("folder", process)
            .setValueName("<folder-to-clean>")
            .setDescription("The folder to clean. Defaults to the current folder.");
        final CommandLineParameterVerbose verboseParameter = parameters.addVerbose(process);
        final CommandLineParameterProfiler profiler = parameters.addProfiler(process, QubClean.class);
        final CommandLineParameterHelp help = parameters.addHelp();

        QubCleanRunParameters result = null;
        if (!help.showApplicationHelpLines(process).await())
        {
            profiler.await();

            final Folder folderToClean = folderToCleanParameter.getValue().await();
            final CharacterToByteWriteStream output = process.getOutputWriteStream();
            final Folder qubCleanDataFolder = process.getQubProjectDataFolder().await();
            final VerboseCharacterToByteWriteStream verbose = verboseParameter.getVerboseCharacterToByteWriteStream().await();
            result = QubCleanRunParameters.create(folderToClean, output, qubCleanDataFolder)
                .setVerbose(verbose);
        }

        return result;
    }

    static void run(QubCleanRunParameters parameters)
    {
        PreCondition.assertNotNull(parameters, "parameters");

        final Folder folderToClean = parameters.getFolderToClean();
        final Folder qubCleanDataFolder = parameters.getDataFolder();
        final LogStreams logStreams = CommandLineLogsAction.addLogStreamFromDataFolder(qubCleanDataFolder, parameters.getOutput(), parameters.getVerbose());
        try (final CharacterWriteStream logStream = logStreams.getLogStream())
        {
            final IndentedCharacterWriteStream output = IndentedCharacterWriteStream.create(logStreams.getOutput());
            final CharacterWriteStream verbose = logStreams.getVerbose();

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