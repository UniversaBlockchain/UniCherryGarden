package com.myodov.unicherrygarden.cherrygardener;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * The API to the CLI conf file access.
 * <p>
 * The file should be a HOCON conf file, and located in <code>~/.unicherrygarden/cli.conf</code>.
 */
public class ConfFile {
    private static final Logger logger = LoggerFactory.getLogger(ConfFile.class);

    private final Path confPath = Paths.get(
            System.getProperty("user.home"),
            ".unicherrygarden",
            "cli.conf");

    private final Config config = ConfigFactory.parseFile(confPath.toFile());

    /**
     * Constructor.
     */
    public ConfFile() {
    }

    /**
     * Note: prints errors to STDERR.
     */
    @NonNull
    public Optional<List<String>> getConnectUrls() {
        final @NonNull List<String> candidate;
        try {
            candidate = config.getStringList("connect.urls");
        } catch (ConfigException.Missing e) {
            // No setting but this is okay, no error
            System.err.printf("Note: instead of --connect CLI argument, you can put \"connect.urls\" setting in %s HOCON conf file.\n", confPath);
            return Optional.empty();
        } catch (ConfigException.WrongType e) {
            System.err.println("`connect.urls` setting in your conf file is of wrong type!");
            return Optional.empty();
        }

        return Optional.of(candidate);
    }

    /**
     * Note: prints errors to STDERR.
     */
    @NonNull
    public Optional<Integer> getListenPort() {
        final int candidate;
        try {
            candidate = config.getInt("listen.port");
            if (!(0 <= candidate && candidate <= 65535)) {
                System.err.println("\"listen.port\" setting in your conf file must be a port in [0..65535] range!");
                return Optional.empty();
            }
        } catch (ConfigException.Missing e) {
            // No setting but this is okay, no error
            System.err.printf("Note: instead of --listen-port CLI argument, you can put \"listen.port\" setting in %s HOCON conf file.\n", confPath);
            return Optional.empty();
        } catch (ConfigException.WrongType e) {
            System.err.println("`listen.port` setting in your conf file is of wrong type!");
            return Optional.empty();
        }

        return Optional.of(candidate);
    }
}
