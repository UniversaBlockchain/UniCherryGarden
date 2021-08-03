package com.myodov.unicherrygarden.cherrygardener;

import com.myodov.unicherrygarden.cherrygardener.connector.api.types.Currency;
import com.myodov.unicherrygarden.cherrygardener.connector.impl.ClientConnectorImpl;
import org.apache.commons.cli.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
//        options.addOption("l", "launch", false, "launch the CherryGardener service and stay running");
//        options.addOption("p", "print", false, "print the information about the Bitcoin wallet, sufficient to restore it (on BIP32/BIP39 compatible wallets)");
    }

    private Optional<List<String>> parseConnectUrls(@NonNull CommandLine line) {
        if (line.hasOption("connect")) {
            final List<String> connectUrls = Arrays.asList(line.getOptionValue("connect").strip().split(","));
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
                System.err.println("Listing currencies...");
                final Optional<List<String>> connectUrls = parseConnectUrls(line);
                if (connectUrls.isPresent()) {
                    try {
                        final ClientConnectorImpl connector = new ClientConnectorImpl(connectUrls.get());
                        final List<Currency> currencies = connector.getCurrencies();
                        System.err.printf("Received list of currencies: %s\n", currencies);
                        connector.shutdown();
                    } catch (CompletionException exc) {
                        System.err.println("Could not connect to UniCherryGarden!");
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

    private static final void printHelp() {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar cherrygardener", options);
    }

    public static void main(String[] args) {
        new CherryGardenerCLI(args);
    }
}