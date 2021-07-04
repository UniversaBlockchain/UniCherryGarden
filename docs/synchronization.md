# Synchronization

## Settings

There are two places which specify what ranges of data we are tracking:

* `ucp_currency` – `sync_from` (block number). The earliest block from which we track this currency.
* `ucp_tracked_address` – `synced_from` (block number). The earliest block from which we track this address.

## Progress

There are several states where the progress is tracked independently:

* `overall_state` (`from_block_number`, `to_block_number`) tentative values, tracked in `ucp_state` table manually. Should be taken with a grain of salt – these values are serious only when the overall status is “fully synced”. When not fully synced, these values display only when it was previously fully synced.
* s

