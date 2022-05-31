package com.myodov.unicherrygarden.connector.impl;

import com.myodov.unicherrygarden.connector.api.Sender;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * Test the {@link SenderImpl} class, on Mainnet network.
 * <p>
 * For better usage, you should have a mainnet node available; and a HOCON config file containing
 * the data to utilize it.
 * <p>
 * Using config file: `-Dconfig.file=conf/conf/config-local-specs-nogit.conf`
 * <p>
 * Example of config file:
 *
 * <pre>
 *  unicherrygarden {
 *    ethereum {
 *      rpc_servers = ["http://localhost:8545"]
 *    }
 *  }
 * </pre>
 */
public class SenderImplMainnetTest {
    final Logger logger = LoggerFactory.getLogger(SenderImplMainnetTest.class);

    private static final Config config = ConfigFactory.load();
    private static final List<String> nodeUrls = config.getStringList("unicherrygarden.ethereum.rpc_servers");

    private static final SenderImpl sender = new SenderImpl();

    @Test
    public void testFeeSuggestion() {
        final Sender.FeeSuggestion feeSuggestion = sender.suggestFees();
        System.err.println(feeSuggestion);
    }
}
