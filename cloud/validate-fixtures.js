import { evaluateOffer } from "./decision-engine.js";
import { loadDecisionScenarios } from "./fixture-loader.js";

const fixture = await loadDecisionScenarios();
let failures = 0;

for (const scenario of fixture.scenarios) {
  const result = evaluateOffer({
    offer: scenario.offer,
    profile: fixture.profiles[scenario.profileId],
    policy: fixture.policy
  });

  if (result.decision !== scenario.expected.decision || result.reason !== scenario.expected.reason) {
    failures += 1;
    console.error(`[FALHA] ${scenario.id}:`, result, "esperado:", scenario.expected);
  }
}

if (failures > 0) {
  process.exitCode = 1;
} else {
  console.log(`${fixture.scenarios.length} cenarios validados com sucesso.`);
}
