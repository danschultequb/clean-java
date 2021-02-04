package qub;

public interface QubCleanRunTests
{
    static void test(TestRunner runner)
    {
        runner.testGroup(QubCleanRun.class, () ->
        {
            runner.testGroup("getParameters(DesktopProcess)", () ->
            {
                runner.test("with null process", (Test test) ->
                {
                    final FakeDesktopProcess process = null;
                    final String fullActionName = "full-action-name";
                    test.assertThrows(() -> QubCleanRun.getParameters(process, fullActionName),
                        new PreConditionFailure("process cannot be null."));
                });
                
                runner.test("with null fullActionName", (Test test) ->
                {
                    try (final FakeDesktopProcess process = FakeDesktopProcess.create())
                    {
                        final String fullActionName = null;
                        test.assertThrows(() -> QubCleanRun.getParameters(process, fullActionName),
                            new PreConditionFailure("fullActionName cannot be null."));
                    }
                });
                
                runner.test("with empty fullActionName", (Test test) ->
                {
                    try (final FakeDesktopProcess process = FakeDesktopProcess.create())
                    {
                        final String fullActionName = "";
                        test.assertThrows(() -> QubCleanRun.getParameters(process, fullActionName),
                            new PreConditionFailure("fullActionName cannot be empty."));
                    }
                });

                runner.test("with no arguments", (Test test) ->
                {
                    try (final FakeDesktopProcess process = FakeDesktopProcess.create())
                    {
                        final String fullActionName = "full-action-name";
                        final QubCleanRunParameters parameters = QubCleanRun.getParameters(process, fullActionName);
                        test.assertNotNull(parameters);
                        test.assertEqual(process.getCurrentFolder(), parameters.getFolderToClean());
                        test.assertSame(process.getOutputWriteStream(), parameters.getOutput());
                        test.assertNotNull(parameters.getVerbose());
                    }
                });

                runner.test("with -?", (Test test) ->
                {
                    try (final FakeDesktopProcess process = FakeDesktopProcess.create("-?"))
                    {
                        final String fullActionName = "full-action-name";
                        test.assertNull(QubCleanRun.getParameters(process, fullActionName));

                        test.assertEqual(
                            Iterable.create(
                                "Usage: full-action-name [[--folder=]<folder-to-clean>] [--verbose] [--profiler] [--help]",
                                "  Clean build outputs from source code projects.",
                                "  --folder:     The folder to clean. Defaults to the current folder.",
                                "  --verbose(v): Whether or not to show verbose logs.",
                                "  --profiler:   Whether or not this application should pause before it is run to allow a profiler to be attached.",
                                "  --help(?):    Show the help message for this application."),
                            Strings.getLines(process.getOutputWriteStream().getText().await()));
                    }
                });

                runner.test("with anonymous relative path argument", (Test test) ->
                {
                    try (final FakeDesktopProcess process = FakeDesktopProcess.create("hello/there"))
                    {
                        final String fullActionName = "full-action-name";
                        final QubCleanRunParameters parameters = QubCleanRun.getParameters(process, fullActionName);
                        test.assertNotNull(parameters);
                        test.assertEqual(process.getCurrentFolder().getFolder("hello/there").await(), parameters.getFolderToClean());
                        test.assertSame(process.getOutputWriteStream(), parameters.getOutput());
                        test.assertNotNull(parameters.getVerbose());
                    }
                });

                runner.test("with named relative path argument", (Test test) ->
                {
                    try (final FakeDesktopProcess process = FakeDesktopProcess.create("--folder=hello/there"))
                    {
                        final String fullActionName = "full-action-name";
                        final QubCleanRunParameters parameters = QubCleanRun.getParameters(process, fullActionName);
                        test.assertNotNull(parameters);
                        test.assertEqual(process.getCurrentFolder().getFolder("hello/there").await(), parameters.getFolderToClean());
                        test.assertSame(process.getOutputWriteStream(), parameters.getOutput());
                        test.assertNotNull(parameters.getVerbose());
                    }
                });

                runner.test("with anonymous rooted path argument", (Test test) ->
                {
                    try (final FakeDesktopProcess process = FakeDesktopProcess.create("/hello/there"))
                    {
                        final String fullActionName = "full-action-name";
                        final QubCleanRunParameters parameters = QubCleanRun.getParameters(process, fullActionName);
                        test.assertNotNull(parameters);
                        test.assertEqual(process.getFileSystem().getFolder("/hello/there").await(), parameters.getFolderToClean());
                        test.assertSame(process.getOutputWriteStream(), parameters.getOutput());
                        test.assertNotNull(parameters.getVerbose());
                    }
                });

                runner.test("with named rooted path argument", (Test test) ->
                {
                    try (final FakeDesktopProcess process = FakeDesktopProcess.create("--folder=/hello/there"))
                    {
                        final String fullActionName = "full-action-name";
                        final QubCleanRunParameters parameters = QubCleanRun.getParameters(process, fullActionName);
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
                    test.assertThrows(() -> QubCleanRun.run(null),
                        new PreConditionFailure("parameters cannot be null."));
                });

                runner.test("when folder to clean doesn't exist", (Test test) ->
                {
                    final InMemoryFileSystem fileSystem = InMemoryFileSystem.create();
                    fileSystem.createRoot("/");
                    final Folder folderToClean = fileSystem.getFolder("/folder/to/clean/").await();
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final Folder dataFolder = fileSystem.getFolder("/data/folder/").await();
                    final QubCleanRunParameters parameters = QubCleanRunParameters.create(folderToClean, output, dataFolder);

                    QubCleanRun.run(parameters);

                    test.assertEqual(
                        Iterable.create(
                            "Cleaning...",
                            "The folder /folder/to/clean/ doesn't exist."),
                        Strings.getLines(output.getText().await()));
                    test.assertTrue(dataFolder.exists().await());
                });

                runner.test("when no folders to clean exist", (Test test) ->
                {
                    final InMemoryFileSystem fileSystem = InMemoryFileSystem.create();
                    fileSystem.createRoot("/");
                    final Folder folderToClean = fileSystem.createFolder("/folder/to/clean/").await();
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final Folder dataFolder = fileSystem.getFolder("/data/folder/").await();
                    final QubCleanRunParameters parameters = QubCleanRunParameters.create(folderToClean, output, dataFolder);

                    QubCleanRun.run(parameters);

                    test.assertEqual(
                        Iterable.create(
                            "Cleaning...",
                            "Found no folders to delete."),
                        Strings.getLines(output.getText().await()));
                    test.assertTrue(dataFolder.exists().await());
                });

                runner.test("when no folders to clean exist with verbose", (Test test) ->
                {
                    final InMemoryFileSystem fileSystem = InMemoryFileSystem.create();
                    fileSystem.createRoot("/");
                    final Folder folderToClean = fileSystem.createFolder("/folder/to/clean/").await();
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final Folder dataFolder = fileSystem.getFolder("/data/folder/").await();
                    final QubCleanRunParameters parameters = QubCleanRunParameters.create(folderToClean, output, dataFolder)
                        .setVerbose(VerboseCharacterToByteWriteStream.create(output));

                    QubCleanRun.run(parameters);

                    test.assertEqual(
                        Iterable.create(
                            "Cleaning...",
                            "VERBOSE: Checking if /folder/to/clean/outputs/ exists...",
                            "VERBOSE: Doesn't exist.",
                            "VERBOSE: Checking if /folder/to/clean/out/ exists...",
                            "VERBOSE: Doesn't exist.",
                            "VERBOSE: Checking if /folder/to/clean/target/ exists...",
                            "VERBOSE: Doesn't exist.",
                            "VERBOSE: Checking if /folder/to/clean/output/ exists...",
                            "VERBOSE: Doesn't exist.",
                            "VERBOSE: Checking if /folder/to/clean/dist/ exists...",
                            "VERBOSE: Doesn't exist.",
                            "Found no folders to delete."),
                        Strings.getLines(output.getText().await()));
                    test.assertTrue(dataFolder.exists().await());
                });

                runner.test("when outputs folder exists", (Test test) ->
                {
                    final InMemoryFileSystem fileSystem = InMemoryFileSystem.create();
                    fileSystem.createRoot("/");
                    final Folder folderToClean = fileSystem.createFolder("/folder/to/clean/").await();
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final Folder dataFolder = fileSystem.getFolder("/data/folder/").await();
                    final QubCleanRunParameters parameters = QubCleanRunParameters.create(folderToClean, output, dataFolder);

                    final Folder outputsFolder = folderToClean.createFolder("outputs").await();

                    QubCleanRun.run(parameters);

                    test.assertEqual(
                        Iterable.create(
                            "Cleaning...",
                            "Deleting folder " + outputsFolder + "... Done."),
                        Strings.getLines(output.getText().await()));
                    test.assertFalse(outputsFolder.exists().await());
                });

                runner.test("when outputs folder exists with verbose", (Test test) ->
                {
                    final InMemoryFileSystem fileSystem = InMemoryFileSystem.create();
                    fileSystem.createRoot("/");
                    final Folder folderToClean = fileSystem.createFolder("/folder/to/clean/").await();
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final Folder dataFolder = fileSystem.getFolder("/data/folder/").await();
                    final QubCleanRunParameters parameters = QubCleanRunParameters.create(folderToClean, output, dataFolder)
                        .setVerbose(VerboseCharacterToByteWriteStream.create(output));

                    final Folder outputsFolder = folderToClean.createFolder("outputs").await();

                    QubCleanRun.run(parameters);

                    test.assertEqual(
                        Iterable.create(
                            "Cleaning...",
                            "VERBOSE: Checking if /folder/to/clean/outputs/ exists...",
                            "Deleting folder /folder/to/clean/outputs/... Done.",
                            "VERBOSE: Checking if /folder/to/clean/out/ exists...",
                            "VERBOSE: Doesn't exist.",
                            "VERBOSE: Checking if /folder/to/clean/target/ exists...",
                            "VERBOSE: Doesn't exist.",
                            "VERBOSE: Checking if /folder/to/clean/output/ exists...",
                            "VERBOSE: Doesn't exist.",
                            "VERBOSE: Checking if /folder/to/clean/dist/ exists...",
                            "VERBOSE: Doesn't exist."),
                        Strings.getLines(output.getText().await()));
                    test.assertFalse(outputsFolder.exists().await());
                });

                runner.test("when outputs folder exists but can't be deleted", (Test test) ->
                {
                    final InMemoryFileSystem fileSystem = InMemoryFileSystem.create();
                    fileSystem.createRoot("/");
                    final Folder folderToClean = fileSystem.createFolder("/folder/to/clean/").await();
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final Folder dataFolder = fileSystem.getFolder("/data/folder/").await();
                    final QubCleanRunParameters parameters = QubCleanRunParameters.create(folderToClean, output, dataFolder);

                    final Folder outputsFolder = folderToClean.createFolder("outputs").await();
                    fileSystem.setFolderCanDelete(outputsFolder.getPath(), false);

                    QubCleanRun.run(parameters);

                    test.assertEqual(
                        Iterable.create(
                            "Cleaning...",
                            "Deleting folder /folder/to/clean/outputs/... Failed.",
                            "  The folder at \"/folder/to/clean/outputs/\" doesn't exist."),
                        Strings.getLines(output.getText().await()));
                    test.assertTrue(outputsFolder.exists().await());
                });
            });
        });
    }
}
