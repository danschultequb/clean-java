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
                    test.assertThrows(() -> QubClean.main((String[])null), new PreConditionFailure("args cannot be null."));
                });
            });

            runner.testGroup("main(Console)", () ->
            {
                runner.test("with null", (Test test) ->
                {
                    test.assertThrows(() -> QubClean.main((Console)null), new PreConditionFailure("console cannot be null."));
                });

                runner.test("with -? command line argument", (Test test) ->
                {
                    final InMemoryCharacterStream output = getInMemoryCharacterStream(test);
                    try (final Console console = createConsole(output, "-?"))
                    {
                        QubClean.main(console);
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Usage: qub-clean [[--folder=]<folder-to-clean>] [--verbose] [--profiler] [--help]",
                            "  Used to clean build outputs from source code projects.",
                            "  --folder: The folder to clean. Defaults to the current folder.",
                            "  --verbose(v): Whether or not to show verbose logs.",
                            "  --profiler: Whether or not this application should pause before it is run to allow a profiler to be attached.",
                            "  --help(?): Show the help message for this application."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("when no folders to clean exist", (Test test) ->
                {
                    final InMemoryCharacterStream output = getInMemoryCharacterStream(test);
                    final Folder currentFolder = getInMemoryCurrentFolder(test);
                    try (final Console console = createConsole(output, currentFolder, "-verbose=false"))
                    {
                        QubClean.main(console);
                    }
                    final String outputText = output.getText().await();
                    test.assertContains(outputText, "Cleaning...");
                    test.assertContains(outputText, "Found no folders to delete.");
                    test.assertContains(outputText, "Done (");
                    test.assertContains(outputText, "Seconds)");
                });

                runner.test("when no folders to clean exist with verbose", (Test test) ->
                {
                    final InMemoryCharacterStream output = getInMemoryCharacterStream(test);
                    final Folder currentFolder = getInMemoryCurrentFolder(test);
                    try (final Console console = createConsole(output, currentFolder, "-verbose=true"))
                    {
                        QubClean.main(console);
                    }
                    final String outputText = output.getText().await();
                    test.assertContains(outputText, "Cleaning...");
                    test.assertContains(outputText, "VERBOSE: Checking if /outputs exists...");
                    test.assertContains(outputText, "VERBOSE: Doesn't exist.");
                    test.assertContains(outputText, "VERBOSE: Checking if /out exists...");
                    test.assertContains(outputText, "VERBOSE: Doesn't exist.");
                    test.assertContains(outputText, "VERBOSE: Checking if /target exists...");
                    test.assertContains(outputText, "VERBOSE: Doesn't exist.");
                    test.assertContains(outputText, "VERBOSE: Checking if /output exists...");
                    test.assertContains(outputText, "VERBOSE: Doesn't exist.");
                    test.assertContains(outputText, "Found no folders to delete.");
                    test.assertContains(outputText, "Done (");
                    test.assertContains(outputText, "Seconds)");
                });

                runner.test("when outputs folder exists", (Test test) ->
                {
                    final InMemoryCharacterStream output = getInMemoryCharacterStream(test);
                    final Folder currentFolder = getInMemoryCurrentFolder(test);
                    final Folder outputsFolder = currentFolder.createFolder("outputs").await();
                    test.assertTrue(outputsFolder.exists().await());
                    try (final Console console = createConsole(output, currentFolder))
                    {
                        QubClean.main(console);
                    }
                    test.assertFalse(outputsFolder.exists().await());
                    final String outputText = output.getText().await();
                    test.assertContains(outputText, "Cleaning...");
                    test.assertContains(outputText, "Deleting folder /outputs... Done.");
                    test.assertContains(outputText, "Done (");
                    test.assertContains(outputText, "Seconds)");
                });

                runner.test("when outputs folder exists with verbose", (Test test) ->
                {
                    final InMemoryCharacterStream output = getInMemoryCharacterStream(test);
                    final Folder currentFolder = getInMemoryCurrentFolder(test);
                    final Folder outputsFolder = currentFolder.createFolder("outputs").await();
                    test.assertTrue(outputsFolder.exists().await());
                    try (final Console console = createConsole(output, currentFolder, "-verbose"))
                    {
                        QubClean.main(console);
                    }
                    test.assertFalse(outputsFolder.exists().await());
                    final String outputText = output.getText().await();
                    test.assertContains(outputText, "Cleaning...");
                    test.assertContains(outputText, "Deleting folder /outputs... Done.");
                    test.assertContains(outputText, "VERBOSE: Checking if /out exists...");
                    test.assertContains(outputText, "VERBOSE: Doesn't exist.");
                    test.assertContains(outputText, "VERBOSE: Checking if /target exists...");
                    test.assertContains(outputText, "VERBOSE: Doesn't exist.");
                    test.assertContains(outputText, "VERBOSE: Checking if /output exists...");
                    test.assertContains(outputText, "VERBOSE: Doesn't exist.");
                    test.assertContains(outputText, "Done (");
                    test.assertContains(outputText, "Seconds)");
                });

                runner.test("when outputs folder exists but can't be deleted", (Test test) ->
                {
                    final InMemoryCharacterStream output = getInMemoryCharacterStream(test);
                    final InMemoryFileSystem fileSystem = getInMemoryFileSystem(test);
                    final Folder currentFolder = fileSystem.getFolder("/").await();
                    final Folder outputsFolder = currentFolder.createFolder("outputs").await();
                    fileSystem.setFolderCanDelete(outputsFolder.getPath(), false);
                    test.assertTrue(outputsFolder.exists().await());
                    try (final Console console = createConsole(output, currentFolder))
                    {
                        QubClean.main(console);
                    }
                    test.assertTrue(outputsFolder.exists().await());
                    final String outputText = output.getText().await();
                    test.assertContains(outputText, "Cleaning...");
                    test.assertContains(outputText, "Deleting folder /outputs... Failed.");
                    test.assertContains(outputText, "The folder at \"/outputs\" doesn't exist.");
                    test.assertContains(outputText, "Done (");
                    test.assertContains(outputText, "Seconds)");
                });

                runner.test("with unnamed folder argument to rooted folder that doesn't exist", (Test test) ->
                {
                    final InMemoryCharacterStream output = getInMemoryCharacterStream(test);
                    final Folder currentFolder = getInMemoryCurrentFolder(test);
                    try (final Console console = createConsole(output, currentFolder, "/i/dont/exist"))
                    {
                        QubClean.main(console);
                    }
                    final String outputText = output.getText().await();
                    test.assertContains(outputText, "Cleaning...");
                    test.assertContains(outputText, "The folder /i/dont/exist doesn't exist.");
                    test.assertContains(outputText, "Done (");
                    test.assertContains(outputText, "Seconds)");
                });

                runner.test("with named folder argument to unrooted folder that doesn't exist", (Test test) ->
                {
                    final InMemoryCharacterStream output = getInMemoryCharacterStream(test);
                    final Folder currentFolder = getInMemoryCurrentFolder(test);
                    try (final Console console = createConsole(output, currentFolder, "i/dont/exist"))
                    {
                        QubClean.main(console);
                    }
                    final String outputText = output.getText().await();
                    test.assertContains(outputText, "Cleaning...");
                    test.assertContains(outputText, "The folder /i/dont/exist doesn't exist.");
                    test.assertContains(outputText, "Done (");
                    test.assertContains(outputText, "Seconds)");
                });

                runner.test("with named folder argument to rooted folder that doesn't exist", (Test test) ->
                {
                    final InMemoryCharacterStream output = getInMemoryCharacterStream(test);
                    final Folder currentFolder = getInMemoryCurrentFolder(test);
                    try (final Console console = createConsole(output, currentFolder, "-folder=/i/dont/exist"))
                    {
                        QubClean.main(console);
                    }
                    final String outputText = output.getText().await();
                    test.assertContains(outputText, "Cleaning...");
                    test.assertContains(outputText, "The folder /i/dont/exist doesn't exist.");
                    test.assertContains(outputText, "Done (");
                    test.assertContains(outputText, "Seconds)");
                });

                runner.test("with named folder argument to unrooted folder that doesn't exist", (Test test) ->
                {
                    final InMemoryCharacterStream output = getInMemoryCharacterStream(test);
                    final Folder currentFolder = getInMemoryCurrentFolder(test);
                    try (final Console console = createConsole(output, currentFolder, "-folder=i/dont/exist"))
                    {
                        QubClean.main(console);
                    }
                    final String outputText = output.getText().await();
                    test.assertContains(outputText, "Cleaning...");
                    test.assertContains(outputText, "The folder /i/dont/exist doesn't exist.");
                    test.assertContains(outputText, "Done (");
                    test.assertContains(outputText, "Seconds)");
                });
            });
        });
    }

    static InMemoryCharacterStream getInMemoryCharacterStream(Test test)
    {
        return new InMemoryCharacterStream();
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

        return getInMemoryFileSystem(test).getFolder("/").await();
    }

    static Console createConsole(CharacterWriteStream output, String... commandLineArguments)
    {
        PreCondition.assertNotNull(output, "output");
        PreCondition.assertNotNull(commandLineArguments, "commandLineArguments");

        final Console result = new Console(CommandLineArguments.create(commandLineArguments));
        result.setLineSeparator("\n");
        result.setOutputCharacterWriteStream(output);

        return result;
    }

    static Console createConsole(CharacterWriteStream output, Folder currentFolder, String... commandLineArguments)
    {
        PreCondition.assertNotNull(output, "output");
        PreCondition.assertNotNull(currentFolder, "currentFolder");
        PreCondition.assertNotNull(commandLineArguments, "commandLineArguments");

        final Console result = createConsole(output, commandLineArguments);
        result.setFileSystem(currentFolder.getFileSystem());
        result.setCurrentFolderPath(currentFolder.getPath());

        PostCondition.assertNotNull(result, "result");

        return result;
    }
}
