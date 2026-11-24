import test from "node:test";
import assert from "node:assert/strict";
import worker, { validatePlan, serializeValidPlan } from "./worker.js";

const step = (extra = {}) => ({
  action: "torch", target: "", value: "", ...extra
});
const plan = (steps = [step()]) => ({ language: "hi-IN", steps });
const battery = {
  type: "BATTERY_PERCENT",
  operator: "LESS_THAN",
  value: 20,
  upperValue: 0
};

test("accepts legacy plan without conditions", () => {
  assert.equal(validatePlan(plan()), true);
});

test("accepts valid battery condition", () => {
  assert.equal(validatePlan(plan([step({ condition: battery })])), true);
});

test("accepts overnight time condition", () => {
  assert.equal(validatePlan(plan([step({ condition: {
    type: "TIME_OF_DAY", operator: "BETWEEN",
    value: 1320, upperValue: 360
  }})])), true);
});

test("accepts previous-step condition after first step", () => {
  assert.equal(validatePlan(plan([
    step(),
    step({ condition: {
      type: "PREVIOUS_STEP", operator: "SUCCEEDED",
      value: 0, upperValue: 0
    }})
  ])), true);
});

test("rejects previous-step condition on first step", () => {
  assert.equal(validatePlan(plan([step({ condition: {
    type: "PREVIOUS_STEP", operator: "SUCCEEDED",
    value: 0, upperValue: 0
  }})])), false);
});

test("rejects unknown actions and extra fields", () => {
  assert.equal(validatePlan(plan([step({ action: "unknown" })])), false);
  assert.equal(validatePlan({ ...plan(), extra: true }), false);
});

test("rejects invalid condition operator and bounds", () => {
  assert.equal(validatePlan(plan([step({ condition: {
    ...battery, operator: "BETWEEN"
  }})])), false);
  assert.equal(validatePlan(plan([step({ condition: {
    ...battery, value: 101
  }})])), false);
});

test("rejects fractional condition values and extra condition keys", () => {
  assert.equal(validatePlan(plan([step({ condition: {
    ...battery, value: 20.5
  }})])), false);
  assert.equal(validatePlan(plan([step({ condition: {
    ...battery, extra: true
  }})])), false);
});

test("rejects malformed step types and oversized strings", () => {
  assert.equal(validatePlan(plan([step({ target: 42 })])), false);
  assert.equal(validatePlan(plan([step({ value: "x".repeat(501) })])), false);
});

test("rejects empty or oversized step arrays", () => {
  assert.equal(validatePlan(plan([])), false);
  assert.equal(validatePlan(plan(Array.from({ length: 6 }, () => step()))), false);
});


test("serializes a valid provider plan", () => {
  const result = serializeValidPlan(JSON.stringify(plan()));
  assert.equal(result, JSON.stringify(plan()));
});

test("rejects malformed provider JSON", () => {
  assert.equal(serializeValidPlan("{invalid"), null);
});

test("rejects schema-invalid provider response", () => {
  assert.equal(
    serializeValidPlan(JSON.stringify(plan([step({ action: "unknown" })]))),
    null
  );
});


test("Gemini invalid response retries Groq and returns valid plan", async () => {
  const originalFetch = globalThis.fetch;
  const calls = [];

  globalThis.fetch = async (url) => {
    const address = String(url);
    calls.push(address);

    if (address.includes("generativelanguage.googleapis.com")) {
      return new Response(JSON.stringify({
        candidates: [{ content: { parts: [{ text: "{bad json" }] } }]
      }), { status: 200 });
    }

    if (address.includes("api.groq.com")) {
      return new Response(JSON.stringify({
        choices: [{ message: { content: JSON.stringify(plan()) } }]
      }), { status: 200 });
    }

    throw new Error("Unexpected provider URL");
  };

  try {
    const env = {
      GEMINI_API_KEY: "test-key",
      GROQ_API_KEY: "test-key",
      RATE_LIMIT: {
        get: async () => null,
        put: async () => {}
      }
    };

    const request = new Request("https://worker.test/", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ text: "torch on karo" })
    });

    const result = await worker.fetch(request, env);
    const body = await result.json();

    assert.equal(result.status, 200);
    assert.equal(body.result, JSON.stringify(plan()));
    assert.equal(calls.length, 2);
    assert.match(calls[0], /generativelanguage/);
    assert.match(calls[1], /api.groq.com/);
  } finally {
    globalThis.fetch = originalFetch;
  }
});
