import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { calculateMetrics } from "../cloud/decision-engine.js";
import { normalizeUberCardSample, validateUberCardSample } from "../cloud/uber-card-fixture.js";

const fixtureUrl = new URL("../fixtures/uber-card-samples.json", import.meta.url);
const fixture = JSON.parse(await readFile(fixtureUrl, "utf8"));

test("valida as variantes anonimizadas observadas nos cards", () => {
  assert.equal(fixture.samples.length, 10);
  for (const sample of fixture.samples) {
    assert.equal(validateUberCardSample(sample), true);
  }
});

test("nao envia corrida ja encontrada ao motor de decisao", () => {
  const normalized = fixture.samples.map((sample) => normalizeUberCardSample(sample));
  assert.equal(normalized.filter(Boolean).length, 9);
  assert.equal(normalized.filter((offer) => offer === null).length, 1);
});

test("toda oferta observada gera metricas finitas", () => {
  for (const sample of fixture.samples) {
    const offer = normalizeUberCardSample(sample);
    if (!offer) continue;

    const metrics = calculateMetrics(offer);
    assert.equal(Number.isFinite(metrics.perKm), true);
    assert.equal(Number.isFinite(metrics.perHour), true);
  }
});

test("propaga o sinal de direcao ao destino", () => {
  const sample = fixture.samples.find((item) => item.badges.includes("TOWARD_DESTINATION"));
  const offer = normalizeUberCardSample(sample);
  assert.equal(offer.destination.matches, true);
});
