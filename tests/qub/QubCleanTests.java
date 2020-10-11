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
                    test.assertThrows(() -> QubClean.main((String[])null),
                        new PreConditionFailure("arguments cannot be null."));
                });
            });

            runner.testGroup("getParameters(Process)", () ->
            {
                runner.test("with null", (Test test) ->
                {
                    test.assertThrows(() -> QubClean.getParameters(null),
                        new PreConditionFailure("process cannot be null."));
                });

                runner.test("with no arguments", (Test test) ->
                {
                    try (final QubProcess process = QubProcess.create())
                    {
                        final QubCleanParameters parameters = QubClean.getParameters(process);
                        test.assertNotNull(parameters);
                        test.assertEqual(process.getCurrentFolder(), parameters.getFolderToClean());
                        test.assertSame(process.getOutputWriteStream(), parameters.getOutput());
                        test.assertNotNull(parameters.getVerbose());
                    }
                });

                runner.test("with -?", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    try (final QubProcess process = QubCleanTests.createProcess(output, "-?"))
                    {
                        test.assertNull(QubClean.getParameters(process));

                        test.assertEqual(
                            Iterable.create(
                                "Usage: qub-clean [[--folder=]<folder-to-clean>] [--verbose] [--profiler] [--help]",
                                "  Used to clean build outputs from source code projects.",
                                "  --folder:     The folder to clean. Defaults to the current folder.",
                                "  --verbose(v): Whether or not to show verbose logs.",
                                "  --profiler:   Whether or not this application should pause before it is run to allow a profiler to be attached.",
                                "  --help(?):    Show the help message for this application."),
                            Strings.getLines(output.getText().await()));
                    }
                });

                runner.test("with anonymous relative path argument", (Test test) ->
                {
                    try (final QubProcess process = QubProcess.create("hello/there"))
                    {
                        final QubCleanParameters parameters = QubClean.getParameters(process);
                        test.assertNotNull(parameters);
                        test.assertEqual(process.getCurrentFolder().getFolder("hello/there").await(), parameters.getFolderToClean());
                        test.assertSame(process.getOutputWriteStream(), parameters.getOutput());
                        test.assertNotNull(parameters.getVerbose());
                    }
                });

                runner.test("with named relative path argument", (Test test) ->
                {
                    try (final QubProcess process = QubProcess.create("--folder=hello/there"))
                    {
                        final QubCleanParameters parameters = QubClean.getParameters(process);
                        test.assertNotNull(parameters);
                        test.assertEqual(process.getCurrentFolder().getFolder("hello/there").await(), parameters.getFolderToClean());
                        test.assertSame(process.getOutputWriteStream(), parameters.getOutput());
                        test.assertNotNull(parameters.getVerbose());
                    }
                });

                runner.test("with anonymous rooted path argument", (Test test) ->
                {
                    try (final QubProcess process = QubProcess.create("/hello/there"))
                    {
                        final QubCleanParameters parameters = QubClean.getParameters(process);
                        test.assertNotNull(parameters);
                        test.assertEqual(process.getFileSystem().getFolder("/hello/there").await(), parameters.getFolderToClean());
                        test.assertSame(process.getOutputWriteStream(), parameters.getOutput());
                        test.assertNotNull(parameters.getVerbose());
                    }
                });

                runner.test("with named rooted path argument", (Test test) ->
                {
                    try (final QubProcess process = QubProcess.create("--folder=/hello/there"))
                    {
                        final QubCleanParameters parameters = QubClean.getParameters(process);
                        test.assertNotNull(parameters);
                        test.assertEqual(process.getFileSystem().getFolder("/hello/there").await(), parameters.getFolderToClean());
                        test.assertSame(process.getOutputWriteStream(), parameters.getOutput());
                        test.assertNotNull(parameters.getVerbose());
                    }
                });
            });

            runner.testGroup("run(QubCleanParameters)", () ->
            {
                runner.test("with null", (Test test) ->
                {
                    test.assertThrows(() -> QubClean.run((QubCleanParameters)null),
                        new PreConditionFailure("parameters cannot be null."));
                });

                runner.test("when no folders to clean exist", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final Folder currentFolder = QubCleanTests.getInMemoryCurrentFolder(test);
                    try (final QubProcess process = QubCleanTests.createProcess(output, currentFolder, "-verbose=false"))
                    {
                        QubClean.run(QubClean.getParameters(process));

                        test.assertEqual(0, process.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Cleaning...",
                            "Found no folders to delete."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("when no folders to clean exist with verbose", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final Folder currentFolder = QubCleanTests.getInMemoryCurrentFolder(test);
                    try (final QubProcess process = QubCleanTests.createProcess(output, currentFolder, "-verbose=true"))
                    {
                        QubClean.run(QubClean.getParameters(process));

                        test.assertEqual(0, process.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Cleaning...",
                            "VERBOSE: Checking if /outputs/ exists...",
                            "VERBOSE: Doesn't exist.",
                            "VERBOSE: Checking if /out/ exists...",
                            "VERBOSE: Doesn't exist.",
                            "VERBOSE: Checking if /target/ exists...",
                            "VERBOSE: Doesn't exist.",
                            "VERBOSE: Checking if /output/ exists...",
                            "VERBOSE: Doesn't exist.",
                            "VERBOSE: Checking if /dist/ exists...",
                            "VERBOSE: Doesn't exist.",
                            "Found no folders to delete."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("when outputs folder exists", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final Folder currentFolder = QubCleanTests.getInMemoryCurrentFolder(test);
                    final Folder outputsFolder = currentFolder.createFolder("outputs").await();
                    test.assertTrue(outputsFolder.exists().await());
                    try (final QubProcess process = QubCleanTests.createProcess(output, currentFolder))
                    {
                        QubClean.run(QubClean.getParameters(process));

                        test.assertEqual(0, process.getExitCode());
                    }
                    test.assertFalse(outputsFolder.exists().await());
                    test.assertEqual(
                        Iterable.create(
                            "Cleaning...",
                            "Deleting folder /outputs/... Done."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("when outputs folder exists with verbose", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final Folder currentFolder = QubCleanTests.getInMemoryCurrentFolder(test);
                    final Folder outputsFolder = currentFolder.createFolder("outputs").await();
                    test.assertTrue(outputsFolder.exists().await());
                    try (final QubProcess process = QubCleanTests.createProcess(output, currentFolder, "-verbose"))
                    {
                        QubClean.run(QubClean.getParameters(process));

                        test.assertEqual(0, process.getExitCode());
                    }
                    test.assertFalse(outputsFolder.exists().await());
                    test.assertEqual(
                        Iterable.create(
                            "Cleaning...",
                            "VERBOSE: Checking if /outputs/ exists...",
                            "Deleting folder /outputs/... Done.",
                            "VERBOSE: Checking if /out/ exists...",
                            "VERBOSE: Doesn't exist.",
                            "VERBOSE: Checking if /target/ exists...",
                            "VERBOSE: Doesn't exist.",
                            "VERBOSE: Checking if /output/ exists...",
                            "VERBOSE: Doesn't exist.",
                            "VERBOSE: Checking if /dist/ exists...",
                            "VERBOSE: Doesn't exist."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("when outputs folder exists but can't be deleted", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final InMemoryFileSystem fileSystem = getInMemoryFileSystem(test);
                    final Folder currentFolder = fileSystem.getFolder("/").await();
                    final Folder outputsFolder = currentFolder.createFolder("outputs").await();
                    fileSystem.setFolderCanDelete(outputsFolder.getPath(), false);
                    test.assertTrue(outputsFolder.exists().await());
                    try (final QubProcess process = QubCleanTests.createProcess(output, currentFolder))
                    {
                        QubClean.run(QubClean.getParameters(process));

                        test.assertEqual(0, process.getExitCode());
                    }
                    test.assertTrue(outputsFolder.exists().await());
                    test.assertEqual(
                        Iterable.create(
                            "Cleaning...",
                            "Deleting folder /outputs/... Failed.",
                            "  The folder at \"/outputs/\" doesn't exist."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with unnamed folder argument to rooted folder that doesn't exist", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final Folder currentFolder = QubCleanTests.getInMemoryCurrentFolder(test);
                    try (final QubProcess process = QubCleanTests.createProcess(output, currentFolder, "/i/dont/exist"))
                    {
                        QubClean.run(QubClean.getParameters(process));

                        test.assertEqual(0, process.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Cleaning...",
                            "The folder /i/dont/exist/ doesn't exist."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with named folder argument to unrooted folder that doesn't exist", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final Folder currentFolder = QubCleanTests.getInMemoryCurrentFolder(test);
                    try (final QubProcess process = QubCleanTests.createProcess(output, currentFolder, "i/dont/exist"))
                    {
                        QubClean.run(QubClean.getParameters(process));

                        test.assertEqual(0, process.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Cleaning...",
                            "The folder /i/dont/exist/ doesn't exist."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with named folder argument to rooted folder that doesn't exist", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final Folder currentFolder = QubCleanTests.getInMemoryCurrentFolder(test);
                    try (final QubProcess process = QubCleanTests.createProcess(output, currentFolder, "-folder=/i/dont/exist"))
                    {
                        QubClean.run(QubClean.getParameters(process));

                        test.assertEqual(0, process.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Cleaning...",
                            "The folder /i/dont/exist/ doesn't exist."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with named folder argument to unrooted folder that doesn't exist", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final Folder currentFolder = QubCleanTests.getInMemoryCurrentFolder(test);
                    try (final QubProcess process = QubCleanTests.createProcess(output, currentFolder, "-folder=i/dont/exist"))
                    {
                        QubClean.run(QubClean.getParameters(process));

                        test.assertEqual(0, process.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Cleaning...",
                            "The folder /i/dont/exist/ doesn't exist."),
                        Strings.getLines(output.getText().await()));
                });
            });
        });
    }

    static InMemoryFileSystem getInMemoryFileSystem(Test test)
    {
        PreCondition.assertNotNull(test, "test");

        final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
        fileSystem.createRoot("/").await();

        return fileSystem;
    }

    static Folder getInMemoryCurrentFolder(Test test)
    {
        PreCondition.assertNotNull(test, "test");

        return QubCleanTests.getInMemoryFileSystem(test).getFolder("/").await();
    }

    static QubProcess createProcess(InMemoryCharacterToByteStream output, String... commandLineArguments)
    {
        PreCondition.assertNotNull(output, "output");
        PreCondition.assertNotNull(commandLineArguments, "commandLineArguments");

        final QubProcess result = QubProcess.create(commandLineArguments);
        result.setLineSeparator("\n");
        result.setOutputWriteStream(output);

        return result;
    }

    static QubProcess createProcess(InMemoryCharacterToByteStream output, Folder currentFolder, String... commandLineArguments)
    {
        PreCondition.assertNotNull(output, "output");
        PreCondition.assertNotNull(currentFolder, "currentFolder");
        PreCondition.assertNotNull(commandLineArguments, "commandLineArguments");

        final QubProcess result = QubCleanTests.createProcess(output, commandLineArguments);
        result.setFileSystem(currentFolder.getFileSystem());
        result.setCurrentFolderPath(currentFolder.getPath());

        PostCondition.assertNotNull(result, "result");

        return result;
    }
}
