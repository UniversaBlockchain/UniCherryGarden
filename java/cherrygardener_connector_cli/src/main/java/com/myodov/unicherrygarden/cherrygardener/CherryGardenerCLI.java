package com.myodov.unicherrygarden.cherrygardener;

import com.myodov.unicherrygarden.api.types.dlt.Currency;
import com.myodov.unicherrygarden.cherrygardener.connector.api.ClientConnector;
import com.myodov.unicherrygarden.cherrygardener.connector.api.Observer;
import com.myodov.unicherrygarden.cherrygardener.connector.impl.ClientConnectorImpl;
import org.apache.commons.cli.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.CompletionException;


/**
 * Command Line Interface frontend to CherryGardener Connector API.
 */
public class CherryGardenerCLI {
    private static final Logger logger = LoggerFactory.getLogger(CherryGardenerCLI.class);

    private static final Options options = new Options();

    static {
        options.addOption(
                "h", "help", false,
                "display help");
        options.addOption(
                "c", "connect", true,
                "comma-separated list of addresses to connect; e.g. \"127.0.0.1:2551,127.0.0.1:2552\"");
        options.addOption(
                null, "list-supported-currencies", false,
                "print the list of currencies supported by CherryGardener");
        options.addOption(
                null, "list-tracked-addresses", false,
                "print the list of addresses tracked by CherryPicker");
//        options.addOption("l", "launch", false, "launch the CherryGardener service and stay running");
    }

    protected static final Properties propsCLI = loadPropsFromNamedResource("cherrygardener_connector_cli.properties");
    protected static final Properties propsConnector = loadPropsFromNamedResource("cherrygardener_connector.properties");

    /**
     * Load {@link Properties} from a .properties-formatted file in the artifact resources.
     */
    @NonNull
    private static final Properties loadPropsFromNamedResource(@NonNull String resourceName) {
        final String resourcePath = "unicherrygarden/" + resourceName;
        final Properties props = new Properties();
        final @Nullable InputStream resourceAsStream = CherryGardenerCLI.class.getClassLoader().getResourceAsStream(resourcePath);
        if (resourceAsStream == null) {
            System.err.printf("Cannot load resource from %s\n", resourcePath);
        } else {
            try {
                props.load(resourceAsStream);
            } catch (IOException e) {
                System.err.printf("Cannot load properties from %s\n", resourcePath);
                throw new RuntimeException(String.format("Cannot load properties file from %s", resourcePath));
            }
        }
        return props;
    }

    private Optional<List<String>> parseConnectUrls(@NonNull CommandLine line) {
        if (line.hasOption("connect")) {
            final String optUnstripped = line.getOptionValue("connect");
            final String optStripped = optUnstripped.replaceAll("^\\s+|\\s+$", ""); // TODO Since Java 11: optUnstripped.strip()
            final List<String> connectUrls = Arrays.asList(optStripped.split(","));
            if (connectUrls.isEmpty()) {
                System.err.println("--connect option must be non-empty! " +
                        "Recommended 2 or more URLs, like \"127.0.0.1:2551,127.0.0.1:2552\".");
                return Optional.empty();
            } else {
                return Optional.of(connectUrls);
            }
        } else {
            System.err.println("--connect option must be present!");
            return Optional.empty();
        }
    }

    /**
     * Constructor: analyze the CLI arguments and act accordingly.
     */
    public CherryGardenerCLI(@NonNull String[] args) {
        final CommandLineParser parser = new DefaultParser();
        try {
            final CommandLine line = parser.parse(options, args);
            if (line.hasOption("help")) {
                printHelp();
            } else if (line.hasOption("list-supported-currencies")) {
                printTitle(System.err);

                System.err.println("Listing supported currencies...");

                final Optional<List<String>> connectUrls = parseConnectUrls(line);
                if (connectUrls.isPresent()) {
                    try {
                        final ClientConnector connector = new ClientConnectorImpl(connectUrls.get());
                        @Nullable final List<Currency> currencies = connector.getCurrencies();
                        if (currencies == null) {
                            System.err.println("Could not get the supported currencies");
                        } else {
                            System.err.println("Received list of currencies:");
                            for (final Currency c : currencies) {
                                final @Nullable String optComment = c.getComment();
                                System.err.printf("  %s: \"%s\" - %s%s\n",
                                        c.getSymbol(),
                                        c.getName(),
                                        (c.getCurrencyType() == Currency.CurrencyType.ETH) ?
                                                "Ether cryptocurrency" :
                                                String.format("ERC20 token at %s", c.getDAppAddress()),
                                        (optComment == null) ? "" : String.format(" (%s)", optComment)
                                );
                            }
                            connector.shutdown();
                        }
                    } catch (CompletionException exc) {
                        System.err.println("Could not connect to UniCherryGarden!");
                    }
                }
            } else if (line.hasOption("list-tracked-addresses")) {
                printTitle(System.err);

                System.err.println("Listing tracked addresses...");

                final Optional<List<String>> connectUrls = parseConnectUrls(line);
                if (connectUrls.isPresent()) {
                    try {
                        final ClientConnector connector = new ClientConnectorImpl(connectUrls.get());
                        final Observer observer = connector.getObserver();

                        @Nullable final List<@NonNull String> trackedAddresses = observer.getTrackedAddresses();
                        if (trackedAddresses == null) {
                            System.err.println("Could not get the tracked addresses");
                        } else {
                            for (final String addr : trackedAddresses) {
                                System.err.printf("  %s\n", addr);
                            }
                        }
                        connector.shutdown();
                    } catch (CompletionException exc) {
                        System.err.println("Could not connect to UniCherryGarden!");
                        System.err.printf("%s\n", exc);
                    }
                }
            } else {
                printHelp();
            }
        } catch (ParseException exp) {
            System.err.printf("Parsing failed. Reason: %s\n", exp.getMessage());
            printHelp();
        }
    }

    private static final void printTitle(@NonNull PrintStream printStream) {
        final String header = String.format("CherryGardener CLI v. %s", propsCLI.getProperty("version"));
        final String underline = String.join(
                "",
                Collections.nCopies(header.length(), "-")
        );
        printStream.printf("" +
                        "%s\n" + // CherryGardener CLI v. 1.23...
                        "%s\n" + // -----
                        "CLI: version %s, built at %s\n" +
                        "CherryGardener connector: version %s, built at %s\n",
                header,
                underline,
                propsCLI.getProperty("version"), propsCLI.getProperty("build_timestamp"),
                propsConnector.getProperty("version"), propsConnector.getProperty("build_timestamp")
        );
    }

    private static final void printHelp() {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar cherrygardener", options);
    }

    public static void main(String[] args) {
        new CherryGardenerCLI(args);
    }
}
