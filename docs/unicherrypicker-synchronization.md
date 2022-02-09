# UniCherryPicker Synchronization

## Problems

UniCherryPicker (or just “CherryPicker”) does the task of synchronizing the Ethereum blockchain to the regular relational DB (PostgreSQL in the default and most tested implementation).

It supports “cherry-picking” (hence the name) of specific currencies (ERC20 tokens) and addresses, ignoring any transactions/transaction logs that are not related to the chosen ones.

There are some usage scenarios that should be carefully considered for the synchronization implementation:

### CherryPicker lagging behind the Ethereum node

In various scenarios when CherryPicker just starting up, or has been shut down for a while, it may lag behind the Ethereum node status. This may happen as far as hundreds thousands or even millions of blocks (especially on first start up).

CherryPicker must re-sync the big batches as fast as it can, when there are big distances to resync.

#### ... low resource utilization otherwise

When CherryPicker is synced at least to the `eth.syncing.currentBlock` block of Ethereum node (though not necessary to the `highestBlock`), it is not expected to have much data soon – especially if `eth.syncing.currentBlock` = `eth.syncing.highestBlock` meaning that the Ethereum node is fully synced itself. A fully synced Ethereum node gets a new block about each 10–15 seconds (normally about 13 seconds), so CherryPicker doesn’t need to poll the Ethereum node faster than about once per 10 seconds.  


### Ethereum node lagging behind the blockchain status

Ethereum node itself may be lagging and not fully synced to the blockchain (`eth.syncing.currentBlock` < `eth.syncing.highestBlock`). We shouldn’t assume it is synced; it may be just in the process of being synced (and that may take long, hours, even days).

#### ... attention: `eth.syncing.currentBlock` may even move backwards!

If we switched the Ethereum node, or rebooted/wiped it, CherryPicker may suddenly see a node which is synced to the older blocks than CherryPicker itself! It cannot do much in this case, just have to wait for the node to be synced. The whole syncing process should happen up to `eth.syncing.currentBlock` only, not to the `eth.syncing.highestBlock`.


### Block reorganization

A most annoying but otherwise possible and expected scenario is when some recent blocks in the blockchain suddenly start to differ from what we’ve seen before: the hashes of their contents changed. This may happen not to just the most recent, but to multiple blocks in the end (though the length of such reorganization is expected to be reasonably short; people know it may happen but do not expect it to be longer than like 6–12 blocks because that’s the amount of “confirmations” many assume to be sufficient for the transactions to be treated as finished). 

### Adding new currencies/addresses and resyncing the old blocks

When we added some new currency or address to track, we may specify some old block number to track it from. This may mean the information from the blockchain must be reread from that block up to the recent blocks.

#### ... should not impede the most-recent data access

But these added currencies or addresses, and the rereading process, should not stop the most-recent blocks to be resynced. The other data in the database is still valid; and if the blockchain is more-or-less synced for the other tokens/addresses, it should continue to sync for them, even though some older tokens are promptly being resynced.


## Settings

Before using the system, we need to setup some global settings, as well as (later) the settings for currencies and addresses which we are going to track (usage scenario settings). 

### Global settings

Some of the settings (which are not indended to be changed often; probably – never) are stored in the `application.conf` file for the application. When launching UniCherryGarden, you may want to specific the `-Dconfig.file=path-to-application.conf` JRE setting. Otherwise, the default settings from the `reference.conf` will be used.

There are also some settings which are specified using the database (`ucg_state` table).

#### `application.conf` file settings

`application.conf` file is a regular HOCON file. There may be some settings used by Akka components and by other subsystems. Here is the schema of UniCherryGarden-specific settings, and their default values:

```hocon
unicherrygarden {
  cherrypicker {
    syncers {
      max_reorg = 100 # >= 1
      head_syncer {
        batch_size = 100 # >= 1
        catch_up_brake_max_lead = 10000 # >= max(head_syncer.batch_size, tail_syncer.batch_size)
      }
      tail_syncer {
        batch_size = 100 # >= 1
      }
    }
  }
  cherryplanter {
  }
  cherrygardener {
  }
}
```

