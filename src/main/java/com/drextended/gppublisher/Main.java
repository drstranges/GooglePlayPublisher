package com.drextended.gppublisher;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.internal.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

public class Main {

    private static final Log log = LogFactory.getLog(Main.class);

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

        } catch (ParameterException e) {
            System.err.println(e.getMessage());
            jCommander.usage();
            System.exit(1);
        }

        try {
            UploadApkUtils.uploadApk(
                    config.applicationName,
                    config.packageName,
                    config.serviceAccountEmail,
                    config.p12KeyPath,
                    config.apkPath,
                    config.track);
        } catch (IOException | GeneralSecurityException ex) {
            log.error("Exception was thrown while uploading apk to " + config.track + " track", ex);
        }
    }

    private static class ConfigArgs {

        @Parameter
        public List<String> parameters = Lists.newArrayList();

        @Parameter(names = "-help", help = true, description = "Print usage")
        public boolean isHelp;

        @Parameter(names = {"-n", "-applicationName"}, required = true, description = "The name of your application. " +
                "If the application name is null or blank, the application will log a warning. " +
                "Suggested format is \"MyCompany-Application/1.0\".")
        public String applicationName;

        @Parameter(names = {"-p", "-packageName"}, required = true, description = "The package name of the app")
        public String packageName;

        @Parameter(names = {"-a", "-serviceAccountEmail"}, required = true, description = "The service account email")
        public String serviceAccountEmail;

        @Parameter(names = {"-k", "-p12KeyPath"}, required = true, description = "The service account key.p12 file path")
        public String p12KeyPath;

        @Parameter(names = "-apk", required = true, description = "The apk file path of the apk to upload")
        public String apkPath;

        @Parameter(names = {"-t", "-track"}, required = true, description = "The track for uploading the apk, can be \"alpha\", \"beta\", \"production\" or \"rollout\"")
        public String track;
    }
}
