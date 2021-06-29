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
            
            runner.testGroup("run(DesktopProcess)", () ->
            {
                runner.test("with null", (Test test) ->
                {
                    test.assertThrows(() -> QubClean.run(null),
                        new PreConditionFailure("process cannot be null."));
                });

                runner.test("with -?",
                    (TestResources resources) -> Tuple.create(resources.createFakeDesktopProcess("-?")),
                    (Test test, FakeDesktopProcess process) ->
                {
                    QubClean.run(process);

                    test.assertLinesEqual(
                        Iterable.create(
                            "Usage: qub-clean [--action=]<action-name> [--help]",
                            "  Used to clean build outputs from source code projects.",
                            "  --action(a): The name of the action to invoke.",
                            "  --help(?):   Show the help message for this application.",
                            "",
                            "Actions:",
                            "  logs:          Show the logs folder.",
                            "  run (default): Clean build outputs from source code projects."),
                        process.getOutputWriteStream());
                    test.assertLinesEqual(
                        Iterable.create(),
                        process.getErrorWriteStream());
                    test.assertEqual(-1, process.getExitCode());
                });

                runner.test("with run -?",
                    (TestResources resources) -> Tuple.create(resources.createFakeDesktopProcess("run", "-?")),
                    (Test test, FakeDesktopProcess process) ->
                {
                    QubClean.run(process);

                    test.assertLinesEqual(
                        Iterable.create(
                            "Usage: qub-clean run [[--folder=]<folder-to-clean>] [--verbose] [--profiler] [--help]",
                            "  Clean build outputs from source code projects.",
                            "  --folder:     The folder to clean. Defaults to the current folder.",
                            "  --verbose(v): Whether or not to show verbose logs.",
                            "  --profiler:   Whether or not this application should pause before it is run to allow a profiler to be attached.",
                            "  --help(?):    Show the help message for this application."),
                        process.getOutputWriteStream());
                    test.assertLinesEqual(
                        Iterable.create(),
                        process.getErrorWriteStream());
                    test.assertEqual(-1, process.getExitCode());
                });

                runner.test("with logs -?",
                    (TestResources resources) -> Tuple.create(resources.createFakeDesktopProcess("logs", "-?")),
                    (Test test, FakeDesktopProcess process) ->
                {
                    QubClean.run(process);

                    test.assertLinesEqual(
                        Iterable.create(
                            "Usage: qub-clean logs [--openWith] [--help]",
                            "  Show the logs folder.",
                            "  --openWith: The application to use to open the logs folder.",
                            "  --help(?):  Show the help message for this application."),
                        process.getOutputWriteStream());
                    test.assertLinesEqual(
                        Iterable.create(),
                        process.getErrorWriteStream());
                    test.assertEqual(-1, process.getExitCode());
                });

                runner.test("when folder to clean doesn't exist",
                    (TestResources resources) -> Tuple.create(resources.createFakeDesktopProcess("/folder/to/clean/")),
                    (Test test, FakeDesktopProcess process) ->
                {
                    QubClean.run(process);

                    test.assertLinesEqual(
                        Iterable.create(
                            "Cleaning...",
                            "The folder /folder/to/clean/ doesn't exist."),
                        process.getOutputWriteStream());
                    test.assertLinesEqual(
                        Iterable.create(),
                        process.getErrorWriteStream());
                    test.assertEqual(0, process.getExitCode());
                    test.assertTrue(process.getQubProjectDataFolder().await().exists().await());
                });

                runner.test("when no folders to clean exist",
                    (TestResources resources) -> Tuple.create(resources.createFakeDesktopProcess("/folder/to/clean/")),
                    (Test test, FakeDesktopProcess process) ->
                {
                    process.getFileSystem().createFolder("/folder/to/clean/").await();

                    QubClean.run(process);

                    test.assertLinesEqual(
                        Iterable.create(
                            "Cleaning...",
                            "Found no folders to delete."),
                        process.getOutputWriteStream());
                    test.assertLinesEqual(
                        Iterable.create(),
                        process.getErrorWriteStream());
                    test.assertEqual(0, process.getExitCode());
                    test.assertTrue(process.getQubProjectDataFolder().await().exists().await());
                });

                runner.test("when no folders to clean exist with verbose",
                    (TestResources resources) -> Tuple.create(resources.createFakeDesktopProcess("/folder/to/clean/", "--verbose")),
                    (Test test, FakeDesktopProcess process) ->
                {
                    process.getFileSystem().createFolder("/folder/to/clean/").await();

                    QubClean.run(process);

                    test.assertLinesEqual(
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
                        process.getOutputWriteStream());
                    test.assertLinesEqual(
                        Iterable.create(),
                        process.getErrorWriteStream());
                    test.assertEqual(0, process.getExitCode());
                    test.assertTrue(process.getQubProjectDataFolder().await().exists().await());
                });

                runner.test("when outputs folder exists",
                    (TestResources resources) -> Tuple.create(resources.createFakeDesktopProcess("/folder/to/clean/")),
                    (Test test, FakeDesktopProcess process) ->
                {
                    final Folder folderToClean = process.getFileSystem().getFolder("/folder/to/clean/").await();
                    final Folder outputsFolder = folderToClean.getFolder("outputs").await();
                    outputsFolder.create().await();

                    QubClean.run(process);

                    test.assertLinesEqual(
                        Iterable.create(
                            "Cleaning...",
                            "Deleting folder " + outputsFolder + "... Done."),
                        process.getOutputWriteStream());
                    test.assertLinesEqual(
                        Iterable.create(),
                        process.getErrorWriteStream());
                    test.assertEqual(0, process.getExitCode());
                    test.assertFalse(outputsFolder.exists().await());
                    test.assertTrue(folderToClean.exists().await());
                    test.assertTrue(process.getQubProjectDataFolder().await().exists().await());
                });

                runner.test("when outputs folder exists with verbose",
                    (TestResources resources) -> Tuple.create(resources.createFakeDesktopProcess("/folder/to/clean/", "--verbose")),
                    (Test test, FakeDesktopProcess process) ->
                {
                    final Folder folderToClean = process.getFileSystem().getFolder("/folder/to/clean/").await();
                    final Folder outputsFolder = folderToClean.getFolder("outputs").await();
                    outputsFolder.create().await();

                    QubClean.run(process);

                    test.assertLinesEqual(
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
                        process.getOutputWriteStream());
                    test.assertLinesEqual(
                        Iterable.create(),
                        process.getErrorWriteStream());
                    test.assertEqual(0, process.getExitCode());
                    test.assertFalse(outputsFolder.exists().await());
                    test.assertTrue(folderToClean.exists().await());
                    test.assertTrue(process.getQubProjectDataFolder().await().exists().await());
                });

                runner.test("when outputs folder exists but can't be deleted",
                    (TestResources resources) -> Tuple.create(resources.createFakeDesktopProcess("/folder/to/clean/")),
                    (Test test, FakeDesktopProcess process) ->
                {
                    final InMemoryFileSystem fileSystem = process.getFileSystem();
                    final Folder folderToClean = fileSystem.getFolder("/folder/to/clean/").await();
                    final Folder outputsFolder = folderToClean.getFolder("outputs").await();
                    outputsFolder.create().await();
                    fileSystem.setFolderCanDelete(outputsFolder.getPath(), false);

                    QubClean.run(process);

                    test.assertLinesEqual(
                        Iterable.create(
                            "Cleaning...",
                            "Deleting folder /folder/to/clean/outputs/... Failed.",
                            "  The folder at \"/folder/to/clean/outputs/\" doesn't exist."),
                        process.getOutputWriteStream());
                    test.assertLinesEqual(
                        Iterable.create(),
                        process.getErrorWriteStream());
                    test.assertEqual(0, process.getExitCode());
                    test.assertTrue(outputsFolder.exists().await());
                    test.assertTrue(process.getQubProjectDataFolder().await().exists().await());
                });
            });
        });
    }
}
