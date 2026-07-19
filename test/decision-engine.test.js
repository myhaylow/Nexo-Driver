import test from "node:test";
import assert from "node:assert/strict";
import { calculateMetrics, evaluateOffer } from "../cloud/decision-engine.js";
import { loadDecisionScenarios } from "../cloud/fixture-loader.js";

test("calcula distancia, tempo, valor por km e valor por hora", () => {
  const metrics = calculateMetrics({
    grossFare: 24,
    pickupDistanceKm: 2,
    tripDistanceKm: 10,
    pickupMinutes: 5,
    tripMinutes: 25,
    readingConfidence: 1
  });

  assert.deepEqual(metrics, {
    totalDistanceKm: 12,
    totalMinutes: 30,
    perKm: 2,
    perHour: 48
  });
});

test("todos os cenarios versionados produzem o resultado esperado", async (t) => {
  const fixture = await loadDecisionScenarios();

  for (const scenario of fixture.scenarios) {
    await t.test(scenario.id, () => {
      const result = evaluateOffer({
        offer: scenario.offer,
        profile: fixture.profiles[scenario.profileId],
        policy: fixture.policy
      });

      assert.equal(result.decision, scenario.expected.decision);
      assert.equal(result.reason, scenario.expected.reason);
    });
  }
});

test("rejeita entradas invalidas antes de recomendar uma corrida", () => {
  assert.throws(
    () => calculateMetrics({ grossFare: 20 }),
    /deve ser um numero finito/
  );
});
