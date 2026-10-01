# frozen_string_literal: true

require "minitest/autorun"
require_relative "../lib/ledger_bridge"

class LedgerBridgeTest < Minitest::Test
  def setup
    @service = LedgerBridge::PaymentService.new
  end

  def test_capture_is_idempotent_and_balanced
    payment = @service.create_payment(amount_cents: 12_500, idempotency_key: "create-1")

    first = @service.capture(payment_id: payment.id, idempotency_key: "capture-1")
    second = @service.capture(payment_id: payment.id, idempotency_key: "capture-1")

    assert_same first, second
    assert_equal :succeeded, payment.status
    assert_equal 12_500, payment.captured_cents
    assert @service.ledger.balanced?
    assert_equal 2, @service.ledger.entries.count
  end

  def test_idempotency_key_cannot_be_reused_for_different_request
    @service.create_payment(amount_cents: 5_000, idempotency_key: "same-key")

    assert_raises(LedgerBridge::IdempotencyConflict) do
      @service.create_payment(amount_cents: 9_000, idempotency_key: "same-key")
    end
  end

  def test_partial_refund_posts_reverse_entries
    payment = @service.create_payment(amount_cents: 10_000, idempotency_key: "create-2")
    @service.capture(payment_id: payment.id, idempotency_key: "capture-2")
    @service.refund(payment_id: payment.id, amount_cents: 2_500, idempotency_key: "refund-1")

    assert_equal 2_500, payment.refunded_cents
    assert_equal 4, @service.ledger.entries.count
    assert @service.ledger.balanced?
    assert_equal 7_500, @service.ledger.balance("merchant:#{payment.id}")
  end

  def test_refund_cannot_exceed_capture
    payment = @service.create_payment(amount_cents: 1_000, idempotency_key: "create-3")
    @service.capture(payment_id: payment.id, idempotency_key: "capture-3")

    assert_raises(LedgerBridge::DomainError) do
      @service.refund(payment_id: payment.id, amount_cents: 1_001, idempotency_key: "refund-too-big")
    end
  end

  def test_unbalanced_postings_are_rejected
    ledger = LedgerBridge::Ledger.new

    assert_raises(LedgerBridge::UnbalancedLedger) do
      ledger.post(
        payment_id: "p",
        currency: "usd",
        kind: "capture",
        postings: [
          { account: "merchant", amount_cents: 500 },
          { account: "platform", amount_cents: -499 }
        ]
      )
    end
  end
end
