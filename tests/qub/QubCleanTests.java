package qub;

public interface QubCleanTests
{
    static void test(TestRunner runner)
    {
        runner.testGroup(QubClean.class, () ->
        {
            runner.testGroup("main(String[])", () ->
            {
                runner.test("with null", (Test test) ->
                {
                    test.assertThrows(() -> QubClean.main(null),
                        new PreConditionFailure("args cannot be null."));
                });
            });
            
            runner.testGroup("run(QubProcess)", () ->
            {
                runner.test("with null", (Test test) ->
                {
                    test.assertThrows(() -> QubClean.run(null),
                        new PreConditionFailure("process cannot be null."));
                });

                runner.test("with -?", (Test test) ->
                {
                    try (final FakeDesktopProcess process = FakeDesktopProcess.create("-?"))
                    {
                        QubClean.run(process);

                        test.assertEqual(
                            Iterable.create(
                                "Usage: qub-clean [--action=]<action-name> [--help]",
                                "  Used to clean build outputs from source code projects.",
                                "  --action(a): The name of the action to invoke.",
                                "  --help(?):   Show the help message for this application.",
                                "",
                                "Actions:",
                                "  logs:          Show the logs folder.",
                                "  run (default): Clean build outputs from source code projects."),
                            Strings.getLines(process.getOutputWriteStream().getText().await()));
                    }
                });

                runner.test("with run -?", (Test test) ->
                {
                    try (final FakeDesktopProcess process = FakeDesktopProcess.create("run", "-?"))
                    {
                        QubClean.run(process);

                        test.assertEqual(
                            Iterable.create(
                                "Usage: qub-clean run [[--folder=]<folder-to-clean>] [--verbose] [--profiler] [--help]",
                                "  Clean build outputs from source code projects.",
                                "  --folder:     The folder to clean. Defaults to the current folder.",
                                "  --verbose(v): Whether or not to show verbose logs.",
                                "  --profiler:   Whether or not this application should pause before it is run to allow a profiler to be attached.",
                                "  --help(?):    Show the help message for this application."),
                            Strings.getLines(process.getOutputWriteStream().getText().await()));
                    }
                });

                runner.test("with logs -?", (Test test) ->
                {
                    try (final FakeDesktopProcess process = FakeDesktopProcess.create("logs", "-?"))
                    {
                        QubClean.run(process);

                        test.assertEqual(
                            Iterable.create(
                                "Usage: qub-clean logs [--openWith] [--help]",
                                "  Show the logs folder.",
                                "  --openWith: The application to use to open the logs folder.",
                                "  --help(?):  Show the help message for this application."),
                            Strings.getLines(process.getOutputWriteStream().getText().await()));
                    }
                });
            });
        });
    }
}
