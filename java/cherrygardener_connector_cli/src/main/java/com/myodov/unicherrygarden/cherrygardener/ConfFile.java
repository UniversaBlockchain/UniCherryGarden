package com.myodov.unicherrygarden.cherrygardener;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The API to the CLI conf file access.
 * <p>
 * The file should be a HOCON conf file, and located in <code>~/.unicherrygarden/cli.conf</code>.
 */
public class ConfFile {
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
     * @param cliParamName the name of parameter, as can be given in CLI (e.g. "--connect").
     * @param configPath   the path to the parameter in HOCON file (e.g. "connect.urls").
     * @param validator    (optional) validator that tests the candidate before returning;
     *                     if validation fails, it is the validator’s own duty to print the explanation to STDERR,
     *                     if needed.
     * @implNote prints errors to STDERR.
     */
    @NonNull
    public final <RES> Optional<RES> getParamFromCliOrConfig(
            @NonNull String cliParamName,
            @NonNull String configPath,
            @NonNull Function<String, RES> getter,
            @Nullable Predicate<RES> validator
    ) {
        final @NonNull RES candidate;
        try {
            candidate = getter.apply(configPath);

            if (validator != null && !validator.test(candidate)) {
                return Optional.empty();
            }
        } catch (ConfigException.Missing e) {
            // No setting but this is okay, no error
            System.err.printf("Note: instead of %s CLI argument, you can put \"%s\" setting in %s HOCON conf file.\n",
                    cliParamName, configPath, confPath);
            return Optional.empty();
        } catch (ConfigException.WrongType e) {
            System.err.printf("`%s` setting in your conf file is of wrong type!\n",
                    configPath);
            return Optional.empty();
        }

        return Optional.of(candidate);
    }


    /**
     * @implNote prints errors to STDERR.
     */
    @NonNull
    public final Optional<List<String>> getConnectUrls() {
        return getParamFromCliOrConfig(
                "--connect",
                "connect.urls",
                config::getStringList,
                null
        );
    }

    /**
     * @implNote prints errors to STDERR.
     */
    @NonNull
    public final Optional<Integer> getListenPort() {
        return getParamFromCliOrConfig(
                "--listen-port",
                "listen.port",
                config::getInt,
                (candidate) -> {
                    if (!(0 <= candidate && candidate <= 65535)) {
                        System.err.println("\"listen.port\" setting in your conf file must be a port in [0..65535] range!");
                        return false;
                    } else {
                        return true;
                    }
                }
        );
    }

    /**
     * @implNote prints errors to STDERR.
     */
    @NonNull
    public final Optional<String> getRealm() {
        return getParamFromCliOrConfig(
                "--realm",
                "connect.realm",
                config::getString,
                (candidate) -> {
                    if (candidate.matches("^[-_a-zA-Z0-9]*$")) {
                        return true;
                    } else {
                        System.err.println("\"connect.realm\" setting can contain only latin letters, digits, \"-\" or \"_\" sign.");
                        return false;
                    }
                }
        );
    }

    /**
     * @implNote prints errors to STDERR.
     */
    @NonNull
    public final Optional<Long> getChainId() {
        return getParamFromCliOrConfig(
                "--chain-id",
                "blockchain.chain_id",
                config::getLong,
                (candidate) -> {
                    if (candidate == -1) {
                        System.err.println("Chain ID value of -1 means None, but you probably want a real Chain ID " +
                                "for EIP-155 compatibility. But using it for now.");
                        return true;
                    } else if (candidate < 1) {
                        System.err.println("Chain ID value must be >=1; may be -1 but that means None " +
                                "and is not recommended.");
                        return false;
                    } else {
                        return true;
                    }
                }
        );
    }
}