* `syncers.max_reorg` – maximum length of reorg (number of blocks in the blockchain mismatching what we’ve seen in the blockchain before) we expect.
* `syncers.head_syncer.batch_size`, `syncers.tail_syncer.batch_size` – maximum number of blocks in a batch-read; the larger the batch, the longer the query and the more it takes from geth to respond.
* `syncers.head_syncer.catch_up_brake_max_lead` – sometimes both HeadSyncer and TailSyncer are running an at the same time, and TailSyncer trying to catch up (imagine TailSyncer syncing from block 2500, HeadSyncer syncing from block 3000, they both sync in batches of 100 blocks, and they need to reach block 5000). Especially if `head_syncer.batch_size` ≥ `tail_syncer.batch_size`, this may mean TailSyncer rescans many of the blocks just passed by HeadSyncer. `catch_up_brake_max_lead` is the setting for HeadSyncer to pause running forward, if it notices that TailSyncer is closer than `catch_up_brake_max_lead`; so TailSyncer may reach HeadSyncer faster, and only one of them will need to run further.

See the further details of synchronization algorithm for the specifics of these settings usage. 

#### `ucg_state` DB settings

The `ucg_state` stores the most important/most persistent “runtime state” of the UniCherryGarden system, visible from outside.

* `synced_from_block_number` field: integer value, specifies the earliest block of the blockchain we are interested in. It will be the first block stored in `ucg_block`. Choose carefully; most of the operations and blockchain data syncing do not allow to ever go earlier than this block. That is: if you specified value **1,000,000** for `synced_from_block_number`, no block, currency or address will be ever tracked earlier than that. This may change in the future though.   


### Usage scenario settings (DB)

There are two places which specify what ranges of data we are tracking:

* `ucg_currency` table – `sync_from` (block number). The earliest block from which we track this currency.
* `ucg_tracked_address` table – `synced_from` (block number). The earliest block from which we track this address.


## Architecture

The architecture of the syncing solution that resolves all the problems from the “Problems” section, is described below.

(Note: The real implementation may be different in detail (and may change in the future), and may have some more, newer, specifics implemented. As of late 2021, this is the primary guide to grok the overall CherryPicker synchronization idea.)

The syncing process is split to two primary subsystems, **HeadSyncer** and **TailSyncer**. There is also a tiny helper, **EthereumStatePoller**.

**EthereumStatePoller** is the component that just polls the Ethereum node for the node’s syncing status, once per about 10 seconds (because in full-sync condition, it isn’t expected to change earlier than in 10 seconds), and notifies both HeadSyncer and TailSyncer about it (so they don’t have to do it themselves).

**HeadSyncer** is the subsystem that syncs the most recent blocks (after the ones which are already stored in the `ucg_block` table). It’s the component that will poll the blocks after the full-sync has been achieved (and will do it slowly, just about once in 10 seconds).

In general it does everything that is related to the “head” (most recent data) of the stored blocks.

**TailSyncer** is the subsystem that syncs and resyncs the “older data”, like, the blocks that have to be resynced due to adding a new currency or token.

Both subsystems run concurrently. They do not take the responsibilities from each other, and they can even communicate with each other. Both of them can “batch” their requests, getting multiple blocks at once (to speed up the execution). Both of them can run their “next iterations” immediately (in some cases), or with a specified delay (about 10 seconds), depending on their state and progress.

### EthereumStatePoller

Very simple timer that polls the Ethereum node for its syncing status (`eth.syncing` using JSON-RPC or GraphQL), and passes this information to both syncers.

### HeadSyncer

This is the subsystem that syncs the most recent data; and the subsystem that syncs the new data when just launching the system.

On each iteration (assuming it has received the current Ethereum syncing status from the EthereumStatePoller), it does the following:

1. First, it tries to reorg check/rewind the most recent blocks; but only if
  * this is the very first iteration of HeadSyncer since the launch (because who knows, maybe we have shut down the CherryPicker right on the blocks that were reorged); or
  * if the most recent block in `ucg_block` is within the `syncers.max_reorg` distance (see the “Settings” section!) from the `eth.syncing.highestBlock`; this is what `reiterateMayCheckReorg` (see below) decides on its “may” part.
2. Then it iterates/resync the most recent blocks needed to sync.

Inner FSM states related to the iterations:

* **`reiterate`** (common with TailSyncer);
* **`pauseThenReiterate`** (common with TailSyncer): timer → `iterate`;
* `reiterateMustCheckReorg` – (on first launch, and on errors) → `syncBlocks`; “run iteration but must check for reorg before it”;
* `reiterateMayCheckReorg` – (on subsequent launches) synonim to `reiterate`;
* `syncBlocks`: do the actual block syncing;
* `pauseThenMustCheckReorg`;
* `pauseThenMayCheckReorg` – synonym to `pauseThenReiterate`.

