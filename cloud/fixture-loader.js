import { readFile } from "node:fs/promises";

const fixtureUrl = new URL("../fixtures/decision-scenarios.json", import.meta.url);

export async function loadDecisionScenarios() {
  const content = await readFile(fixtureUrl, "utf8");
  const parsed = JSON.parse(content);
  if (!Array.isArray(parsed.scenarios) || parsed.scenarios.length === 0) {
    throw new Error("O arquivo de cenarios precisa conter uma lista nao vazia");
  }
  return parsed;
}
