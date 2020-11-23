package qub;

public interface QubClean
{
    String applicationName = "qub-clean";
    String applicationDescription = "Used to clean build outputs from source code projects.";

    static void main(String[] args)
    {
        QubProcess.run(args, QubClean::run);
    }

    static void run(QubProcess process)
    {
        PreCondition.assertNotNull(process, "process");

        final CommandLineActions<QubProcess> actions = process.createCommandLineActions();
        actions.setApplicationName(QubClean.applicationName);
        actions.setApplicationDescription(QubClean.applicationDescription);

        actions.addAction(QubCleanRun.actionName, QubCleanRun::getParameters, QubCleanRun::run)
            .setDescription(QubCleanRun.actionDescription)
            .setDefaultAction();

        CommandLineLogsAction.add(actions);

        actions.run(process);
    }
}