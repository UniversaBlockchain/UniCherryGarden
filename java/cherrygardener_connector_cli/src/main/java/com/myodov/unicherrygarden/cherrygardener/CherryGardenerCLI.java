package com.myodov.unicherrygarden.cherrygardener;

import com.myodov.unicherrygarden.api.types.SystemSyncStatus;
import com.myodov.unicherrygarden.api.types.Transfer;
import com.myodov.unicherrygarden.api.types.dlt.Currency;
import com.myodov.unicherrygarden.connector.api.ClientConnector;
import com.myodov.unicherrygarden.connector.api.Observer;
import com.myodov.unicherrygarden.connector.impl.ClientConnectorImpl;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import com.myodov.unicherrygarden.messages.cherrypicker.AddTrackedAddresses;
import com.myodov.unicherrygarden.messages.cherrypicker.GetBalances;
import com.myodov.unicherrygarden.messages.cherrypicker.GetTransfers;
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
import java.util.function.Supplier;


/**
 * Command Line Interface frontend to CherryGardener Connector API.
 */
public class CherryGardenerCLI {
    private static final Logger logger = LoggerFactory.getLogger(CherryGardenerCLI.class);

    /**
     * HOCON conf file in <code>~/.unicherrygarden/cli.conf</code>.
     */
    final static ConfFile confFile = new ConfFile();

    private static final Options options = new Options();

    protected static final int DEFAULT_NUMBER_OF_CONFIRMATIONS = 6;

