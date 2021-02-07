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

        process.createCommandLineActions()
            .setApplicationName(QubClean.applicationName)
            .setApplicationDescription(QubClean.applicationDescription)
            .addAction(QubCleanRun::addAction)
            .addAction(CommandLineLogsAction::addAction)
            .run();
    }
}