Any error in any iteration causes HeadSyncer to delay (for like 10 seconds) and then switch to `reiterateMustCheckReorg` state; i.e. going to `pauseThenMustCheckReorg` state.

#### Reorg check/rewinding

Consists of four phases:

1. Is node reachable? – does the data in the DB and in the Ethereum node sufficient for us to even do anything?
2. Reorg check check (read this as “check for reorg check”) – do we really need to check for reorg?
3. Reorg check – did reorg occur? is rewinding needed?
4. Rewinding – actual deletion from the database.

Most of the reorg-checking decisions are taken upon these several most important numbers:

* `min(ucg_block.number)` – the earliest block stored in our DB.
* `max(ucg_block.number)` – the newest block stored in our DB.
* `eth.syncing.currentBlock` – the block that the Ethereum node reports is the latest known to it/synced to it.
* `eth.syncing.highestBlock` – the block that the Ethereum node reports is the latest which it is aware about, but may not have the full information about it; typically `eth.syncing.highestBlock` ≥ `eth.syncing.currentBlock`.
* `syncers.max_reorg` – the configuration setting defining the number of blocks how far we do even check for reorgs.

##### 1. Is node reachable

If `max(ucg_block.number)` > `eth.syncing.currentBlock`, it means the Ethereum node was probably wiped/restarted (and our DB has more information than the node) and is still being synced; so we should not go further, and go to `pauseThenMustCheckReorg` state instead, waiting until the Ethereum node syncs-up further to be able to even ask it for reorg data.

Another case for us to think the actual-state node is “unreachable”, is if our recent `eth.syncing` data from the Ethereum node is even unavailable.

(Interestingly, if there are no blocks in `ucg_block` – that’s okay and we go to the next step; at least we didn’t fail).

##### 2. Reorg check check

Otherwise, if the basic data sanity is ensured (and “the node is reachable”), we going further to question if we need to check for reorg.

Checking for reorg is a resource-consuming operation, too – at least, it will need to get the hashes for a number of blocks from the DB and from the Ethereum node; think about like 100 records from DB and 100 records from the Ethereum node.

We actually do not need to check for reorg very often, even when moving the HeadSyncer forward. There are some cases when we *must* do it (like, when we launch the system for the first time). On most of the subsequent iterations we *may* do it.

The HeadSyncer internal state contains a flag `nextIterationMustCheckReorg` which is related to the “must”-condition. If it is set, we *undoubtedly do* the reorg check phase – i.e. the reorg reorg check **is needed**. If it is unset, we still “may” check the reorg.

The conditions for the “we may do reorg check but should we?” question are following:

* If there are no blocks in `ucg_block`, reorg check **is not needed** (as we don’t have any blocks to check).
* If `max(ucg_block.number)` < `eth.syncing.currentBlock - syncers.max_reorg`, it means we are very far from the current tip of the blockchain, and we are hurrying to sync it up to it. We did the reorg-check once after the bootup (in some “mustCheckReorg” condition); but now, as we are in hurry, the reorg check **is not needed**.
* Otherwise, we are in a situation when the latest of our stored blocks is within the “danger zone” for the reorg. For the utmost safety, the reorg check **is needed**.

##### 3. Reorg check

If “reorg check check” decided we need to do the reorg check we do it:

`reorgCheckEndBlock` to read: `max(ucg_block.number).`

`reorgCheckStartBlock` to read: `greatest(min(ucg_block.number), reorgCheckEndBlock - syncers.max_reorg + 1)`

Then, knowing the `reorgCheckStartBlock` and `reorgCheckEndBlock` values, we ask the Ethereum node for just the hashes of all blocks in `[reorgCheckStartBlock..reorgCheckEndBlock]` range (any error, as usual, causes it to go to `pauseThenMustCheckReorg`).

We compare the results with the data in our database (basically, we get the most recent blocks stored in `ucg_block`, as much as available up to `syncers.max_reorg`). The first block where the hash doesn’t match, is assumed the first block to rewind.

##### 4. Rewind

`rewindStartBlock`: the first block in our `ucg_block` whose hash didn’t match the data from the Ethereum node.
`rewindEndBlock`: `max(ucg_block.number)`.

