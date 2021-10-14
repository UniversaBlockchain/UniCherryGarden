# UniCherryGarden: universal “gardening” solution for Ethereum blockchain data.

## About

**CherryGarden** is the convenient set of utilities capable of working with Ethereum blockchain safely. It consists of several components, such as:

* **CherryPicker** – high-level watcher over the Ethereum blockchain data; converts the Ethereum blocks into the database-stored and SQL-readable data. “High-level” means that you now can not just see the regular Ethereum data (such as ETH transfers, block information, etc) but even analyze the ERC20 token transfers (which are not available in the blockchain data directly). CherryPicker is capable of “cherry-picking” the data, watching for only specific ERC20 tokens and specific addresses, saving a lot of disk space.
* **CherryPlanter** – “plants” the Ethereum transactions in the Ethereum blockchain. Doesn’t store any private keys, relying on the client to store them (and ensure their privacy).
* **CherryGardener** – the “master gardener”, providing a convenient Java client interface to all the CherryGarden features.
* **GardenWatcher** – an extra component (normally not used directly) to support the Akka cluster availability.

Most CherryGarden components are written on Scala (and use Akka module/actor management and PostgreSQL for data storage). CherryGardener client API uses Java for better integration with other languages.

* **CherryGardener Connector** – is a Java-based API to connect to CherryGardener and execute any required operations from Java/Scala/Kotlin code.


## Usage

The native Java API, in the form of `ClientConnectorImpl`, is available in the `cherrygardener_connector` package.

### Setting CherryGardener Connector as a dependency

The packages are being built and published to Sonatype Central repository, so most of the times you can use in the Java build tool of your choice very easily. For example, if the currently published version is **0.1.1**, you can use this package in the following way:


#### Maven

~~~maven
<dependency>
    <groupId>com.myodov.unicherrygarden</groupId>
    <artifactId>cherrygardener_connector</artifactId>
    <version>0.1.1</version>
</dependency>

~~~

#### Gradle

~~~gradle
implementation group: 'com.myodov.unicherrygarden', name: 'cherrygardener_connector', version: '0.1.1'
~~~

#### sbt

~~~sbt
libraryDependencies += "com.myodov.unicherrygarden" % "cherrygardener_connector" % "0.1.1"
~~~

#### ivy

~~~ivy
<dependency org="com.myodov.unicherrygarden" name="cherrygardener_connector" rev="0.1.1"/>
~~~

### Usage in the code

After you’ve made a dependency on `cherrygardener_connector`, you have the Java API interfaces available in `com.myodov.unicherrygarden.connector.api`, and the default implementation in `com.myodov.unicherrygarden.connector.impl`.

Create an instance of the connector in this way:

~~~java
import com.myodov.unicherrygarden.connector.api.ClientConnector;
import com.myodov.unicherrygarden.connector.impl.ClientConnectorImpl;

// ...

    final List<String> urls = List.of("127.0.0.1:2551", "127.0.0.1:2552");
    final ClientConnector connector = new ClientConnectorImpl(urls);
~~~

`ClientConnectorImpl` class constructor takes a list of URLs as the arguments; normally these are the URLs for CherryGardener and GardenWatcher instances (the order doesn’t matter). Most of the time, you want to launch at least a single instance of CherryGardener, and at least a single instance of GardenWatcher, and upgrade them in sequence (so at least one of them always up while the other is being upgraded), to maintain the cluster connectivity.

You shouldn’t pass `null` instead of the list to the constructor; but you can pass an empty list, implying you don’t want the ClientConnector to connect to anything. It will try to work in “offline mode” then, having some of its functionality unavailable (which requires the connection to the CherryGardener) but still have some functionality available (which runs on the address space of the connector – such as, generation of private keys, or confirmation/validation of addresses through signing the messages).


## Building

Build it using sbt tool:

```sh
sbt universal:packageBin
```

The result is in `launcher/target/universal/`


## Architecture

Uses Akka library for modularity/components and establishing cluster of services.

PostgreSQL database is used to store and efficiently index the collected blockchain data and the internal state.

The code is written in Java 8 language and Scala 2.13.

* The Java artifacts are built without using the Scala versioning in their titles, and in JDK8 compatible mode.
* The Scala artifacts are built with the Scala version in their title (cross-building).


## Authors

The project is sponsored by [Universa Blockchain](https://universablockchain.com),
the enterprise/country-grade DLT with true smart contracts (rather than dApps) and data-less decentralized storage and verification of smart contract validity.


## License

UniCherryGardener is licensed under the MIT license,
also included in our repository in the LICENSE file.
