import { evaluateOffer } from "./decision-engine.js";
import { loadDecisionScenarios } from "./fixture-loader.js";

const labels = {
  ACCEPT: "ACEITAR",
  ANALYZE: "ANALISAR",
  REJECT: "RECUSAR"
};

const fixture = await loadDecisionScenarios();

for (const scenario of fixture.scenarios) {
  const result = evaluateOffer({
    offer: scenario.offer,
    profile: fixture.profiles[scenario.profileId],
    policy: fixture.policy
  });

  console.log(
    `${scenario.id.padEnd(34)} ${labels[result.decision].padEnd(9)}` +
    ` R$ ${result.metrics.perKm.toFixed(2)}/km | R$ ${result.metrics.perHour.toFixed(2)}/h` +
    `${result.destination ? " | DESTINO" : ""}`
  );
}
