# frozen_string_literal: true

require "securerandom"
require "time"

module LedgerBridge
  class DomainError < StandardError; end
  class IdempotencyConflict < DomainError; end
  class InvalidTransition < DomainError; end
  class UnbalancedLedger < DomainError; end

  Entry = Data.define(:entry_id, :payment_id, :account, :amount_cents, :currency, :kind, :created_at)

  class Ledger
    attr_reader :entries

    def initialize
      @entries = []
    end

    def post(payment_id:, currency:, postings:, kind:)
      sum = postings.sum { |posting| Integer(posting.fetch(:amount_cents)) }
      raise UnbalancedLedger, "postings must net to zero (got #{sum})" unless sum.zero?

      timestamp = Time.now.utc.iso8601(6)
      batch = postings.map do |posting|
        Entry.new(
          entry_id: SecureRandom.uuid,
          payment_id: payment_id,
          account: posting.fetch(:account),
          amount_cents: Integer(posting.fetch(:amount_cents)),
          currency: currency,
          kind: kind,
          created_at: timestamp
        )
      end
      @entries.concat(batch)
      batch
    end

    def balance(account, currency: "usd")
      @entries
        .select { |entry| entry.account == account && entry.currency == currency }
        .sum(&:amount_cents)
    end

    def balanced?
      @entries.group_by { |entry| [entry.payment_id, entry.kind] }.all? do |_key, entries|
        entries.sum(&:amount_cents).zero?
      end
    end
  end

  class PaymentIntent
    STATES = %i[requires_payment_method requires_capture succeeded canceled].freeze

    attr_reader :id, :amount_cents, :currency, :status, :captured_cents, :refunded_cents

    def initialize(amount_cents:, currency: "usd", id: SecureRandom.uuid)
      @id = id
      @amount_cents = Integer(amount_cents)
      raise DomainError, "amount must be positive" unless @amount_cents.positive?

      @currency = currency.downcase
      @status = :requires_payment_method
      @captured_cents = 0
      @refunded_cents = 0
    end

    def attach_payment_method!
      transition!(:requires_payment_method, :requires_capture)
      self
    end

    def capture!(ledger:, merchant_account:, platform_account:)
      transition!(:requires_capture, :succeeded)
      @captured_cents = amount_cents

      ledger.post(
        payment_id: id,
        currency: currency,
        kind: "capture",
        postings: [
          { account: merchant_account, amount_cents: amount_cents },
          { account: platform_account, amount_cents: -amount_cents }
        ]
      )
      self
    end

    def refund!(amount_cents:, ledger:, merchant_account:, platform_account:)
      raise InvalidTransition, "payment must be succeeded before refund" unless status == :succeeded

      amount = Integer(amount_cents)
      raise DomainError, "refund must be positive" unless amount.positive?
      raise DomainError, "refund exceeds captured amount" if refunded_cents + amount > captured_cents

      @refunded_cents += amount
      ledger.post(
        payment_id: id,
        currency: currency,
        kind: "refund",
        postings: [
          { account: merchant_account, amount_cents: -amount },
          { account: platform_account, amount_cents: amount }
        ]
      )
      self
    end

    private

    def transition!(from, to)
      raise InvalidTransition, "cannot transition #{status} -> #{to}" unless status == from

      @status = to
    end
  end

  class IdempotencyStore
    def initialize
      @records = {}
    end

    def fetch_or_record(key, fingerprint)
      existing = @records[key]
      if existing
        raise IdempotencyConflict, "idempotency key reused with different request" if existing[:fingerprint] != fingerprint

        return existing[:result]
      end

      result = yield
      @records[key] = { fingerprint: fingerprint, result: result }
      result
    end
  end

  class PaymentService
    attr_reader :ledger

    def initialize(ledger: Ledger.new, idempotency: IdempotencyStore.new)
      @ledger = ledger
      @idempotency = idempotency
      @payments = {}
    end

    def create_payment(amount_cents:, currency: "usd", idempotency_key:)
      fingerprint = "create:#{amount_cents}:#{currency}"
      @idempotency.fetch_or_record(idempotency_key, fingerprint) do
        payment = PaymentIntent.new(amount_cents: amount_cents, currency: currency)
        @payments[payment.id] = payment
        payment
      end
    end

    def capture(payment_id:, idempotency_key:)
      payment = fetch_payment(payment_id)
      fingerprint = "capture:#{payment_id}:#{payment.amount_cents}"
      @idempotency.fetch_or_record(idempotency_key, fingerprint) do
        payment.attach_payment_method! if payment.status == :requires_payment_method
        payment.capture!(
          ledger: ledger,
          merchant_account: "merchant:#{payment_id}",
          platform_account: "platform:cash"
        )
      end
    end

    def refund(payment_id:, amount_cents:, idempotency_key:)
      payment = fetch_payment(payment_id)
      fingerprint = "refund:#{payment_id}:#{amount_cents}"
      @idempotency.fetch_or_record(idempotency_key, fingerprint) do
        payment.refund!(
          amount_cents: amount_cents,
          ledger: ledger,
          merchant_account: "merchant:#{payment_id}",
          platform_account: "platform:cash"
        )
      end
    end

    def fetch_payment(payment_id)
      @payments.fetch(payment_id) { raise DomainError, "unknown payment #{payment_id}" }
    end
  end
end