    static {
        options.addOption(
                "c", "connect", true,
                "Comma-separated list of addresses to connect;\n" +
                        "e.g. \"127.0.0.1:2551,127.0.0.1:2552\".");
        options.addOption(
                null, "confirmations", true,
                "The number of confirmations (Ethereum blocks already mined after an event);\n" +
                        "Should be a non-negative integer number, likely 6 or 12 or more. Default: 6.");
        options.addOption(
                "lp", "listen-port", true,
                "The IP port to listen;\n" +
                        "Should be a non-negative integer number, 0 to 65535, 0 means autogenerate. " +
                        "Note the client (at this port) should be reachable to the servers, so if you are using " +
                        "SSH port forwarding, you may need to forward both local and remote ports. " +
                        "Default: 0 (autogenerate).");

        final OptionGroup commandOptionGroup = new OptionGroup() {{
            addOption(new Option(
                    "h", "help", false,
                    "Display help."));
            addOption(new Option(
                    "gc", "get-currencies", false,
                    "Print the list of currencies supported by CherryGardener and all other components."));
            addOption(new Option(
                    "gta", "get-tracked-addresses", false,
                    "Print the list of addresses tracked by CherryPicker."));
            addOption(new Option(
                    "ata", "add-tracked-address", true,
                    "Add an Ethereum address to track.\n" +
                            "Should be a valid Ethereum address;\n" +
                            "e.g. \"0x884191033518be08616821d7676ca01695698451\".\n" +
                            "See also:\n" +
                            "--track-from-block (mandatory),\n" +
                            "--comment (optional)."));
            addOption(new Option(
                    "gb", "get-balances", true,
                    "Get balances (of currencies/tokens) for an (already tracked) Ethereum address.\n" +
                            "Should be a valid Ethereum address;\n" +
                            "e.g. \"0x884191033518be08616821d7676ca01695698451\".\n" +
                            "See also:\n" +
                            "--confirmations (optional; default: 0)."));
            addOption(new Option(
                    "gt", "get-transfers", false,
                    "Get the transfers balances (of currencies/tokens).\n" +
                            "At least one of --sender or --receiver values should be provided, " +
                            "and must be an already tracked Ethereum address!\n" +
                            "See also:\n" +
                            "--confirmations (optional; default: 0);\n" +
                            "--sender (optional; valid Ethereum address);\n" +
                            "--receiver (optional; valid Ethereum address);\n" +
                            "--from-block (optional; default: first available);\n" +
                            "--to-block (optional; default: last available, but not newer than " +
                            "the --confirmations value permit).\n"

            ));
            setRequired(true);
        }};
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
        options.addOption(
                "from", "sender", true,
                "Sender Ethereum address."
        );
        options.addOption(
                "to", "receiver", true,
                "Receiver Ethereum address."
        );
        options.addOption(
                null, "from-block", true,
                "First block to use (inclusive) for read operations.\n" +
                        "Should contain a number of the block (0 or more)."
        );
        options.addOption(
                null, "to-block", true,
                "Last block to use (inclusive) for read operations.\n" +
                        "Should contain a number of the block (0 or more).\n" +
                        "If --from-block is also present, --to-block value should be greater or equal " +
                        "to --from-block value."
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

    private static Optional<Integer> _parseListenPort(@NonNull CommandLine line) {
        final Optional<Integer> listenPortConf = confFile.getListenPort();

        if (line.hasOption("listen-port")) {
            final String optString = line.getOptionValue("listen-port");
            final int listenPort;
            try {
                listenPort = Integer.parseUnsignedInt(optString);
            } catch (NumberFormatException e) {
                System.err.println("WARNING: --listen-port option should contain an IP port number!");
                return Optional.empty();
            }
            if (!(0 <= listenPort && listenPort <= 65535)) {
                System.err.println("WARNING: --listen-port value should be between 0 and 65535 inclusive!");
                return Optional.empty();
            }
            return Optional.of(listenPort);
        } else if (listenPortConf.isPresent()) {
            // Fallback to the option in conf file
            return listenPortConf;
        } else {
            // Final fallback to the default: 0
            System.err.println("Note: --listen-port value is missing, using 0 as default (autogenerate the port). " +
                    "Please make sure your client at this port is reachable from the server network!");
            return Optional.of(0);
        }
    }

    private static Optional<List<String>> _parseConnectUrls(@NonNull CommandLine line) {
        final Optional<List<String>> connectUrlsConf = confFile.getConnectUrls();

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
        } else if (connectUrlsConf.isPresent()) {
            // Fallback to the option in conf file
            return connectUrlsConf;
        } else {
            // Final fallback: no defaults
            System.err.println("WARNING: --connect option must be present!");
            return Optional.empty();
        }
    }

    /**
     * Parses the "--listen-port" and "--connect" option assuming that:
     * <ul>
     *     <li><code>--listen-port</code> setting is optional, with <b>0</b> as a default (meaning the port
     *     will be autogenerated).</li>
     *     <li><code>--connect</code></li>
     * </ul>
     * <p>
     * it is optional, with 0 (autogenerate) as default,
     * printing all necessary warnings in the process.
     *
     * @return non-empty {@link Optional<>} with the connection settings if they have been properly parsed;
     * “empty” optional if parsing failed (and all necessary warnings were printed).
     */
    private static Optional<ConnectionSettings> parseConnectionSettings(@NonNull CommandLine line) {
        final Optional<Integer> listenPortOpt = _parseListenPort(line);
        final Optional<List<String>> connectUrlsOpt = _parseConnectUrls(line);

        if (listenPortOpt.isPresent() && connectUrlsOpt.isPresent()) {
            return Optional.of(new ConnectionSettings(listenPortOpt.get().intValue(), connectUrlsOpt.get()));
        } else {
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
                                                                     @NonNull String optionName,
                                                                     boolean mandatory) {
        @Nullable final String address = line.getOptionValue(optionName);
        if (address == null) {
            if (mandatory) {
                System.err.printf("WARNING: --%s option should be present!\n", optionName);
            }
            return Optional.empty();
        } else if (!EthUtils.Addresses.isValidAddress(address)) {
            System.err.printf("WARNING: --%s option should contain a valid Ethereum address!\n", optionName);
            return Optional.empty();
        } else {
            return Optional.of(address.toLowerCase());
        }
    }

    /**
     * Parse an option (with the name of the option passed as the "optionName" argument)
     * that should contain a valid block number,
     * printing all necessary warnings in the process.
     *
     * @param optionName The name of the option to parse.
     * @return non-empty {@link Optional<>} with the parsed Ethereum block number,
     * if it has been properly parsed;
     * “empty” optional if parsing failed (and all required warnings were printed).
     */
    private static final Optional<Integer> parseBlockNumberOption(@NonNull CommandLine line,
                                                                  @NonNull String optionName,
                                                                  boolean mandatory) {
        @Nullable final String blockNumberCandidate = line.getOptionValue(optionName);
        if (blockNumberCandidate == null) {
            if (mandatory) {
                System.err.printf("WARNING: --%s option should be present!\n", optionName);
            }
            return Optional.empty();
        } else {
            final int intValue;
            try {
                intValue = Integer.parseInt(blockNumberCandidate);
            } catch (NumberFormatException e) {
                System.err.printf("WARNING: --%s option should contain a valid Ethereum block number!\n", optionName);
                return Optional.empty();
            }

            if (intValue < 0) {
                System.err.printf("WARNING: --%s option should be 0 or higher!\n", optionName);
                return Optional.empty();
            } else {
                return Optional.of(intValue);
            }
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
            return new TrackFromBlockOption(AddTrackedAddresses.StartTrackingAddressMode.FROM_BLOCK, block);
        }

        /**
         * Create the {@link TrackFromBlockOption} when you autodetect the number of the block.
         *
         * @param mode The mode of detection.
         *             Use any mode except {@link AddTrackedAddresses.StartTrackingAddressMode#FROM_BLOCK}
         *             (if you want to use a specific block number, use {@link #fromSpecificBlock(int)} method instead).
         */
        static TrackFromBlockOption fromAutoDetectedBlock(AddTrackedAddresses.@NonNull StartTrackingAddressMode mode) {
            assert mode != null && mode != AddTrackedAddresses.StartTrackingAddressMode.FROM_BLOCK : mode;
            return new TrackFromBlockOption(mode, null);
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
     * if it has been properly parsed (or the option is missing, then the default value is assumed);
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
     * Parse "--confirmations" option,
     * printing all necessary warnings in the process.
     * The default number of confirmations is used.
     *
     * @return non-empty {@link Optional<>} with the parsed number of confirmations,
     * if it has been properly parsed (or the option is missing, then the default value is assumed);
     * “empty” optional if parsing failed (and all required warnings were printed).
     */
    private static Optional<Integer> parseConfirmations(@NonNull CommandLine line) {
        return parseConfirmations(line, DEFAULT_NUMBER_OF_CONFIRMATIONS);
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
            } else if (line.hasOption("get-transfers")) {
                handleGetTransfers(line);
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

        final @NonNull Optional<ConnectionSettings> connectionSettingsOpt = parseConnectionSettings(line);

        if (connectionSettingsOpt.isPresent()) {
            System.err.println("Getting supported currencies...");
            final @NonNull ConnectionSettings connectionSettings = connectionSettingsOpt.get();

            try {
                final ClientConnector connector =
                        new ClientConnectorImpl(connectionSettings.connectUrls, connectionSettings.listenPort);
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
                }
                connector.shutdown();
            } catch (CompletionException exc) {
                System.err.println("ERROR: Could not connect to UniCherryGarden!");
            }
        }
    }

    private static final void handleGetTrackedAddresses(@NonNull CommandLine line) {
        assert line != null;

        printTitle(System.err);

        final @NonNull Optional<ConnectionSettings> connectionSettingsOpt = parseConnectionSettings(line);

        if (connectionSettingsOpt.isPresent()) {
            System.err.println("Getting tracked addresses...");
            final @NonNull ConnectionSettings connectionSettings = connectionSettingsOpt.get();

            try {
                final ClientConnector connector =
                        new ClientConnectorImpl(connectionSettings.connectUrls, connectionSettings.listenPort);
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

        final @NonNull Optional<ConnectionSettings> connectionSettingsOpt = parseConnectionSettings(line);
        final @NonNull Optional<String> addressOpt = parseEthereumAddressOption(line, "add-tracked-address", true);
        final @NonNull Optional<TrackFromBlockOption> trackFromBlockOpt = parseTrackFromBlock(line);
        final @NonNull Optional<String> commentOpt = Optional.ofNullable(line.getOptionValue("comment"));

        if (true &&
                connectionSettingsOpt.isPresent() &&
                addressOpt.isPresent() &&
                trackFromBlockOpt.isPresent()
        ) {
            final @NonNull ConnectionSettings connectionSettings = connectionSettingsOpt.get();
            final @NonNull String address = addressOpt.get();
            final @NonNull TrackFromBlockOption trackFromBlock = trackFromBlockOpt.get();

            System.err.printf("Adding tracked address %s with %s; tracking from %s, %s...\n",
                    address,
                    commentOpt.isPresent() ? String.format("comment \"%s\"", commentOpt) : "no comment",
                    trackFromBlock.mode,
                    trackFromBlock.block
            );

            try {
                final ClientConnector connector =
                        new ClientConnectorImpl(connectionSettings.connectUrls, connectionSettings.listenPort);
                final Observer observer = connector.getObserver();

                final boolean success = observer.startTrackingAddress(
                        address,
                        trackFromBlock.mode,
                        trackFromBlock.block,
                        commentOpt.orElse(null));
                if (success) {
                    System.err.printf("Address %s successfully added!\n", address);
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

        final @NonNull Optional<ConnectionSettings> connectionSettingsOpt = parseConnectionSettings(line);
        final @NonNull Optional<String> addressOpt = parseEthereumAddressOption(line, "get-balances", true);
        final @NonNull Optional<Integer> confirmationsOpt = parseConfirmations(line);

        if (true &&
                connectionSettingsOpt.isPresent() &&
                addressOpt.isPresent() &&
                confirmationsOpt.isPresent()
        ) {
            final @NonNull ConnectionSettings connectionSettings = connectionSettingsOpt.get();
            final @NonNull String address = addressOpt.get();
            final int confirmations = confirmationsOpt.get().intValue();

            System.err.printf("Getting balances for %s with %s confirmation(s)...\n",
                    address, confirmations);

            try {
                final ClientConnector connector = new ClientConnectorImpl(
                        connectionSettings.connectUrls,
                        connectionSettings.listenPort,
                        confirmations);
                final Optional<GetBalances.BalanceRequestResult> resultOpt = Optional.ofNullable(
                        connector.getObserver().getAddressBalances(
                                address,
                                null,
                                0 // on top of connector-level confirmations number
                        ));
                if (!resultOpt.isPresent()) {
                    System.err.printf("ERROR: Could not get the balances for %s!\n", address);
                } else {
                    final GetBalances.BalanceRequestResult result = resultOpt.get();
                    System.err.printf("Received the balances for %s (with %s confirmation(s)):\n",
                            address, confirmations);

                    for (final GetBalances.BalanceRequestResult.CurrencyBalanceFact balanceFact : result.balances) {
                        System.err.printf("  %s: %s (synced to %s, %s)\n",
                                balanceFact.currency,
                                balanceFact.amount,
                                balanceFact.syncedToBlock,
                                balanceFact.syncState
                        );
                    }
                    printOverallStatus(result.syncStatus);
                }
                connector.shutdown();
            } catch (CompletionException exc) {
                System.err.println("ERROR: Could not connect to UniCherryGarden!");
            }
        }
    }

    private static final void handleGetTransfers(@NonNull CommandLine line) {
        assert line != null;

        printTitle(System.err);

        final @NonNull Optional<ConnectionSettings> connectionSettingsOpt = parseConnectionSettings(line);
        final @NonNull Optional<Integer> confirmationsOpt = parseConfirmations(line);
        final @NonNull Optional<String> senderOpt = parseEthereumAddressOption(line, "sender", false);
        final @NonNull Optional<String> receiverOpt = parseEthereumAddressOption(line, "receiver", false);
        final @NonNull Optional<Integer> fromBlockOpt = parseBlockNumberOption(line, "from-block", false);
        final @NonNull Optional<Integer> toBlockOpt = parseBlockNumberOption(line, "to-block", false);

        if (true &&
                connectionSettingsOpt.isPresent() &&
                confirmationsOpt.isPresent()
        ) {
            final @NonNull ConnectionSettings connectionSettings = connectionSettingsOpt.get();
            final int confirmations = confirmationsOpt.get();

            // Extra validations to co-validate the arguments
            final boolean coValid;
            {
                final Supplier<Boolean> coValidator = (() -> {
                    if (!senderOpt.isPresent() && !receiverOpt.isPresent()) {
                        System.err.println("ERROR: at least one of --sender or --receiver must be defined!");
                        return false;
                    }

                    if (fromBlockOpt.isPresent() && toBlockOpt.isPresent()) {
                        final int fromBlock = fromBlockOpt.get();
                        final int toBlock = toBlockOpt.get();
                        if (toBlock < fromBlock) {
                            System.err.println("ERROR: --from-block value must be <= --to-block value!");
                            return false;
                        }
                    }
                    // In any other case, all the arguments are valid together.
                    return true;
                });
                coValid = coValidator.get();
            }

            // All error messages were printed already by coValidator, so we don’t handle the `if !coValid` case at all
            if (coValid) {
                @NonNull final String transfersDescription = String.format("%s%s%s%swith %s confirmation(s)",
                        (senderOpt.isPresent() ? String.format("from %s ", senderOpt.get()) : ""),
                        (receiverOpt.isPresent() ? String.format("to %s ", receiverOpt.get()) : ""),
                        (fromBlockOpt.isPresent() ? String.format("from block %s ", fromBlockOpt.get()) : ""),
                        (toBlockOpt.isPresent() ? String.format("to block %s ", toBlockOpt.get()) : ""),
                        confirmations);

                System.err.printf("Getting transfers %s...\n", transfersDescription);

                try {
                    final ClientConnector connector = new ClientConnectorImpl(
                            connectionSettings.connectUrls,
                            connectionSettings.listenPort,
                            confirmations);
//                    final GetTransfers.@NonNull TransfersRequestResult result =
                    final Optional<GetTransfers.TransfersRequestResult> resultOpt = Optional.ofNullable(connector.getObserver().getTransfers(
                            0, // on top of connector-level confirmations number
                            senderOpt.orElse(null),
                            receiverOpt.orElse(null),
                            fromBlockOpt.orElse(null),
                            toBlockOpt.orElse(null),
                            null
                    ));
                    if (!resultOpt.isPresent()) {
                        System.err.printf("ERROR: Could not get the transfers %s!\n", transfersDescription);
                    } else {
                        final GetTransfers.TransfersRequestResult result = resultOpt.get();

                        System.err.printf("Received the transfers %s:\n", transfersDescription);

                        for (final Transfer transfer : result.transfers) {
                            System.err.printf("  * %s of %s from %s to %s\n",
                                    transfer.amount,
                                    transfer.currencyKey,
                                    transfer.from,
                                    transfer.to
                            );
                        }
                        printOverallStatus(result.syncStatus);
                    }
                    connector.shutdown();
                } catch (CompletionException exc) {
                    System.err.println("ERROR: Could not connect to UniCherryGarden!");
                }
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

    private static final void printOverallStatus(@NonNull SystemSyncStatus syncStatus) {
        System.err.printf("" +
                        "Overall status:\n" +
                        "  Blockchain:\n" +
                        "    block %10s: latest known,\n" +
                        "    block %10s: latest synced by node,\n" +
                        "  UniCherryPicker:\n" +
                        "    block %10s: latest processed.\n",
                syncStatus.blockchain.latestKnownBlock,
                syncStatus.blockchain.latestSyncedBlock,
                syncStatus.cherryPicker.latestSyncedBlock
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
