package com.myodov.unicherrygarden.cherrygardener;

import com.myodov.unicherrygarden.cherrygardener.connector.api.types.Currency;
import com.myodov.unicherrygarden.cherrygardener.connector.impl.ClientConnectorImpl;
import org.apache.commons.cli.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


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
                null, "list-supported-currencies", false,
                "print the list of currencies supported by CherryGardener");
//        options.addOption("l", "launch", false, "launch the CherryGardener service and stay running");
//        options.addOption("p", "print", false, "print the information about the Bitcoin wallet, sufficient to restore it (on BIP32/BIP39 compatible wallets)");
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
                final ClientConnectorImpl connector = new ClientConnectorImpl("localhost:1234", "localhost:1235");

                final List<Currency> currencies = connector.getCurrencies();
                System.err.printf("Received list of currencies: %s\n", currencies);

                connector.shutdown();
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