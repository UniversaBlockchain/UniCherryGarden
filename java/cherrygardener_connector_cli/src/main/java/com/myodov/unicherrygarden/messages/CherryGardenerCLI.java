package com.myodov.unicherrygarden.messages;

import com.myodov.unicherrygarden.api.types.dlt.Currency;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import com.myodov.unicherrygarden.messages.cherrypicker.AddTrackedAddresses;
import com.myodov.unicherrygarden.messages.connector.api.ClientConnector;
import com.myodov.unicherrygarden.messages.connector.api.Observer;
import com.myodov.unicherrygarden.messages.connector.impl.ClientConnectorImpl;
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
        final OptionGroup commandOptionGroup = new OptionGroup();

        commandOptionGroup.addOption(new Option(
                "h", "help", false,
                "Display help."));
        commandOptionGroup.addOption(new Option(
                null, "list-supported-currencies", false,
                "Print the list of currencies supported by CherryGardener and all other components."));
        commandOptionGroup.addOption(new Option(
                "lta", "list-tracked-addresses", false,
                "Print the list of addresses tracked by CherryPicker."));
        commandOptionGroup.addOption(new Option(
                "ata", "add-tracked-address", true,
                "Add an Ethereum address to track.\n" +
                        "Should be lowercased and valid Ethereum address;\n" +
                        "e.g. \"0x884191033518be08616821d7676ca01695698451\".\n" +
                        "See also:\n" +
                        "--track-from-block (mandatory),\n" +
                        "--comment (optional)."));
        commandOptionGroup.setRequired(true);

        options.addOption(
                "c", "connect", true,
                "Comma-separated list of addresses to connect;\n" +
                        "e.g. \"127.0.0.1:2551,127.0.0.1:2552\".");
        options.addOptionGroup(commandOptionGroup);

        options.addOption(
                null, "comment", true,
                "Text comment."
        );
        options.addOption(
                null, "track-from-block", true,
                "How to choose a block from which to track the address.\n" +
                        "Values (enter one of):\n" +
                        "* <BLOCK_NUMBER> – integer number of block, e.g. \"4451131\";\n" +
                        "* LATEST_KNOWN – latest block known to Ethereum node." /* + "\n" +
                        "* LATEST_NODE_SYNCED – latest block fully synced by Ethereum node (available to the Ethereum node);\n" +
                        "* LATEST_CHERRYGARDEN_SYNCED – latest block fully synced by UniCherryGarden." */
        );
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
                System.err.println("WARNING: --connect option must be non-empty! " +
                        "Recommended 2 or more URLs, like \"127.0.0.1:2551,127.0.0.1:2552\".");
                return Optional.empty();
            } else {
                return Optional.of(connectUrls);
            }
        } else {
            System.err.println("WARNING: --connect option must be present!");
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
                if (!connectUrls.isPresent()) {
                    System.err.println("WARNING: --connect option is mandatory!");
                } else {
                    try {
                        final ClientConnector connector = new ClientConnectorImpl(connectUrls.get());
                        @Nullable final List<Currency> currencies = connector.getCurrencies();
                        if (currencies == null) {
                            System.err.println("ERROR: Could not get the supported currencies!");
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
                        System.err.println("ERROR: Could not connect to UniCherryGarden!");
                    }
                }
            } else if (line.hasOption("list-tracked-addresses")) {
                printTitle(System.err);

                System.err.println("Listing tracked addresses...");

                final Optional<List<String>> connectUrls = parseConnectUrls(line);
                if (!connectUrls.isPresent()) {
                    System.err.println("WARNING: --connect option is mandatory!");
                } else {
                    try {
                        final ClientConnector connector = new ClientConnectorImpl(connectUrls.get());
                        final Observer observer = connector.getObserver();

                        @Nullable final List<@NonNull String> trackedAddresses = observer.getTrackedAddresses();
                        if (trackedAddresses == null) {
                            System.err.println("ERROR: Could not get the tracked addresses!");
                        } else {
                            for (final String addr : trackedAddresses) {
                                System.err.printf("  %s\n", addr);
                            }
                        }
                        connector.shutdown();
                    } catch (CompletionException exc) {
                        System.err.println("ERROR: Could not connect to UniCherryGarden!");
                        System.err.printf("%s\n", exc);
                    }
                }
            } else if (line.hasOption("add-tracked-address")) {
                printTitle(System.err);

                final String address = line.getOptionValue("add-tracked-address");
                @Nullable final String trackFromBlockStr = line.getOptionValue("track-from-block");
                @Nullable final String commentOpt = line.getOptionValue("comment");

                // If null, it means that validation failed
                final AddTrackedAddresses.@Nullable StartTrackingAddressMode trackFromBlockMode;
                final int trackFromBlock; // 0 if any mode is different from FROM_BLOCK.
                final boolean argValidationSuccess;
                {
                    if (!EthUtils.Addresses.isValidLowercasedAddress(address)) {
                        System.err.println("" +
                                "WARNING: --add-tracked-address option should contain " +
                                "a valid lowercased Ethereum address!");
                        trackFromBlockMode = null;
                        trackFromBlock = 0; // dummy
                        argValidationSuccess = false;
                    } else if (trackFromBlockStr == null) {
                        System.err.println("WARNING: --track-from-block option is mandatory!");
                        trackFromBlockMode = null;
                        trackFromBlock = 0;
                        argValidationSuccess = false;
                    } else {
                        switch (trackFromBlockStr.toUpperCase()) {
                            case "LATEST_KNOWN":
                                trackFromBlockMode = AddTrackedAddresses.StartTrackingAddressMode.LATEST_KNOWN_BLOCK;
                                trackFromBlock = 0; // dummy
                                argValidationSuccess = true;
                                break;
                            default:
                                int blockNumberCandidate;
                                boolean success;
                                try {
                                    blockNumberCandidate = Integer.parseUnsignedInt(trackFromBlockStr);
                                    success = true;
                                } catch (NumberFormatException e) {
                                    System.err.println("" +
                                            "WARNING: --track-from-block option should contain one of the supported constants, " +
                                            "or non-negative block number!");
                                    blockNumberCandidate = 0;
                                    success = false;
                                }
                                assert blockNumberCandidate >= 0 : blockNumberCandidate;
                                trackFromBlockMode = AddTrackedAddresses.StartTrackingAddressMode.FROM_BLOCK;
                                trackFromBlock = blockNumberCandidate;
                                argValidationSuccess = success;
                                break;
                        }
                    }
                }

                if (argValidationSuccess) {

                    System.err.printf("Adding an address %s with %s; tracking from %s, %s...\n",
                            address,
                            (commentOpt == null) ? "no comment" : String.format("comment \"%s\"", commentOpt),
                            trackFromBlockMode,
                            trackFromBlock
                    );

                    final Optional<List<String>> connectUrls = parseConnectUrls(line);
                    if (!connectUrls.isPresent()) {
                        System.err.println("WARNING: --connect option is mandatory!");
                    } else {
                        try {
                            final ClientConnector connector = new ClientConnectorImpl(connectUrls.get());
                            final Observer observer = connector.getObserver();

                            final boolean success = observer.startTrackingAddress(
                                    address,
                                    trackFromBlockMode,
                                    trackFromBlock,
                                    commentOpt);
                            if (success) {
                                System.err.printf("Address %s successfully added!\n");
                            } else {
                                System.err.printf("ERROR: Address %s failed to add!\n");
                            }

                            connector.shutdown();
                        } catch (CompletionException exc) {
                            System.err.println("ERROR: Could not connect to UniCherryGarden!");
                            System.err.printf("%s\n", exc);
                        }
                    }
                }
            } else {
                printHelp();
            }
        } catch (ParseException exp) {
            System.err.printf("ERROR: Parsing failed. Reason: %s\n", exp.getMessage());
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
        formatter.setOptionComparator(null); // don’t sort the options
        formatter.printHelp("java -jar cherrygardener", options);
    }

    public static void main(String[] args) {
        new CherryGardenerCLI(args);
    }
}
