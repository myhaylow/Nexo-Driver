import { readFile } from "node:fs/promises";
import { normalizeUberCardSample, validateUberCardSample } from "./uber-card-fixture.js";

const url = new URL("../fixtures/uber-card-samples.json", import.meta.url);
const fixture = JSON.parse(await readFile(url, "utf8"));

let offers = 0;
let matched = 0;

for (const sample of fixture.samples) {
  validateUberCardSample(sample);
  if (normalizeUberCardSample(sample)) {
    offers += 1;
  } else {
    matched += 1;
  }
}

console.log(
  `${fixture.samples.length} cards anonimizados validados: ${offers} ofertas e ${matched} estado pos-selecao.`
);
