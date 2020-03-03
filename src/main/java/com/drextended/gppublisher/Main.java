package com.drextended.gppublisher;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.internal.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.List;

public class Main {

    private static final Log logger = LogFactory.getLog(Main.class);

    public static void main(String[] args) {

        ConfigArgs config = new ConfigArgs();
        JCommander jCommander = new JCommander(config);
        jCommander.setProgramName("Google Play Publisher");
        try {
            jCommander.parse(args);

            if (config.isHelp) {
                jCommander.usage();
                System.exit(0);
            }

            config.prepare();

        } catch (ParameterException e) {
            System.err.println(e.getMessage());
            jCommander.usage();
            System.exit(1);
        }

        try {
            logger.info("Start deploy task for app " + config.applicationName);

            AndroidPublishRequestWrapper helper = new AndroidPublishRequestWrapper(
                    config.workingDir,
                    config.applicationName,
                    config.packageName,
                    config.useJsonKeyAsFile,
                    config.jsonKey,
                    config.artifactPath,
                    config.deobfuscationFilePath,
                    config.listings,
                    config.tracks,
                    config.rolloutFraction
            );
            helper.init();
            helper.makeInsertRequest();

            logger.info("\n=============================================\n" +
                    "=             !!! PUBLISHED !!!             =\n" +
                    "=============================================\n"
            );
        } catch (IOException | GeneralSecurityException e) {
            logger.error("Exception was thrown while uploading apk", e);
        }

    }

    private static class ConfigArgs {

        @SuppressWarnings("unused")
        @Parameter
        public List<String> parameters = Lists.newArrayList();

        @Parameter(names = "-help", help = true, description = "Print usage")
        public boolean isHelp;

        @Parameter(names = {"-n", "-appName"}, required = true, description = "The name of your application. " +
                "If the application name is null or blank, the application will log a warning. " +
                "Suggested format is \"MyCompany-Application/1.0\".")
        public String applicationName;

        @Parameter(names = {"-p", "-packageName"}, required = true, description = "The package name of the app")
        public String packageName;

        @Parameter(names = {"-k", "-jsonKey"}, required = true, description = "The service account key.json file path or file content as text")
        public String jsonKey;

        @Parameter(names = {"-a", "-apk", "-aab"}, required = true, description = "The file path to the apk/aab artifact")
        public String artifactPath;

        @Parameter(names = {"-df", "-deobfuscation"}, description = "The file path to the deobfuscation file of the specified apk/aab")
        public String deobfuscationFilePath;

        @Parameter(
                names = {"-l", "-listings"},
                description = "The file path to recent changes in format: [BCP47 Language Code]:[recent changes file path]. " +
                        "Multiple listing thought comma. Sample: en-US:C:\\temp\\listing_en.txt"
        )
        public String listings;

        @Parameter(
                names = {"-t", "-tracks"},
                variableArity = true,
                description = "Comma separated track names for assigning artifact. " +
                        "Can be \"internal\", \"alpha\", \"beta\", \"production\", \"rollout\" or any custom. " +
                        "If not set - artifact will not be assigned to any tracks." )
        public List<String> tracks;

        @Parameter(names = {"-fraction"}, description = "The rollout fraction. Acceptable values are 0.05, 0.1, 0.2, and 0.5")
        public Double rolloutFraction;

        public transient File workingDir;

        public transient boolean useJsonKeyAsFile;

        public void prepare() throws ParameterException {
            try {
                URI uri = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI();
                workingDir = new File(uri);
                useJsonKeyAsFile = jsonKey != null && !jsonKey.trim().startsWith("{");
            } catch (URISyntaxException e) {
                throw new ParameterException("Invalid working directory");
            }
        }
    }
}
