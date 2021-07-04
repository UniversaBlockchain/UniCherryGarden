# UniCherryGarden: universal “gardening” solution for Ethereum blockchain data.

## About

**CherryGarden** is the convenient set of utilities capable of working with Ethereum blockchain safely. It consists of several components, such as:

* **CherryPicker** – high-level watcher over the Ethereum blockchain data; converts the Ethereum blocks into the database-stored and SQL-readable data. “High-level” means that you now can not just see the regular Ethereum data (such as ETH transfers, block information, etc) but even analyze the ERC20 token transfers (which are not available in the blockchain data directly). CherryPicker is capable of “cherry-picking” the data, watching for only specific ERC20 tokens and specific addresses, saving a lot of disk space.
* **CherryPlanter** – “plants” the Ethereum transactions in the Ethereum blockchain. Doesn’t store any private keys, relying on the client to store them (and ensure their privacy).
* **CherryGardener** – the “master gardener”, providing a convenient Java client interface to all the CherryGarden features.
* **GardenWatcher** – an extra component (normally not used directly) to support the Akka cluster availability.

Most CherryGarden components are written on Scala (and use Akka module/actor management and PostgreSQL for data storage). CherryGardener client API uses Java for better integration with other languages.

* **CherryGardener Connector** – is a Java-based API to connect to CherryGardener and execute any required operations from Java/Scala/Kotlin code.


## Building

Build it using sbt tool:

```sh
sbt universal:packageBin
```

The result is in `launcher/target/universal/`


## Architecture

Uses Akka library for modularity/components and establishing cluster of services.

PostgreSQL database is used to store and efficiently index the collected blockchain data and the internal state.


## Authors

The project is sponsored by [Universa Blockchain](https://universablockchain.com),
the enterprise/country-grade DLT with true smart contracts (rather than dApps) and data-less decentralized storage and verification of smart contract validity.


## License

UniCherryGardener is licensed under the MIT license,
also included in our repository in the LICENSE file.
