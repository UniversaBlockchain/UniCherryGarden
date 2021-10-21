package com.myodov.unicherrygarden.cherrygardener;

import com.myodov.unicherrygarden.api.types.dlt.Currency;
import com.myodov.unicherrygarden.connector.api.ClientConnector;
import com.myodov.unicherrygarden.connector.api.Observer;
import com.myodov.unicherrygarden.connector.impl.ClientConnectorImpl;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import com.myodov.unicherrygarden.messages.cherrypicker.AddTrackedAddresses;
import com.myodov.unicherrygarden.messages.cherrypicker.GetBalances;
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
                "gc", "get-currencies", false,
                "Print the list of currencies supported by CherryGardener and all other components."));
        commandOptionGroup.addOption(new Option(
                "gta", "get-tracked-addresses", false,
                "Print the list of addresses tracked by CherryPicker."));
        commandOptionGroup.addOption(new Option(
                "ata", "add-tracked-address", true,
                "Add an Ethereum address to track.\n" +
                        "Should be a valid Ethereum address;\n" +
                        "e.g. \"0x884191033518be08616821d7676ca01695698451\".\n" +
                        "See also:\n" +
                        "--track-from-block (mandatory),\n" +
                        "--comment (optional)."));
        commandOptionGroup.addOption(new Option(
                "gb", "get-balances", true,
                "Get balances (of currencies tokens) for an (already tracked) Ethereum address.\n" +
                        "Should be a valid Ethereum address;\n" +
                        "e.g. \"0x884191033518be08616821d7676ca01695698451\".\n" +
                        "See also:\n" +
                        "--confirmations (optional; default: 0)."));
        commandOptionGroup.setRequired(true);

        options.addOption(
                "c", "connect", true,
                "Comma-separated list of addresses to connect;\n" +
                        "e.g. \"127.0.0.1:2551,127.0.0.1:2552\".");
        options.addOption(
                null, "confirmations", true,
                "The number of confirmations (Ethereum blocks already mined after an event);\n" +
                        "Should be a non-negative integer number, likely 6 or 12.");
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

    /**
     * Parses the "--connect" option assuming it is mandatory,
     * printing all necessary warnings in the process.
     *
     * @return non-empty {@link Optional<>} with the list of strings (connect URLs) if the URLs are properly parsed;
     * “empty” optional if parsing failed (and all necessary warnings were printed).
     */
    private static Optional<List<String>> parseConnectUrls(@NonNull CommandLine line) {
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
     * Parse an option (with the name of the option passed as the "optionName" argument)
     * that should contain an Ethereum address,
     * printing all necessary warnings in the process.
     *
     * @param optionName The name of the option to parse.
     * @return non-empty {@link Optional<>} with the parsed (lowercased) Ethereum address
     * if it has been properly parsed;
     * “empty” optional if parsing failed (and all required warnings were printed).
     */
    private static final Optional<String> parseEthereumAddressOption(@NonNull CommandLine line,
                                                                     @NonNull String optionName) {
        @NonNull final String address = line.getOptionValue(optionName).toLowerCase();
        if (!EthUtils.Addresses.isValidLowercasedAddress(address)) {
            System.err.printf(
                    "WARNING: --%s option should contain a valid Ethereum address!",
                    optionName);
            return Optional.empty();
        } else {
            return Optional.of(address);
        }
    }


    static class TrackFromBlockOption {
        final AddTrackedAddresses.@NonNull StartTrackingAddressMode mode;
        @Nullable
        final Integer block;

        /**
         * Constructor.
         */
        private TrackFromBlockOption(AddTrackedAddresses.@NonNull StartTrackingAddressMode mode,
                                     @Nullable Integer block) {
            assert (mode == AddTrackedAddresses.StartTrackingAddressMode.FROM_BLOCK) == (block != null)
                    :
                    String.format("%s:%s", mode, block);
            assert (block == null) || (block.intValue() >= 0) : block;
            this.mode = mode;
            this.block = block;
        }

        /**
         * Create the {@link TrackFromBlockOption} when you know the specific number of block.
         */
        static TrackFromBlockOption fromSpecificBlock(int block) {
            assert block >= 0 : block;
            return new TrackFromBlockOption(AddTrackedAddresses.StartTrackingAddressMode.LATEST_KNOWN_BLOCK, block);
        }

        /**
         * Create the {@link TrackFromBlockOption} when you autodetect the number of the block.
         *
         * @param mode The mode of detection.
         *             Use any mode except {@link AddTrackedAddresses.StartTrackingAddressMode#FROM_BLOCK}
         *             (if you want to use a specific block number, use {@link #fromSpecificBlock(int)} method instead).
         */
        static TrackFromBlockOption fromAutoDetectedBlock(AddTrackedAddresses.@NonNull StartTrackingAddressMode mode) {
            assert mode != null && mode != AddTrackedAddresses.StartTrackingAddressMode.FROM_BLOCK: mode;
            return new TrackFromBlockOption(mode,null);
        }
    }

    /**
     * Parse "--track-from-block" option to the result data (as an {@link TrackFromBlockOption} object),
     * printing all necessary warnings in the process.
     *
     * @return non-empty {@link Optional<>} with the parsed "track-from-block" information
     * if it has been properly parsed;
     * “empty” optional if parsing failed (and all required warnings were printed).
     */
    private static Optional<TrackFromBlockOption> parseTrackFromBlock(@NonNull CommandLine line) {
        assert line != null;

        final @Nullable String trackFromBlockStr = line.getOptionValue("track-from-block");
        if (trackFromBlockStr == null) {
            System.err.println("WARNING: --track-from-block option is mandatory!");
            return Optional.empty();
        } else {
            switch (trackFromBlockStr.toUpperCase()) {
                case "LATEST_KNOWN":
                    return Optional.of(TrackFromBlockOption.fromAutoDetectedBlock(
                            AddTrackedAddresses.StartTrackingAddressMode.LATEST_KNOWN_BLOCK));
                case "LATEST_NODE_SYNCED":
                    return Optional.of(TrackFromBlockOption.fromAutoDetectedBlock(
                            AddTrackedAddresses.StartTrackingAddressMode.LATEST_NODE_SYNCED_BLOCK));
                case "LATEST_CHERRYGARDEN_SYNCED":
                    return Optional.of(TrackFromBlockOption.fromAutoDetectedBlock(
                            AddTrackedAddresses.StartTrackingAddressMode.LATEST_CHERRYGARDEN_SYNCED_BLOCK));
                default:
                    try {
                        final int blockNumberCandidate = Integer.parseUnsignedInt(trackFromBlockStr);
                        assert blockNumberCandidate >= 0 : blockNumberCandidate;
                        return Optional.of(TrackFromBlockOption.fromSpecificBlock(blockNumberCandidate));
                    } catch (NumberFormatException e) {
                        System.err.println("" +
                                "WARNING: --track-from-block option should contain one of the supported constants, " +
                                "or non-negative block number!");
                        return Optional.empty();
                    }
            }
        }
    }

    /**
     * Parse "--confirmations" option,
     * printing all necessary warnings in the process.
     *
     * @return non-empty {@link Optional<>} with the parsed number of confirmations,
     * if it has been properly parsed;
     * “empty” optional if parsing failed (and all required warnings were printed).
     * If the optional parameter is missing in the command line,
     * the result is dependent on the "_default" argument:
     * if _default is <code>null</code>,
     * the optional will be assumed "empty" (i.e. parsing failed);
     * if _default contains some value,
     * the optional will be assumed "non-empty" and the default value returned.
     */
    private static Optional<Integer> parseConfirmations(@NonNull CommandLine line,
                                                        @Nullable Integer _default) {
        assert line != null;
        assert (_default == null) || (_default.intValue() >= 0) : _default;

        final @Nullable String confirmationsStr = line.getOptionValue("confirmations");
        if (confirmationsStr == null) {
            // There is no "--confirmations" option; should we use a default or fail?
            if (_default == null) {
                System.err.println("WARNING: --confirmations option is mandatory!");
                return Optional.empty();
            } else {
                return Optional.of(_default);
            }
        } else { // confirmationsStr != null
            try {
                final int confirmationsCandidate = Integer.parseUnsignedInt(confirmationsStr);
                assert confirmationsCandidate >= 0 : confirmationsCandidate;
                return Optional.of(confirmationsCandidate);
            } catch (NumberFormatException e) {
                System.err.println("" +
                        "WARNING: --confirmations option should contain non-negative number of confirmations!");
                return Optional.empty();
            }
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
            } else if (line.hasOption("get-currencies")) {
                handleGetCurrencies(line);
            } else if (line.hasOption("get-tracked-addresses")) {
                handleGetTrackedAddresses(line);
            } else if (line.hasOption("add-tracked-address")) {
                handleAddTrackedAddress(line);
            } else if (line.hasOption("get-balances")) {
                handleGetBalances(line);
            } else {
                printHelp();
            }
        } catch (ParseException exp) {
            System.err.printf("ERROR: Parsing failed. Reason: %s\n", exp.getMessage());
            printHelp();
        }
    }

    private static final void handleGetCurrencies(@NonNull CommandLine line) {
        assert line != null;

        printTitle(System.err);

        final @NonNull Optional<List<String>> connectUrlsOpt = parseConnectUrls(line);

        if (connectUrlsOpt.isPresent()) {
            System.err.println("Getting supported currencies...");

            try {
                final ClientConnector connector = new ClientConnectorImpl(connectUrlsOpt.get());
                final @Nullable List<Currency> currencies = connector.getCurrencies();
                if (currencies == null) {
                    System.err.println("ERROR: Could not get the supported currencies!");
                } else {
                    System.err.printf("Supported currencies (%s):\n", currencies.size());
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
                    System.err.println("---");
                    connector.shutdown();
                }
            } catch (CompletionException exc) {
                System.err.println("ERROR: Could not connect to UniCherryGarden!");
            }
        }
    }

    private static final void handleGetTrackedAddresses(@NonNull CommandLine line) {
        assert line != null;

        printTitle(System.err);

        final @NonNull Optional<List<String>> connectUrlsOpt = parseConnectUrls(line);

        if (connectUrlsOpt.isPresent()) {
            System.err.println("Getting tracked addresses...");

            try {
                final ClientConnector connector = new ClientConnectorImpl(connectUrlsOpt.get());
                final Observer observer = connector.getObserver();

                final @Nullable List<@NonNull String> trackedAddresses = observer.getTrackedAddresses();
                if (trackedAddresses == null) {
                    System.err.println("ERROR: Could not get the tracked addresses!");
                } else {
                    System.err.printf("Tracked addresses (%s):\n", trackedAddresses.size());
                    for (final String addr : trackedAddresses) {
                        System.err.printf("  %s\n", addr);
                    }
                    System.err.println("---");
                }
                connector.shutdown();
            } catch (CompletionException exc) {
                System.err.println("ERROR: Could not connect to UniCherryGarden!");
                System.err.printf("%s\n", exc);
            }
        }
    }

    private static final void handleAddTrackedAddress(@NonNull CommandLine line) {
        assert line != null;

        printTitle(System.err);

        final @NonNull Optional<String> addressOpt = parseEthereumAddressOption(line, "add-tracked-address");
        final @NonNull Optional<TrackFromBlockOption> trackFromBlockOpt = parseTrackFromBlock(line);
        final @NonNull Optional<List<String>> connectUrlsOpt = parseConnectUrls(line);
        final @NonNull Optional<String> commentOpt = Optional.ofNullable(line.getOptionValue("comment"));

        if (true &&
                addressOpt.isPresent() &&
                trackFromBlockOpt.isPresent() &&
                connectUrlsOpt.isPresent()
        ) {
            final @NonNull String address = addressOpt.get();
            final @NonNull TrackFromBlockOption trackFromBlock = trackFromBlockOpt.get();

            System.err.printf("Adding tracked address %s with %s; tracking from %s, %s...\n",
                    address,
                    commentOpt.isPresent() ? String.format("comment \"%s\"", commentOpt) : "no comment",
                    trackFromBlock.mode,
                    trackFromBlock.block
            );

            try {
                final ClientConnector connector = new ClientConnectorImpl(connectUrlsOpt.get());
                final Observer observer = connector.getObserver();

                final boolean success = observer.startTrackingAddress(
                        address,
                        trackFromBlock.mode,
                        // .getBlock() rather than just .block, as it should be null if mode
                        // is different from FROM_BLOCK
                        trackFromBlock.block,
                        commentOpt.orElse(null));
                if (success) {
                    System.err.printf("Address %s successfully added!\n");
                } else {
                    System.err.printf("ERROR: Address %s failed to add!\n", address);
                }

                connector.shutdown();
            } catch (CompletionException exc) {
                System.err.println("ERROR: Could not connect to UniCherryGarden!");
                System.err.printf("%s\n", exc);
            }
        }
    }

    private static final void handleGetBalances(@NonNull CommandLine line) {
        assert line != null;

        printTitle(System.err);

        final @NonNull Optional<String> addressOpt = parseEthereumAddressOption(line, "get-balances");
        final @NonNull Optional<List<String>> connectUrlsOpt = parseConnectUrls(line);
        final @NonNull Optional<Integer> confirmationsOpt = parseConfirmations(line, 0);

        if (true &&
                addressOpt.isPresent() &&
                confirmationsOpt.isPresent() &&
                connectUrlsOpt.isPresent()
        ) {
            final String address = addressOpt.get();
            final int confirmations = confirmationsOpt.get().intValue();

            System.err.printf("Getting balances for %s with %s confirmation(s)...\n",
                    address, confirmations);

            try {
                final ClientConnector connector = new ClientConnectorImpl(connectUrlsOpt.get());
                final GetBalances.@NonNull BalanceRequestResult balanceResult = connector.getObserver().getAddressBalances(
                        address,
                        null,
                        confirmations
                );
                if (!balanceResult.overallSuccess) {
                    System.err.printf("ERROR: Could not get the balances for %s!\n", address);
                } else {
                    System.err.printf("Received the balances for %s (with %s confirmation(s)):\n",
                            address, confirmations);

                    for (final GetBalances.BalanceRequestResult.CurrencyBalanceFact balanceFact :
                            balanceResult.balances) {
                        System.err.printf("  %s: %s (synced to %s, %s)\n",
                                balanceFact.currency,
                                balanceFact.amount,
                                balanceFact.syncedToBlock,
                                balanceFact.syncState
                        );
                    }
                    System.err.printf("" +
                                    "Overall status: %s\n" +
                                    "  block %10s: latest known,\n" +
                                    "  block %10s: latest synced by node,\n" +
                                    "  block %10s: latest processed by UniCherryGarden.\n",
                            balanceResult.overallSuccess,
                            balanceResult.latestBlockchainKnownBlock,
                            balanceResult.latestBlockchainSyncedBlock,
                            balanceResult.latestUniCherryGardenSyncedBlock
                    );
                    connector.shutdown();
                }
            } catch (CompletionException exc) {
                System.err.println("ERROR: Could not connect to UniCherryGarden!");
            }
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
