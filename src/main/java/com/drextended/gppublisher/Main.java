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
                    config.useJsonKeyInFile,
                    config.jsonKeyPath,
                    config.jsonKeyContent,
                    config.artifactPath,
                    config.deobfuscationFilePath,
                    config.listings,
                    config.track,
                    config.rolloutFraction,
                    config.trackCustoms
            );
            helper.init();
            helper.makeInsertRequest();

            logger.info(
                    "\n=============================================\n" +
                            "=             !!! PUBLISHED !!!             =\n" +
                            "=============================================\n"
            );
        } catch (IOException | GeneralSecurityException e) {
            logger.error("Exception was thrown while uploading apk to " + config.track + " track", e);
        }

    }

    private static class ConfigArgs {

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

        @Parameter(names = {"-sa", "-serviceAccountEmail"}, required = true, description = "The service account email")
        public String serviceAccountEmail;

        @Parameter(names = {"-k", "-jsonKey"}, required = false, description = "The service account key.json file path. Required unless you specify -jsonKeyContent param")
        public String jsonKeyPath;

        @Parameter(names = {"-ks", "-jsonKeyContent"}, required = false, description = "The service account key.json file content as text")
        public String jsonKeyContent;

        @Parameter(names = {"-a", "-apk", "-aab"}, required = true, description = "The file path to the apk/aab artifact")
        public String artifactPath;

        @Parameter(names = {"-df", "-deobfuscationFile"}, required = false, description = "The file path to the deobfuscation file of the specified apk/aab")
        public String deobfuscationFilePath;

        @Parameter(
                names = {"-l", "-listings"},
                required = false,
                description = "The file path to recent changes in format: [BCP47 Language Code]:[recent changes file path]. " +
                        "Multiple listing thought comma. Sample: en-US:C:\\temp\\listing_en.txt"
        )
        public String listings;

        @Parameter(names = {"-T", "-tracks"}, required = true, description = "Comma separated track names")
        public String trackCustoms;

        @Parameter(names = {"-t", "-track"}, required = true, description = "The single track for uploading the apk, can be \"internal\", \"alpha\", \"beta\", \"production\" or \"rollout\"")
        public String track;

        @Parameter(names = {"-fraction"}, required = true, description = "The rollout fraction")
        public String rolloutFraction;

        public transient File workingDir;

        public transient boolean useJsonKeyInFile;

        public void prepare() throws ParameterException {
            try {
                URI uri = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI();
                workingDir = new File(uri);
                useJsonKeyInFile = jsonKeyPath != null && !jsonKeyPath.isEmpty();
            } catch (URISyntaxException e) {
                throw new ParameterException("Invalid working directory");
            }

            if (jsonKeyPath == null && jsonKeyContent == null) {
                throw new ParameterException("Required parameter missing: you should specify -jsonKey or -jsonKeyContent");
            }
        }
    }
}
