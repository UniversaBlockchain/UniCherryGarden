package com.myodov.unicherrygarden.cherrygardener;

import com.myodov.unicherrygarden.api.types.MinedTransfer;
import com.myodov.unicherrygarden.api.types.SystemStatus;
import com.myodov.unicherrygarden.api.types.dlt.Currency;
import com.myodov.unicherrygarden.api.types.responseresult.ResponseWithPayload;
import com.myodov.unicherrygarden.connector.api.ClientConnector;
import com.myodov.unicherrygarden.connector.api.Observer;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import com.myodov.unicherrygarden.messages.CherryGardenResponseWithPayload;
import com.myodov.unicherrygarden.messages.cherrygardener.GetCurrencies;
import com.myodov.unicherrygarden.messages.cherrygardener.Ping;
import com.myodov.unicherrygarden.messages.cherrypicker.*;
import org.apache.commons.cli.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.tx.ChainIdLong;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.myodov.unicherrygarden.StringTools.withOffset;


/**
 * Command Line Interface frontend to CherryGardener Connector API.
 */
public class CherryGardenerCLI {
    /**
     * HOCON conf file in <code>~/.unicherrygarden/cli.conf</code>.
     */
    final static ConfFile confFile = new ConfFile();

    private static final Options options = new Options();

    protected static final int DEFAULT_NUMBER_OF_CONFIRMATIONS = 6;

    static {
        options.addOption(Option.builder("c")
                .longOpt("connect")
                .hasArgs()
                .valueSeparator(',')
                .desc("Comma-separated list of addresses to connect;\n" +
                        "e.g. \"127.0.0.1:2551,127.0.0.1:2552\".")
                .build());
        options.addOption(
                null, "realm", true,
                "The \"realm\" text string matching the realm of UniCherryGarden;\n" +
                        "needed to distinguish between multiple CherryGarden components for different blockchains,\n" +
                        "running in the same Akka cluster.");
        options.addOption(
                null, "chain-id", true,
                "The integer Chain ID of Ethereum network;\n" +
                        "needed to create the compatible transactions for the EIP-155 compatibility.\n" +
                        "Default: 1 (Ethereum Mainnet).");
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
                    null, "ping", false,
                    "Ping the CherryGardener."));
            addOption(Option.builder("gc")
                    .longOpt("get-currencies")
                    .hasArgs()
                    .optionalArg(true)
                    .valueSeparator(',')
                    .desc("Print the list of currencies supported by CherryGardener and all other components.\n" +
                            "Pass comma-separated currency keys, i.e. lowercased Ethereum addresses of token contracts,\n" +
                            "or empty string for base blockchain currency (ETH in case of Ethereum Mainnet).\n" +
                            "E.g.:\n" +
                            "  --get-currencies    for all currencies;\n" +
                            "  --get-currencies=   for just a single ETH to select;\n" +
                            "  --get-currencies=,0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7   for ETH and UTNP tokens.")
                    .build());
            addOption(new Option(
                    "gta", "get-tracked-addresses", false,
                    "Print the list of addresses tracked by CherryPicker."));
            addOption(new Option(
                    "gad", "get-address-details", true,
                    "Print the details about a single address, tracked or untracked."));
            addOption(new Option(
                    "ata", "add-tracked-address", true,
                    "Add an Ethereum address to track.\n" +
                            "Should be a valid Ethereum address;\n" +
                            "e.g. \"0x884191033518be08616821d7676ca01695698451\".\n" +
                            "See also:\n" +
                            "--track-from-block (mandatory),\n" +
                            "--comment (optional)."));
            addOption(Option.builder("gb")
                    .longOpt("get-balances")
                    .hasArgs()
                    .valueSeparator(',')
                    .desc("Get balances (of currencies/tokens) for an (already tracked) Ethereum address.\n" +
                            "Should be a valid Ethereum address;\n" +
                            "e.g. \"0x884191033518be08616821d7676ca01695698451\".\n" +
                            "See also:\n" +
                            "--confirmations (optional; default: 0).\n" +
                            "--parallel (optional; default: omitted, run sequentially).")
                    .build());
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
                            "the --confirmations value permit);\n" +
                            "--with-balances (optional; default: omitted)."
            ));
