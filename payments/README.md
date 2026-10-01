# LedgerBridge — Ruby + Scala payments lab

LedgerBridge is a payments-focused subsystem inside PocketAlpha built specifically to practice two unfamiliar languages on a domain where correctness matters.

It is intentionally **not** a real payment processor and does not connect to Stripe or move money. The goal is to model the engineering problems behind a payments platform: idempotency, state transitions, double-entry accounting, refunds, settlement reconciliation, and invariant checking.

## Architecture

```text
             payment request
                   |
                   v
        +-----------------------+
        | Ruby payment service  |
        |-----------------------|
        | PaymentIntent FSM     |
        | idempotency keys      |
        | capture / refund      |
        | double-entry ledger   |
        +-----------+-----------+
                    |
               ledger state
                    |
                    v
        +-----------------------+
        | Scala reconciler      |
        |-----------------------|
        | settlement ingest     |
        | duplicate detection   |
        | arithmetic checks     |
        | amount matching       |
        | orphan detection      |
        +-----------------------+
```

## Ruby side

The Ruby implementation models a compact payment lifecycle:

```text
requires_payment_method
        |
        v
 requires_capture
        |
        v
    succeeded
        |
        +---- partial/full refunds
```

Each capture/refund posts a balanced pair of ledger entries. An operation is rejected if the postings do not net to zero.

Idempotency keys are stored with a request fingerprint. Replaying the same request returns the original result; reusing the key for a different request raises an explicit conflict.

Run:

```bash
ruby payments/ruby/bin/demo.rb
ruby -Ipayments/ruby/lib payments/ruby/test/ledger_bridge_test.rb
```

## Scala side

The Scala reconciler compares internal captured/refunded amounts with processor settlement records and detects:

- missing settlements,
- settlements with no matching internal payment,
- duplicate settlement rows,
- invalid `gross - fee = net` arithmetic,
- internal-vs-settlement amount mismatches.

Run with Scala CLI:

```bash
scala-cli run   payments/scala/src/main/scala/Reconciliation.scala   payments/scala/src/main/scala/Main.scala

scala-cli run   payments/scala/src/main/scala/Reconciliation.scala   payments/scala/src/test/scala/ReconciliationCheck.scala
```

## End-to-end Ruby → Scala path

The CI workflow also runs a cross-language contract:

```text
Ruby payment service
   |
   | exports internal_payments.csv + settlements.csv
   v
Scala reconciliation CLI
   |
   +--> verifies all payments settle exactly once
   +--> exits non-zero on any reconciliation issue
```

Run it locally:

```bash
ruby payments/ruby/bin/export_reconciliation_fixture.rb /tmp/ledgerbridge

scala-cli run \
  payments/scala/src/main/scala/Reconciliation.scala \
  payments/scala/src/main/scala/ReconciliationCli.scala -- \
  /tmp/ledgerbridge/internal_payments.csv \
  /tmp/ledgerbridge/settlements.csv
```

A clean run reports `matched=3` and `issues=0`.

## What this project demonstrates

This code is evidence of **hands-on Ruby and Scala usage**, but the honest framing is still that both are newer languages for this project. The interesting part is applying existing systems fundamentals—state machines, invariants, idempotency, accounting consistency, typed data modeling, and reconciliation—in unfamiliar language ecosystems.

That is much stronger than listing either language based on syntax exercises alone.
