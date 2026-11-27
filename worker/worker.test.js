import test from "node:test";
import assert from "node:assert/strict";
import worker, { validatePlan, serializeValidPlan, validateConversationContext, validateQaContext, containsPrivateData, validateOutboundPayload } from "./worker.js";

const step = (extra = {}) => ({ action: "torch", target: "", value: "", ...extra });
const plan = (steps = [step()]) => ({ language: "hi-IN", steps });
const battery = { type: "BATTERY_PERCENT", operator: "LESS_THAN", value: 20, upperValue: 0 };

test("accepts legacy plan and valid routine conditions", () => {
  assert.equal(validatePlan(plan()), true);
  assert.equal(validatePlan(plan([step({ condition: battery })])), true);
  assert.equal(validatePlan(plan([step(), step({ condition: { type: "PREVIOUS_STEP", operator: "SUCCEEDED", value: 0, upperValue: 0 } })])), true);
});
test("rejects malformed, oversized and invalid plans", () => {
  assert.equal(validatePlan({ ...plan(), extra: true }), false);
  assert.equal(validatePlan(plan([step({ action: "unknown" })])), false);
  assert.equal(validatePlan(plan([step({ target: 42 })])), false);
  assert.equal(validatePlan(plan(Array.from({ length: 6 }, () => step()))), false);
  assert.equal(serializeValidPlan("{bad"), null);
});
test("accepts standalone answers but rejects mixed answer plans", () => {
  assert.equal(validatePlan(plan([step({ action: "answer", value: "नमस्ते" })])), true);
  assert.equal(validatePlan(plan([step(), step({ action: "answer", value: "hello" })])), false);
});
test("validates bounded safe conversation context", () => {
  assert.equal(validateConversationContext([]), true);
  assert.equal(validateConversationContext([{ command: "torch on karo", outcome: "success=1" }]), true);
  assert.equal(validateQaContext([{ question: "भारत की राजधानी?", answer: "नई दिल्ली" }]), true);
  assert.equal(validateConversationContext([{ command: "x".repeat(241), outcome: "ok" }]), false);
});
test("privacy detector catches credentials and personal identifiers", () => {
  for (const secret of ["password is hunter2", "OTP 123456", "पिन: 1234", "CVV 123", "पासवर्ड: secret123", "test@example.com", "UPI name@okaxis", "9876543210", "4111111111111111"]) assert.equal(containsPrivateData(secret), true, secret);
  assert.equal(containsPrivateData("भारत की राजधानी क्या है?"), false);
});
test("outbound payload rejects private user input and context but allows benign prompts", () => {
  assert.equal(validateOutboundPayload("भारत की राजधानी क्या है?", [], []), true);
  assert.equal(validateOutboundPayload("OTP 123456", [], []), false);
  assert.equal(validateOutboundPayload("What was that?", [{ command: "password is hunter2", outcome: "ok" }], []), false);
  assert.equal(validateOutboundPayload("What was that?", [], [{ question: "safe?", answer: "UPI name@okaxis" }]), false);
});
test("Worker refuses sensitive requests before invoking any provider", async () => {
  const originalFetch = globalThis.fetch; let providerCalls = 0;
  globalThis.fetch = async () => { providerCalls++; throw new Error("must not call provider"); };
  try {
    const request = new Request("https://worker.test/", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ text: "OTP 123456" }) });
    const result = await worker.fetch(request, { GEMINI_API_KEY: "test", GROQ_API_KEY: "test", RATE_LIMIT: { get: async () => null, put: async () => {} } });
    assert.equal(result.status, 400); assert.equal(providerCalls, 0);
  } finally { globalThis.fetch = originalFetch; }
});
test("Worker still routes safe question to configured provider", async () => {
  const originalFetch = globalThis.fetch; let providerCalls = 0;
  globalThis.fetch = async (_url, init) => {
    providerCalls++;
    const body = JSON.parse(init.body);
    assert.match(JSON.stringify(body), /भारत की राजधानी/);
    return new Response(JSON.stringify({ candidates: [{ content: { parts: [{ text: JSON.stringify(plan()) }] } }] }), { status: 200 });
  };
  try {
    const request = new Request("https://worker.test/", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ text: "भारत की राजधानी क्या है?" }) });
    const result = await worker.fetch(request, { GEMINI_API_KEY: "test", RATE_LIMIT: { get: async () => null, put: async () => {} } });
    assert.equal(result.status, 200); assert.equal(providerCalls, 1);
  } finally { globalThis.fetch = originalFetch; }
});


test("Worker provider fallback uses current models", async () => {
  const originalFetch = globalThis.fetch;
  const calls = [];
  globalThis.fetch = async (url, init) => {
    calls.push({ url: String(url), body: JSON.parse(init.body) });
    if (String(url).includes("generativelanguage.googleapis.com")) {
      return new Response("unavailable", { status: 503 });
    }
    return new Response(JSON.stringify({
      choices: [{ message: { content: JSON.stringify(plan()) } }]
    }), { status: 200 });
  };
  try {
    const request = new Request("https://worker.test/", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ text: "भारत की राजधानी क्या है?" })
    });
    const result = await worker.fetch(request, {
      GEMINI_API_KEY: "test",
      GROQ_API_KEY: "test",
      RATE_LIMIT: { get: async () => null, put: async () => {} }
    });
    assert.equal(result.status, 200);
    assert.equal(calls.length, 2);
    assert.match(calls[0].url, /gemini-3\.8-flash/);
    assert.equal(calls[1].body.model, "openai/gpt-oss-20b");
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("Worker returns 503 when both providers are unavailable", async () => {
  const originalFetch = globalThis.fetch;
  let calls = 0;
  globalThis.fetch = async () => {
    calls++;
    return new Response("unavailable", { status: 503 });
  };
  try {
    const request = new Request("https://worker.test/", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ text: "भारत की राजधानी क्या है?" })
    });
    const result = await worker.fetch(request, {
      GEMINI_API_KEY: "test",
      GROQ_API_KEY: "test",
      RATE_LIMIT: { get: async () => null, put: async () => {} }
    });
    assert.equal(result.status, 503);
    assert.equal(calls, 2);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("Worker accepts 4000 characters and rejects 4001", () => {
  assert.equal(validateOutboundPayload("a".repeat(4000), [], []), true);
  assert.equal(validateOutboundPayload("a".repeat(4001), [], []), false);
});