//            addOption(new Option(
//                    "cot", "create-outgoing-transfer", true,
//                    "Build a transaction for outgoing transfer of some currency\n" +
//                            "from some address to some other address.\n" +
//                            "The transaction is just built (locally, in memory) but not sent out to the blockchain\n" +
//                            "and not stored anywhere.\n" +
//                            "See also:\n" +
//                            "--sender (mandatory),\n" +
//                            "--recipient (mandatory),\n" +
//                            "--chain-id (optional, default: 1 for Ethereum Mainnet),\n" +
//                            "--currency-key (mandatory),\n" +
//                            "--amount (mandatory),\n" +
//                            "--comment (optional)."));
//            addOption(new Option(
//                    "st", "sign-transaction", true,
//                    "Build a transaction for outgoing transfer of some currency\n" +
//                            "from some address to some other address.\n" +
//                            "The transaction is just built (locally, in memory) but not sent out to the blockchain\n" +
//                            "and not stored anywhere.\n" +
//                            "See also:\n" +
//                            "--sender (mandatory),\n" +
//                            "--recipient (mandatory),\n" +
//                            "--currency-key (mandatory),\n" +
//                            "--amount (mandatory),\n" +
//                            "--comment (optional)."));
            setRequired(true);
        }};
        options.addOptionGroup(commandOptionGroup);

        options.addOption(
                null, "loop", false,
                "(For those commands that support it) start infinite loop,\n" +
                        "repeating the operation over and over.\n" +
                        "Can be used for benchmark/performance purposes."
        );
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
        options.addOption(
                null, "with-balances", false,
                "Whether the request should also retrieve the balances of the mentioned addresses. \n" +
                        "If present, the balances are requested; if omitted; the balances are not requested."
        );
        options.addOption(
                "par", "parallel", false,
                "If multiple arguments passed, specifies whether the queries should run in parallel. " +
                        "Will run sequentially if omitted."
        );
    }

    protected static final Properties propsCLI = loadPropsFromNamedResource("cherrygardener_connector_cli.properties");
    protected static final Properties propsConnector = loadPropsFromNamedResource("cherrygardener_connector.properties");

    /**
     * Load {@link Properties} from a .properties-formatted file in the artifact resources.
     */
    @NonNull
    private static Properties loadPropsFromNamedResource(@NonNull String resourceName) {
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

        @Nullable final String[] connectEntriesArr = line.getOptionValues("connect");

        if (connectEntriesArr != null) {
            final List<String> connectUrls = Collections.unmodifiableList(Arrays.asList(connectEntriesArr));
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
            System.err.println("WARNING: --connect option must be present, " +
                    "or `connect.urls` setting defined in conf file!");
            return Optional.empty();
        }
    }

    private static Optional<String> _parseRealm(@NonNull CommandLine line) {
        final Optional<String> realmConf = confFile.getRealm();

        if (line.hasOption("realm")) {
            final String optString = line.getOptionValue("realm");

            if (optString.matches("^[-_a-zA-Z0-9]*$")) {
                return Optional.of(optString);
            } else {
                System.err.printf("ERROR: --realm option can contain only latin letters, digits, \"-\" or \"_\" sign. " +
                                "Currently it is \"%s\".\n",
                        optString);
                return Optional.empty();
            }
        } else if (realmConf.isPresent()) {
            // Fallback to the option in conf file
            return realmConf;
        } else {
            // Final fallback: no defaults
            System.err.println("WARNING: --realm option must be present, " +
                    "or `connect.realm` setting defined in conf file!");
            return Optional.empty();
        }
    }

    private static Optional<Long> _parseChainId(@NonNull CommandLine line) {
        final Optional<Long> chainIdConf = confFile.getChainId();

        if (line.hasOption("chain-id")) {
            final String optString = line.getOptionValue("chain-id");
            final long chainId;

            try {
                chainId = Long.parseLong(optString);
            } catch (NumberFormatException e) {
                System.err.println("WARNING: --chain-id option should contain an Ethereum Chain ID as a number!");
                return Optional.empty();
            }
            if (!(chainId == -1 || chainId >= 1)) {
                System.err.println("WARNING: --chain-id value should be >=1; can be -1 (None) but not recommended!");
                return Optional.empty();
            }
            return Optional.of(chainId);
        } else if (chainIdConf.isPresent()) {
            // Fallback to the option in conf file
            return chainIdConf;
        } else {
            // Final fallback to the default: 1 (Mainnet)
            final long defaultValue = ChainIdLong.MAINNET;
            System.err.printf("Note: --chain-id value is missing, using %s as default.\n", defaultValue);
            return Optional.of(defaultValue);
        }
    }

    /**
     * Parses the "--listen-port", "--connect", "--realm" and "--chain-id" option assuming that:
     * <ul>
     *     <li><code>--listen-port</code> setting is optional, with <b>0</b> as a default (meaning the port
     *     will be autogenerated).
     *     (Reason: by default, you will listen on a random part;
     *     but for some complex configurations, you may need more complex setup).</li>
     *     <li><code>--connect</code> setting is mandatory, should either be provided in command line
     *     or as a `connect.urls` setting in conf file.
     *     (Reason: otherwise you won’t even be able to connect to UniCherryGarden cluster).</li>
     *     <li><code>--realm</code> setting is mandatory, should either be provided in command line
     *     or as a `connect.realm` setting in conf file.
     *     (Reason: otherwise, in case of multiple UniCherryGarden systems in one cluster, you won’t be able
     *     to connect to proper one).</li>
     *     <li><code>--chain-id</code> setting is optional, with <b>1</b> (Ethereum Mainnet) as default,
     *     but suggested to be explicitly provided in the command line or as a `blockchain.chain_id` setting in conf file.
     *     (Reason: otherwise you won’t be able to build the transactions matching the proper network; see EIP-155).
     *     </li>
     * </ul>
     * <p>
     *
     * @return non-empty {@link Optional<>} with the connection settings if they have been properly parsed;
     * “empty” optional if parsing failed (and all necessary warnings were printed).
     */
    private static Optional<ConnectionSettings> parseConnectionSettings(@NonNull CommandLine line) {
        final Optional<Integer> listenPortOpt = _parseListenPort(line);
        final Optional<List<String>> connectUrlsOpt = _parseConnectUrls(line);
        final Optional<String> realmOpt = _parseRealm(line);
        final Optional<Long> chainIdOpt = _parseChainId(line);

        if (listenPortOpt.isPresent() &&
                connectUrlsOpt.isPresent() &&
                realmOpt.isPresent() &&
                chainIdOpt.isPresent()) {
            return Optional.of(new ConnectionSettings(
                    listenPortOpt.get(),
                    connectUrlsOpt.get(),
                    realmOpt.get(),
                    chainIdOpt.get()
            ));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Parse an option (with the name of the option passed as the "optionName" argument)
     * that should contain an Ethereum address,
     * printing all necessary warnings in the process.
     *
     * @param optionName the name of the option to parse.
     * @return non-empty {@link Optional<>} with the parsed (lowercased) Ethereum address
     * if it has been properly parsed;
     * “empty” optional if parsing failed (and all required warnings were printed).
     */
    private static Optional<String> parseEthereumAddressOption(@NonNull CommandLine line,
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
     * that should contain a list of comma-separated Ethereum addresses,
     * printing all necessary warnings in the process.
     *
     * @param optionName the name of the option to parse.
     * @return non-empty {@link Optional<>} with the list of parsed (lowercased) Ethereum addresses
     * if it has been properly parsed;
     * “empty” optional if parsing failed (and all required warnings were printed).
     */
    private static Optional<List<String>> parseEthereumAddressesOption(@NonNull CommandLine line,
                                                                       @NonNull String optionName,
                                                                       boolean mandatory,
                                                                       boolean nonEmpty) {
        @Nullable final String[] addressesArr = line.getOptionValues(optionName);

        if (addressesArr == null) {
            if (mandatory) {
                System.err.printf("WARNING: --%s option should be present!\n", optionName);
            }
            return Optional.empty();
        } else {
            final List<String> addresses = Collections.unmodifiableList(Arrays.asList(addressesArr));
            if (nonEmpty && addresses.isEmpty()) {
                System.err.printf("WARNING: --%s option should contain a valid non-empty comma-separated list of Ethereum addresses!\n",
                        optionName);
                return Optional.empty();
            } else if (!addresses.stream().allMatch(EthUtils.Addresses::isValidAddress)) {
                System.err.printf("WARNING: --%s option should contain a valid comma-separated list of Ethereum addresses!\n",
                        optionName);
                return Optional.empty();
            } else {
                return Optional.of(
                        addresses.stream()
                                .map(String::toLowerCase)
                                .collect(Collectors.toList())
                );
            }
        }
    }

    /**
     * Parse an option (with the name of the option passed as the "optionName" argument)
     * that should contain a valid block number,
     * printing all necessary warnings in the process.
     *
     * @param optionName the name of the option to parse.
     * @return non-empty {@link Optional<>} with the parsed Ethereum block number,
     * if it has been properly parsed;
     * “empty” optional if parsing failed (and all required warnings were printed).
     */
    private static Optional<Integer> parseBlockNumberOption(@NonNull CommandLine line,
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
            assert (block == null) || (block >= 0) : block;
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
        assert (_default == null) || (_default >= 0) : _default;

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
     * Parse an option (with the name of the option passed as the "optionName" argument)
     * that should contain a list of currency keys,
     * printing all necessary warnings in the process.
     *
     * @param optionName the name of the option to parse.
     * @return non-empty {@link Optional<List<String>>} with the parsed currency keys
     * if it has been properly parsed;
     * “empty” optional if parsing failed (and all required warnings were printed).
     */
    private static Optional<List<String>> parseCurrencyKeysOption(@NonNull CommandLine line,
                                                                  @NonNull String optionName,
                                                                  boolean mandatory) {

        final String[] optionValues = line.getOptionValues(optionName);
        if (optionValues == null) {
            if (mandatory) {
                System.err.printf("WARNING: --%s option should be present!\n", optionName);
            }
            return Optional.empty();
        } else {
            final List<String> ckCandidates = Arrays.asList(optionValues);
            boolean anyBad = false;
            for (final String ckCandidate : ckCandidates) {
                if (!ckCandidate.isEmpty() && !EthUtils.Addresses.isValidLowercasedAddress(ckCandidate)) {
                    anyBad = true;
                    System.err.printf("WARNING: --%s is not a valid currency key!\n", ckCandidate);
                }
            }
            if (anyBad) {
                return Optional.empty();
            } else {
                return Optional.of(Collections.unmodifiableList(ckCandidates));
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
            } else if (line.hasOption("ping")) {
                handlePing(line);
            } else if (line.hasOption("get-currencies")) {
                handleGetCurrencies(line);
            } else if (line.hasOption("get-tracked-addresses")) {
                handleGetTrackedAddresses(line);
            } else if (line.hasOption("get-address-details")) {
                handleGetAddressDetails(line);
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

    private static void handlePing(@NonNull CommandLine line) {
        assert line != null;

        final Logger logger = LoggerFactory.getLogger(CherryGardenerCLI.class);

        printTitle(System.err);

        // Mandatory
        final @NonNull Optional<ConnectionSettings> connectionSettingsOpt = parseConnectionSettings(line);

        if (connectionSettingsOpt.isPresent()) {
            System.err.println("Pinging...");
            final @NonNull ConnectionSettings connectionSettings = connectionSettingsOpt.get();

            // ChainID is non-essential for Ping, so let it be just the default.
            try (final ClientConnector connector = connectionSettings.createClientConnector()) {

                final Ping.Response response = connector.ping();

                if (response.isFailure()) {
                    System.err.printf("ERROR: Could not ping UniCherryGarden! Problem: %s\n",
                            response.getFailure());
                } else {
                    final Ping.PingRequestResultPayload payload = response.getPayloadAsSuccessful();

                    System.err.printf("" +
                                    "Ping completed!\n" +
                                    "UniCherryGarden running in realm \"%s\"; \n" +
                                    "  build v.%s at %s.\n",
                            payload.realm,
                            payload.version, payload.buildTs);
                    printSystemStatus(payload.systemStatus);
                }
            } catch (Exception e) {
                System.err.printf("ERROR: Could not connect to UniCherryGarden! Exception %s\n", e);
                logger.error("Execution error", e);
            }
        }
    }

    private static void handleGetCurrencies(@NonNull CommandLine line) {
        assert line != null;

        final Logger logger = LoggerFactory.getLogger(CherryGardenerCLI.class);

        printTitle(System.err);

        // Mandatory
        final @NonNull Optional<ConnectionSettings> connectionSettingsOpt = parseConnectionSettings(line);
        // Optional
        final @NonNull Optional<List<String>> filterCurrencyKeys = parseCurrencyKeysOption(line, "get-currencies", false);

        final boolean loopEnabled = line.hasOption("loop");

        if (connectionSettingsOpt.isPresent()) {
            System.err.println("Getting supported currencies...");
            if (filterCurrencyKeys.isPresent()) {
                System.err.printf("Using filter: %s\n", filterCurrencyKeys.get());
            }
            final @NonNull ConnectionSettings connectionSettings = connectionSettingsOpt.get();

            // ChainID is non-essential for GetCurrencies, so let it be just the default.
            try (final ClientConnector connector = connectionSettings.createClientConnector()) {
                do {
                    final Instant startTime = Instant.now();
                    final GetCurrencies.Response response = connector.getCurrencies(
                            filterCurrencyKeys
                                    // Convert to set for network transmission
                                    .map(list -> new HashSet<>(list))
                                    .orElse(null),
                            true,
                            false);

                    if (response.isFailure()) {
                        System.err.printf("ERROR: Could not get the currencies! Problem: %s\n",
                                response.getFailure());
                    } else {
                        final GetCurrencies.CurrenciesRequestResultPayload payload = response.getPayloadAsSuccessful();
                        final List<Currency> currencies = payload.currencies;

                        System.err.printf("Supported currencies (%s):\n", currencies.size());
                        for (final Currency c : currencies) {
                            final @Nullable String optComment = c.getComment();
                            System.err.printf("  %s: \"%s\" (transfer gas limit %s) - %s%s\n",
                                    c.getSymbol(),
                                    c.getName(),
                                    c.getTransferGasLimit(),
                                    (c.getCurrencyType() == Currency.CurrencyType.ETH) ?
                                            "Ether cryptocurrency" :
                                            String.format("ERC20 token at %s", c.getDAppAddress()),
                                    (optComment == null) ? "" : String.format(" (%s)", optComment)
                            );
                        }
                        printSystemStatus(payload.systemStatus);
                    }
                    System.err.printf("--- Done in %s --\n", Duration.between(startTime, Instant.now()));
                } while (loopEnabled);
            } catch (Exception e) {
                System.err.printf("ERROR: Could not connect to UniCherryGarden! Exception %s\n", e);
                logger.error("Execution error", e);
            }
        }
    }

    private static void handleGetTrackedAddresses(@NonNull CommandLine line) {
        assert line != null;

        final Logger logger = LoggerFactory.getLogger(CherryGardenerCLI.class);

        printTitle(System.err);

        final @NonNull Optional<ConnectionSettings> connectionSettingsOpt = parseConnectionSettings(line);

        final boolean loopEnabled = line.hasOption("loop");

        if (connectionSettingsOpt.isPresent()) {
            System.err.println("Getting tracked addresses...");
            final @NonNull ConnectionSettings connectionSettings = connectionSettingsOpt.get();

            // ChainID is non-essential for GetTrackedAddresses, so let it be just the default.
            try (final ClientConnector connector = connectionSettings.createClientConnector()) {
                do {
                    final Instant startTime = Instant.now();

                    final GetTrackedAddresses.Response response = connector.getObserver().getTrackedAddresses();
                    if (response.isFailure()) {
                        System.err.printf("ERROR: Could not get the tracked addresses! Problem: %s\n",
                                response.getFailure());
                    } else {
                        final GetTrackedAddresses.TrackedAddressesRequestResultPayload payload = response.getPayloadAsSuccessful();
                        final List<GetTrackedAddresses.TrackedAddressesRequestResultPayload.TrackedAddressInformation> trackedAddresses = payload.addresses;

                        System.err.printf("Tracked addresses (%s):\n", trackedAddresses.size());
                        for (GetTrackedAddresses.TrackedAddressesRequestResultPayload.TrackedAddressInformation addr : trackedAddresses) {
                            System.err.println(withOffset(2, addr.toHumanString()));
                        }
                    }
                    System.err.printf("--- Done in %s --\n", Duration.between(startTime, Instant.now()));
                } while (loopEnabled);
            } catch (Exception e) {
                System.err.printf("ERROR: Could not connect to UniCherryGarden! Exception %s\n", e);
                logger.error("Execution error", e);
            }
        }
    }

    private static void handleGetAddressDetails(@NonNull CommandLine line) {
        assert line != null;

        final Logger logger = LoggerFactory.getLogger(CherryGardenerCLI.class);

        printTitle(System.err);

        final @NonNull Optional<ConnectionSettings> connectionSettingsOpt = parseConnectionSettings(line);
        final @NonNull Optional<String> addressOpt = parseEthereumAddressOption(line, "get-address-details", true);

        if (true &&
                connectionSettingsOpt.isPresent() &&
                addressOpt.isPresent()
        ) {
            final @NonNull ConnectionSettings connectionSettings = connectionSettingsOpt.get();
            final @NonNull String address = addressOpt.get();

            System.err.printf("Getting details about address %s...\n", address);

            // ChainID is non-essential for GetAddressDetails, so let it be just the default.
            try (final ClientConnector connector = connectionSettings.createClientConnector()) {
                final Observer observer = connector.getObserver();

                final GetAddressDetails.Response response = observer.getAddressDetails(address);
                if (response.isFailure()) {
                    System.err.printf("ERROR: Could not get the details about address %s! Problem: %s\n",
                            address, response.getFailure());
                } else {
                    final GetAddressDetails.AddressDetailsRequestResultPayload payload = response.getPayloadAsSuccessful();
                    final GetAddressDetails.AddressDetailsRequestResultPayload.AddressDetails details = payload.details;

                    if (details.address.equals(address)) {
                        System.err.printf("Address %s:\n" +
                                        "---------------------------------------------------\n" +
                                        "Tracked by CherryPicker: %s\n" +
                                        "%s" +
                                        "%s\n",
                                address,
                                (details.trackedAddressInformation != null) ? "yes" : "no",
                                (details.trackedAddressInformation != null) ?
                                        String.format(
                                                "  %s\n",
                                                details.trackedAddressInformation.toHumanString()
                                        )
                                        :
                                        "",
                                details.nonces.toHumanString()
                        );
                    } else {
                        System.err.printf("ERROR: Requested details for address %s, but received for address %s!\n",
                                address, details.address);
                    }
                }
            } catch (Exception e) {
                System.err.printf("ERROR: Could not connect to UniCherryGarden! Exception %s\n", e);
                logger.error("Execution error", e);
            }
        }
    }

    private static void handleAddTrackedAddress(@NonNull CommandLine line) {
        assert line != null;

        final Logger logger = LoggerFactory.getLogger(CherryGardenerCLI.class);

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

            // ChainID is non-essential for AddTrackingAddress, so let it be just the default.
            try (final ClientConnector connector = connectionSettings.createClientConnector()) {
                final Observer observer = connector.getObserver();

                final AddTrackedAddresses.Response response = observer.startTrackingAddress(
                        address,
                        trackFromBlock.mode,
                        trackFromBlock.block,
                        commentOpt.orElse(null));
                if (response.isFailure()) {
                    System.err.printf("ERROR: Could not add the tracked address %s! Problem: %s\n",
                            address, response.getFailure());
                } else {
                    final AddTrackedAddresses.AddTrackedAddressesRequestResultPayload payload = response.getPayloadAsSuccessful();
                    final Set<String> addedAddresses = payload.addresses;
                    if (addedAddresses.size() == 1 && addedAddresses.stream().findFirst().get().equals(address)) {
                        System.err.printf("Address %s successfully added!\n", address);
                    } else {
                        System.err.printf("ERROR: Address %s failed to add!\n", address);
                    }
                }
            } catch (Exception e) {
                System.err.printf("ERROR: Could not connect to UniCherryGarden! Exception %s\n", e);
                logger.error("Execution error", e);
            }
        }
    }

    private static void handleGetBalances(@NonNull CommandLine line) {
        assert line != null;

        final Logger logger = LoggerFactory.getLogger(CherryGardenerCLI.class);

        printTitle(System.err);

        final @NonNull Optional<ConnectionSettings> connectionSettingsOpt =
                parseConnectionSettings(line);
        final @NonNull Optional<List<String>> addressesOpt =
                parseEthereumAddressesOption(line, "get-balances", true, true);
        final @NonNull Optional<Integer> confirmationsOpt =
                parseConfirmations(line);
        final boolean runInParallel = line.hasOption("parallel");

        if (true &&
                connectionSettingsOpt.isPresent() &&
                addressesOpt.isPresent() &&
                confirmationsOpt.isPresent()
        ) {
            final @NonNull ConnectionSettings connectionSettings = connectionSettingsOpt.get();
            final @NonNull List<String> addresses = addressesOpt.get();

            final int confirmations = confirmationsOpt.get();

            System.err.printf("Getting balances for %s with %s confirmation(s), %s...\n",
                    String.join(", ", addresses), confirmations,
                    (runInParallel ? "in parallel" : "sequentially"));

            final AbstractExecutorService executor = new ForkJoinPool(addresses.size());

            // ChainID is non-essential for GetBalances, so let it be just the default.
            try (final ClientConnector connector = connectionSettings.createClientConnector(confirmations)) {
                final Stream<String> addressStream = runInParallel ?
                        addresses.stream().parallel() :
                        addresses.stream();

                final Instant startTime = Instant.now();

                // Multiple queries – probably executed in parallel – lead to multiple responses.
                final List<GetBalances.@NonNull Response> responses = executor.submit(() ->
                        addressStream
                                .map((final String address) -> connector.getObserver().getAddressBalances(
                                        0, // on top of connector-level confirmations number
                                        address,
                                        null
                                ))
                                .collect(Collectors.toList())
                ).get();

                if (!responses.stream().allMatch(ResponseWithPayload::isSuccess) || addresses.size() != responses.size()) {
                    System.err.printf("ERROR: Could not get some of the balances for %s!\n", addresses);
                } else {
                    final Duration duration = Duration.between(startTime, Instant.now());

                    final List<GetBalances.BalanceRequestResultPayload> results =
                            responses.stream().map(CherryGardenResponseWithPayload::getPayloadAsSuccessful).collect(Collectors.toList());

                    System.err.printf("Received the balances for %s (with %s confirmation(s)), %d result(s) in %s:\n",
                            addresses, confirmations, results.size(), duration);

                    IntStream.range(0, results.size()).forEach((int i) -> {
                        final String address = addresses.get(i);
                        final GetBalances.BalanceRequestResultPayload payload = results.get(i);

                        System.err.printf("--- %d of %d: %s\n",
                                i + 1, results.size(), address);
                        for (final GetBalances.BalanceRequestResultPayload.CurrencyBalanceFact balanceFact : payload.balances) {
                            System.err.printf("  %s: %s (at block %s)\n",
                                    balanceFact.currency,
                                    balanceFact.amount,
                                    balanceFact.blockNumber
                            );
                        }
                        printSystemStatus(payload.systemStatus);
                    });
                    System.err.println("--- done.");
                }
            } catch (Exception e) {
                System.err.printf("ERROR: Could not connect to UniCherryGarden! Exception %s\n", e);
                logger.error("Execution error", e);
            } finally {
                executor.shutdownNow();
            }
        }
    }

    private static void handleGetTransfers(@NonNull CommandLine line) {
        assert line != null;

        final Logger logger = LoggerFactory.getLogger(CherryGardenerCLI.class);

        printTitle(System.err);

        final @NonNull Optional<ConnectionSettings> connectionSettingsOpt = parseConnectionSettings(line);
        final @NonNull Optional<Integer> confirmationsOpt = parseConfirmations(line);
        final @NonNull Optional<String> senderOpt = parseEthereumAddressOption(line, "sender", false);
        final @NonNull Optional<String> receiverOpt = parseEthereumAddressOption(line, "receiver", false);
        final @NonNull Optional<Integer> fromBlockOpt = parseBlockNumberOption(line, "from-block", false);
        final @NonNull Optional<Integer> toBlockOpt = parseBlockNumberOption(line, "to-block", false);

        final boolean withBalances = line.hasOption("with-balances");

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

                // ChainID is non-essential for GetTransfers, so let it be just the default.
                try (final ClientConnector connector = connectionSettings.createClientConnector(confirmations)) {
                    final GetTransfers.Response response = connector.getObserver().getTransfers(
                            0, // on top of connector-level confirmations number
                            senderOpt.orElse(null),
                            receiverOpt.orElse(null),
                            fromBlockOpt.orElse(null),
                            toBlockOpt.orElse(null),
                            null,
                            withBalances
                    );

                    if (response.isFailure()) {
                        System.err.printf("ERROR: Could not add the transfers! Problem: %s\n",
                                response.getFailure());
                    } else {
                        final GetTransfers.TransfersRequestResultPayload payload = response.getPayloadAsSuccessful();

                        System.err.printf("Received the transfers %s:\n", transfersDescription);

                        for (final MinedTransfer tr : payload.transfers) {
                            final String currencyName = tr.currencyKey.isEmpty() ? "ETH" : tr.currencyKey;
                            System.err.printf("  * %s %s from %s to %s (in tx %s from block %d), fees %s.\n",
                                    tr.amount, currencyName, tr.from, tr.to,
                                    tr.tx.txhash, tr.tx.block.blockNumber,
                                    tr.tx.fees
                            );
                        }
                        printSystemStatus(payload.systemStatus);
                    }
                } catch (Exception e) {
                    System.err.printf("ERROR: Could not connect to UniCherryGarden! Exception %s\n", e);
                    logger.error("Execution error", e);
                }
            }
        }
    }

    private static void printTitle(@NonNull PrintStream printStream) {
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

    private static void printSystemStatus(@NonNull SystemStatus systemStatus) {
        System.err.printf("" +
                        "Overall status, as of %s:\n" +
                        "%s\n" +
                        "%s\n",
                systemStatus.actualAt,
                withOffset(2,
                        (systemStatus.blockchain != null) ?
                                systemStatus.blockchain.toHumanString() :
                                "Blockchain: N/A"
                ),
                withOffset(2,
                        (systemStatus.cherryPicker != null) ?
                                systemStatus.cherryPicker.toHumanString() :
                                "UniCherryPicker: N/A"
                )
        );
    }

    private static void printHelp() {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(null); // don’t sort the options
        formatter.printHelp("java -jar cherrygardener", options);
    }

    public static void main(String[] args) {
        new CherryGardenerCLI(args);
    }
}
