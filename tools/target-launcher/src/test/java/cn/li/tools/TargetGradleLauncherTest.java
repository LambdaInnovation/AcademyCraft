package cn.li.tools;

import java.nio.file.Path;
import java.util.List;

/** Dependency-free regression coverage, invoked by the Gradle testLauncher task. */
public final class TargetGradleLauncherTest {
    public static void main(String[] args) {
        sameNamedComponentDoesNotShadowTarget();
        onlyDirectTargetFieldsAreAccepted();
        unknownTargetIsRejected();
        windowsBatchWrapperUsesCommandProcessor();
        requestedTaskDoesNotAddPlatformBuild();
        optionsOnlyDefaultToPlatformBuild();
        System.out.println("TargetGradleLauncher regression tests passed.");
    }

    private static void sameNamedComponentDoesNotShadowTarget() {
        String json = """
                {
                  "buildProfiles": {
                    "profile-17": {
                      "wrapper": "root",
                      "gradleJvmVersion": 21
                    }
                  },
                  "components": {
                    "forge-1.20.1": {
                      "kind": "loader",
                      "source": "platform-src/loader/forge-1.20.1/src/main"
                    }
                  },
                  "targets": {
                    "forge-1.20.1": {
                      "id": "forge-1.20.1",
                      "buildProfile": "profile-17",
                      "gradleJvmVersion": 21,
                      "artifact": {
                        "classifier": "forge-1.20.1"
                      }
                    }
                  }
                }
                """;

        TargetGradleLauncher.TargetModel model =
                TargetGradleLauncher.targetModelFromJson(json, "forge-1.20.1");
        assertEquals(21, model.gradleJvmVersion(), "gradle JVM version");
        assertEquals("root", model.wrapper(), "wrapper");
    }

    private static void onlyDirectTargetFieldsAreAccepted() {
        String json = """
                {
                  "buildProfiles": {
                    "profile-17": {"wrapper": "root"}
                  },
                  "targets": {
                    "nested-only": {
                      "buildProfile": "profile-17",
                      "metadata": {"gradleJvmVersion": 21}
                    }
                  }
                }
                """;

        assertThrows(
                "Target has no gradleJvmVersion: nested-only",
                () -> TargetGradleLauncher.targetModelFromJson(json, "nested-only"));
    }

    private static void unknownTargetIsRejected() {
        String json = """
                {
                  "buildProfiles": {},
                  "components": {"missing": {"gradleJvmVersion": 99}},
                  "targets": {}
                }
                """;

        assertThrows(
                "Unknown target in platform-catalog.json: missing",
                () -> TargetGradleLauncher.targetModelFromJson(json, "missing"));
    }

    private static void windowsBatchWrapperUsesCommandProcessor() {
        List<String> command = TargetGradleLauncher.wrapperCommand(
                Path.of("C:\\repo with spaces\\gradlew.bat"),
                List.of(":platform:build", "-PplatformTarget=forge-1.20.1"),
                true,
                "C:\\Windows\\System32\\cmd.exe");

        assertEquals(
                List.of(
                        "C:\\Windows\\System32\\cmd.exe",
                        "/d",
                        "/c",
                        "C:\\repo with spaces\\gradlew.bat",
                        ":platform:build",
                        "-PplatformTarget=forge-1.20.1"),
                command,
                "Windows wrapper command");
    }

    private static void requestedTaskDoesNotAddPlatformBuild() {
        List<String> arguments = TargetGradleLauncher.gradleArguments(
                Path.of("C:\\repo"),
                "forge-1.20.1",
                new String[] {"forge-1.20.1", ":platform:runData", "--dry-run"},
                1);

        assertEquals(
                List.of(
                        "--gradle-user-home",
                        "C:\\repo\\build\\gradle-home\\forge-1.20.1",
                        "-PplatformTarget=forge-1.20.1",
                        ":platform:runData",
                        "--dry-run"),
                arguments,
                "requested task arguments");
    }

    private static void optionsOnlyDefaultToPlatformBuild() {
        List<String> arguments = TargetGradleLauncher.gradleArguments(
                Path.of("C:\\repo"),
                "forge-1.20.1",
                new String[] {"forge-1.20.1", "--dry-run"},
                1);

        assertEquals(
                List.of(
                        "--gradle-user-home",
                        "C:\\repo\\build\\gradle-home\\forge-1.20.1",
                        "-PplatformTarget=forge-1.20.1",
                        "--dry-run",
                        ":platform:build"),
                arguments,
                "default build arguments");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertThrows(String expectedMessage, Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException ex) {
            assertEquals(expectedMessage, ex.getMessage(), "exception message");
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException: " + expectedMessage);
    }
}