We loop from `rewindEndBlock` downwards to `rewindStartBlock` (inclusive). Not the vice versa (because we cannot delete a block which is being referred by another block, due to DB constraints!)

For each `blockNumberToRewind`, we:

1. delete any `ucg_tx_log` records referring to any transactions referring to the block number `blockNumberToRewind`.
2. delete any `ucg_transaction` records referring to the block number `blockNumberToRewind`.
3. delete the `ucg_block` record with number `blockNumberToRewind`.
4. set any `ucg_currency_tracked_address_progress.synced_to_block_number` to `blockNumberToRewind - 1` if it was equal to `blockNumberToRewind` before.
5. commit the transaction.

After the rewind it just switches to `syncBlocks`.

#### Sync blocks

This is the regular syncing of the latest blocks:

* `syncStartBlock`: `ucg_block.number + 1` (or maybe from `ucg_state.synced_from_block_number` if there are no records in `ucg_block`)
* `syncEndBlock`: `eth.syncing.currentBlock`, but with a batch no larger than `head_syncer.batch_size`.

Interestingly, we don’t always do the actual sync, even if we can. If we received a notification from TailSyncer that its last block to sync is within `head_syncer.catch_up_brake_max_lead` from `syncStartBlock`, we assume that TailSyncer is very close, and can wait for it to reach us; so we go to `pauseThenMayCheckReorg` state instead.

Otherwise, we just poll the blocks from Ethereum connector, and write them to DB.

After writing the blocks to the DB, we decide should we go to `pauseThenMayCheckReorg` or to `reiterateMayCheckReorg`:

* If we have reached `eth.syncing.currentBlock`, we go to `pauseThenMayCheckReorg` state.
* Otherwise we go to `reiterateMayCheckReorg` state.

### TailSyncer

This is the subsystem that syncs the older data (e.g. when some currencies or tokens have been added with `sync_from_block_number` somewhere long ago).

Inner FSM states related to the iterations:

* **`reiterate`** (common with HeadSyncer);
* **`pauseThenReiterate`** (common with HeadSyncer): timer → `iterate`;
* `reiterate`: → `iterate`;
* `iterate`: → `headSync`;
* `headSync`: do the actual block syncing.

`syncStartBlock`: among all the (currency, tracked_address) M2M records (`ucg_currency_tracked_address_progress` table), find the least value of the following two:

* for all the `ucg_currency_tracked_address_progress` records where the record exist already for (currency, tracked_address) pair: `min(ucg_currency_tracked_address_progress.synced_to_block_number) + 1`. I.e. this is the lowest number that any m2m record has been synced to (it may be lower than others).
* for all the `ucg_currency_tracked_address_progress` records where the record doesn’t exist yet for (currency, tracked_address) pair: `greatest(currency.sync_from_block_number, tracked_address.synced_from_block_number)`. I.e. for each record this is the block from which we should start syncing it; and get `min()` of all such blocks.

`syncEndBlock`: `eth.syncing.currentBlock`, but with a batch no larger than `tail_syncer.batch_size`.

As with HeadSyncer, any error (including cases like `syncEndBlock` < `syncStartBlock`) would lead to `pauseThenReiterate` state.

After calculating `syncStartBlock` and `syncEndBlock`, we should think, should we actually perform the sync? Won’t it actually reach/overlay the next HeadSyncer iteration? Did it reach the HeadSyncer?

The answer is simple:

* if `syncStartBlock` = `max(ucg_block.number) + 1` (that is, if all `ucg_currency_tracked_address_progress` records exist and have already reached the same end), we don’t do anything, it’s HeadSyncer task to sync further; → `pauseThenReiterate`.
* otherwise → `tailSync` – do the actual block syncing.

#### Sync blocks

First, we send a message to our mate HeadSyncer to inform it about our `syncStartBlock` and `syncEndBlock` –what we are going to do. So the HeadSyncer can brake down to allow us to catch up if needed (see the `syncers.head_syncer.catch_up_brake_max_lead` setting).

Then we sync these blocks.

After it we switch to either `pauseThenReiterate` or `iterate` state:

* if `syncEndBlock` = `eth.syncing.currentBlock`, we’ve fast-synced as far as we can, not need to hurry anymore. → `pauseThenReiterate`;
* otherwise, we need to fast-sync further: → `iterate`.
