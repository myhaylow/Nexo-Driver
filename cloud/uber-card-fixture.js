const LIFECYCLES = new Set(["OFFER", "MATCHED"]);
const PRESENTATIONS = new Set(["RADAR", "DIRECT_EXCLUSIVE", "MATCHED_RESULT"]);
const CTAS = new Set(["SELECT", "ACCEPT", "START_NAVIGATION"]);

function requireFinite(value, label) {
  if (!Number.isFinite(value)) {
    throw new TypeError(`${label} deve ser um numero finito`);
  }
}

function round(value, places = 2) {
  const factor = 10 ** places;
  return Math.round((value + Number.EPSILON) * factor) / factor;
}

export function validateUberCardSample(sample) {
  if (!sample?.id || typeof sample.id !== "string") {
    throw new TypeError("sample.id deve ser uma string nao vazia");
  }
  if (!LIFECYCLES.has(sample.lifecycle)) {
    throw new TypeError(`${sample.id}.lifecycle invalido`);
  }
  if (!PRESENTATIONS.has(sample.presentation)) {
    throw new TypeError(`${sample.id}.presentation invalido`);
  }
  if (!CTAS.has(sample.cta)) {
    throw new TypeError(`${sample.id}.cta invalido`);
  }

  requireFinite(sample.grossFare, `${sample.id}.grossFare`);
  requireFinite(sample.pickup?.minutes, `${sample.id}.pickup.minutes`);
  requireFinite(sample.pickup?.distanceKm, `${sample.id}.pickup.distanceKm`);
  requireFinite(sample.trip?.minutes, `${sample.id}.trip.minutes`);
  requireFinite(sample.trip?.distanceKm, `${sample.id}.trip.distanceKm`);

  if (sample.lifecycle === "OFFER" && sample.cta === "START_NAVIGATION") {
    throw new TypeError(`${sample.id}: oferta nao pode iniciar navegacao`);
  }
  if (sample.lifecycle === "MATCHED" && sample.cta !== "START_NAVIGATION") {
    throw new TypeError(`${sample.id}: corrida encontrada deve iniciar navegacao`);
  }

  const totalDistanceKm = sample.pickup.distanceKm + sample.trip.distanceKm;
  if (sample.displayedPerKm !== undefined) {
    requireFinite(sample.displayedPerKm, `${sample.id}.displayedPerKm`);
    const calculated = round(sample.grossFare / totalDistanceKm);
    if (calculated !== sample.displayedPerKm) {
      throw new RangeError(
        `${sample.id}.displayedPerKm divergente: exibido ${sample.displayedPerKm}, calculado ${calculated}`
      );
    }
  }

  return true;
}

export function normalizeUberCardSample(sample, readingConfidence = 1) {
  validateUberCardSample(sample);
  if (sample.lifecycle !== "OFFER") {
    return null;
  }

  const destinationMatch = sample.badges?.includes("TOWARD_DESTINATION") ?? false;
  return {
    grossFare: sample.grossFare,
    pickupDistanceKm: sample.pickup.distanceKm,
    tripDistanceKm: sample.trip.distanceKm,
    pickupMinutes: sample.pickup.minutes,
    tripMinutes: sample.trip.minutes,
    readingConfidence,
    ...(destinationMatch ? { destination: { matches: true } } : {})
  };
}
