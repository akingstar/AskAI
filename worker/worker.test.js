import test from "node:test";
import assert from "node:assert/strict";
import { validatePlan } from "./worker.js";

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
