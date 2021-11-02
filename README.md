# UniCherryGarden: universal “gardening” solution for Ethereum blockchain data

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

The packages are being built and published to Sonatype Central repository, so most of the times you can use in the Java build tool of your choice very easily. For example, if the currently published version is **0.1.3**, you can use this package in the following way:


#### Maven

~~~maven
<dependency>
    <groupId>com.myodov.unicherrygarden</groupId>
    <artifactId>cherrygardener_connector</artifactId>
    <version>0.1.3</version>
</dependency>

~~~

#### Gradle

~~~gradle
implementation group: 'com.myodov.unicherrygarden', name: 'cherrygardener_connector', version: '0.1.3'
~~~

#### sbt

~~~sbt
libraryDependencies += "com.myodov.unicherrygarden" % "cherrygardener_connector" % "0.1.3"
~~~

#### ivy

~~~ivy
<dependency org="com.myodov.unicherrygarden" name="cherrygardener_connector" rev="0.1.3"/>
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


### API

After creating the Connector instance, it will enable the interfaces available at [/unicherrygarden/connector/api](/java/cherrygardener_connector/src/main/java/com/myodov/unicherrygarden/connector/api).

For example, the Connector itself has the following API: […/api/ClientConnector.java](/java/cherrygardener_connector/src/main/java/com/myodov/unicherrygarden/connector/api/ClientConnector.java).

This API allows you to use whole-CherryGarden level operations, like 

```java
default List<Currency> getCurrencies();
```

But for some other operations, they are not available from the Connector directly, as they are not overall-UniCherryGarden-level. These operations are grouped by area of functionality (sub-APIs), and to access them, you get an instance of specific sub-API functionality executor:

```java
AddressOwnershipConfirmator getAddressOwnershipConfirmator();

Keygen getKeygen();

Observer getObserver();
```

These sub-APIs are overviewed below.

After using the Connector, and when you are done with all its calls, you may want to explicitly shutdown the connector (and stop all the network activity):

```java
/**
 * Stop whole connector to shut it down.
 */
void shutdown();
```

If you want more specific code examples of using the Connector, you may find the source code of `cherrygardener_connector_cli` amusing and useful: […/CherryGardenerCLI.java](/java/cherrygardener_connector_cli/src/main/java/com/myodov/unicherrygarden/cherrygardener/CherryGardenerCLI.java).


Below is the overview of all sub-APIs available from the connector:

#### AddressOwnershipConfirmator sub-API

This API allows you to ask the user to confirm that they own a private key to some specific Ethereum address. To confirm it, the user signs some message you provide; and then you can verify this signature.

See more at […/api/AddressOwnershipConfirmator.java](/java/cherrygardener_connector/src/main/java/com/myodov/unicherrygarden/connector/api/AddressOwnershipConfirmator.java).

#### Keygen sub-API (WIP)

This API allows you to generate the private key to use with Ethereum blockchain

See more at […/api/Keygen.java](/java/cherrygardener_connector/src/main/java/com/myodov/unicherrygarden/connector/api/Keygen.java).

#### Observer sub-API

This API allows you to observe the collected information from Ethereum blockchain. In general, this is the interface to the “CherryPicker” component of UniCherryGarden.

See more at […/api/Observer.java](/java/cherrygardener_connector/src/main/java/com/myodov/unicherrygarden/connector/api/Observer.java).

#### Sender sub-API (WIP)

This API allows you to generate and send the new transactions to the Ethereum blockchain. In general, this is the interface to the “CherryPlanter” component of UniCherryGarden.

See more at […/api/Sender.java](/java/cherrygardener_connector/src/main/java/com/myodov/unicherrygarden/connector/api/Sender.java).


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
