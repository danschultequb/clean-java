package qub;

public interface QubCleanRunTests
{
    static void test(TestRunner runner)
    {
        runner.testGroup(QubCleanRun.class, () ->
        {
            runner.testGroup("getParameters(Process)", () ->
            {
                runner.test("with null", (Test test) ->
                {
                    test.assertThrows(() -> QubCleanRun.getParameters(null),
                        new PreConditionFailure("process cannot be null."));
                });

                runner.test("with no arguments", (Test test) ->
                {
                    try (final QubProcess process = QubProcess.create())
                    {
                        final QubCleanRunParameters parameters = QubCleanRun.getParameters(process);
                        test.assertNotNull(parameters);
                        test.assertEqual(process.getCurrentFolder(), parameters.getFolderToClean());
                        test.assertSame(process.getOutputWriteStream(), parameters.getOutput());
                        test.assertNotNull(parameters.getVerbose());
                    }
                });

                runner.test("with -?", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    try (final QubProcess process = QubCleanRunTests.createProcess(output, "-?"))
                    {
                        test.assertNull(QubCleanRun.getParameters(process));

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
                        final QubCleanRunParameters parameters = QubCleanRun.getParameters(process);
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
                        final QubCleanRunParameters parameters = QubCleanRun.getParameters(process);
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
                        final QubCleanRunParameters parameters = QubCleanRun.getParameters(process);
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
                        final QubCleanRunParameters parameters = QubCleanRun.getParameters(process);
                        test.assertNotNull(parameters);
                        test.assertEqual(process.getFileSystem().getFolder("/hello/there").await(), parameters.getFolderToClean());
                        test.assertSame(process.getOutputWriteStream(), parameters.getOutput());
                        test.assertNotNull(parameters.getVerbose());
                    }
                });
            });

            runner.testGroup("run(QubCleanRunParameters)", () ->
            {
                runner.test("with null", (Test test) ->
                {
                    test.assertThrows(() -> QubCleanRun.run((QubCleanRunParameters)null),
                        new PreConditionFailure("parameters cannot be null."));
                });

                runner.test("when no folders to clean exist", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final Folder currentFolder = QubCleanRunTests.getInMemoryCurrentFolder(test);
                    try (final QubProcess process = QubCleanRunTests.createProcess(output, currentFolder, "-verbose=false"))
                    {
                        QubCleanRun.run(QubCleanRun.getParameters(process));

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
                    final Folder currentFolder = QubCleanRunTests.getInMemoryCurrentFolder(test);
                    try (final QubProcess process = QubCleanRunTests.createProcess(output, currentFolder, "-verbose=true"))
                    {
                        QubCleanRun.run(QubCleanRun.getParameters(process));

                        test.assertEqual(0, process.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Cleaning...",
                            "VERBOSE: Checking if C:/outputs/ exists...",
                            "VERBOSE: Doesn't exist.",
                            "VERBOSE: Checking if C:/out/ exists...",
                            "VERBOSE: Doesn't exist.",
                            "VERBOSE: Checking if C:/target/ exists...",
                            "VERBOSE: Doesn't exist.",
                            "VERBOSE: Checking if C:/output/ exists...",
                            "VERBOSE: Doesn't exist.",
                            "VERBOSE: Checking if C:/dist/ exists...",
                            "VERBOSE: Doesn't exist.",
                            "Found no folders to delete."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("when outputs folder exists", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final Folder currentFolder = QubCleanRunTests.getInMemoryCurrentFolder(test);
                    final Folder outputsFolder = currentFolder.createFolder("outputs").await();
                    test.assertTrue(outputsFolder.exists().await());
                    try (final QubProcess process = QubCleanRunTests.createProcess(output, currentFolder))
                    {
                        QubCleanRun.run(QubCleanRun.getParameters(process));

                        test.assertEqual(0, process.getExitCode());
                    }
                    test.assertFalse(outputsFolder.exists().await());
                    test.assertEqual(
                        Iterable.create(
                            "Cleaning...",
                            "Deleting folder C:/outputs/... Done."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("when outputs folder exists with verbose", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final Folder currentFolder = QubCleanRunTests.getInMemoryCurrentFolder(test);
                    final Folder outputsFolder = currentFolder.createFolder("outputs").await();
                    test.assertTrue(outputsFolder.exists().await());
                    try (final QubProcess process = QubCleanRunTests.createProcess(output, currentFolder, "-verbose"))
                    {
                        QubCleanRun.run(QubCleanRun.getParameters(process));

                        test.assertEqual(0, process.getExitCode());
                    }
                    test.assertFalse(outputsFolder.exists().await());
                    test.assertEqual(
                        Iterable.create(
                            "Cleaning...",
                            "VERBOSE: Checking if C:/outputs/ exists...",
                            "Deleting folder C:/outputs/... Done.",
                            "VERBOSE: Checking if C:/out/ exists...",
                            "VERBOSE: Doesn't exist.",
                            "VERBOSE: Checking if C:/target/ exists...",
                            "VERBOSE: Doesn't exist.",
                            "VERBOSE: Checking if C:/output/ exists...",
                            "VERBOSE: Doesn't exist.",
                            "VERBOSE: Checking if C:/dist/ exists...",
                            "VERBOSE: Doesn't exist."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("when outputs folder exists but can't be deleted", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final InMemoryFileSystem fileSystem = getInMemoryFileSystem(test);
                    final Folder currentFolder = fileSystem.getFolder("C:/").await();
                    final Folder outputsFolder = currentFolder.createFolder("outputs").await();
                    fileSystem.setFolderCanDelete(outputsFolder.getPath(), false);
                    test.assertTrue(outputsFolder.exists().await());
                    try (final QubProcess process = QubCleanRunTests.createProcess(output, currentFolder))
                    {
                        QubCleanRun.run(QubCleanRun.getParameters(process));

                        test.assertEqual(0, process.getExitCode());
                    }
                    test.assertTrue(outputsFolder.exists().await());
                    test.assertEqual(
                        Iterable.create(
                            "Cleaning...",
                            "Deleting folder C:/outputs/... Failed.",
                            "  The folder at \"C:/outputs/\" doesn't exist."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with unnamed folder argument to rooted folder that doesn't exist", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final Folder currentFolder = QubCleanRunTests.getInMemoryCurrentFolder(test);
                    try (final QubProcess process = QubCleanRunTests.createProcess(output, currentFolder, "C:/i/dont/exist"))
                    {
                        QubCleanRun.run(QubCleanRun.getParameters(process));

                        test.assertEqual(0, process.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Cleaning...",
                            "The folder C:/i/dont/exist/ doesn't exist."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with named folder argument to unrooted folder that doesn't exist", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final Folder currentFolder = QubCleanRunTests.getInMemoryCurrentFolder(test);
                    try (final QubProcess process = QubCleanRunTests.createProcess(output, currentFolder, "i/dont/exist"))
                    {
                        QubCleanRun.run(QubCleanRun.getParameters(process));

                        test.assertEqual(0, process.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Cleaning...",
                            "The folder C:/i/dont/exist/ doesn't exist."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with named folder argument to rooted folder that doesn't exist", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final Folder currentFolder = QubCleanRunTests.getInMemoryCurrentFolder(test);
                    try (final QubProcess process = QubCleanRunTests.createProcess(output, currentFolder, "-folder=C:/i/dont/exist"))
                    {
                        QubCleanRun.run(QubCleanRun.getParameters(process));

                        test.assertEqual(0, process.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Cleaning...",
                            "The folder C:/i/dont/exist/ doesn't exist."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with named folder argument to unrooted folder that doesn't exist", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final Folder currentFolder = QubCleanRunTests.getInMemoryCurrentFolder(test);
                    try (final QubProcess process = QubCleanRunTests.createProcess(output, currentFolder, "-folder=i/dont/exist"))
                    {
                        QubCleanRun.run(QubCleanRun.getParameters(process));

                        test.assertEqual(0, process.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Cleaning...",
                            "The folder C:/i/dont/exist/ doesn't exist."),
                        Strings.getLines(output.getText().await()));
                });
            });
        });
    }

    static InMemoryFileSystem getInMemoryFileSystem(Test test)
    {
        PreCondition.assertNotNull(test, "test");

        final InMemoryFileSystem fileSystem = InMemoryFileSystem.create(test.getClock());
        fileSystem.createRoot("C:/").await();

        return fileSystem;
    }

    static Folder getInMemoryCurrentFolder(Test test)
    {
        PreCondition.assertNotNull(test, "test");

        return QubCleanRunTests.getInMemoryFileSystem(test).getFolder("C:/").await();
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

        final QubProcess result = QubCleanRunTests.createProcess(output, commandLineArguments);
        result.setFileSystem(currentFolder.getFileSystem());
        result.setCurrentFolderPath(currentFolder.getPath());

        PostCondition.assertNotNull(result, "result");

        return result;
    }
}
