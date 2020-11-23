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
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    try (final QubProcess process = QubProcess.create("-?"))
                    {
                        process.setOutputWriteStream(output);

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
                            Strings.getLines(output.getText().await()));
                    }
                });
            });
        });
    }
}
