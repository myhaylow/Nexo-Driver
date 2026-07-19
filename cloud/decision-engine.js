export const Decision = Object.freeze({
  ACCEPT: "ACCEPT",
  ANALYZE: "ANALYZE",
  REJECT: "REJECT"
});

export const Reason = Object.freeze({
  LOW_READING_CONFIDENCE: "LOW_READING_CONFIDENCE",
  SUPERMARKET_BLOCKED: "SUPERMARKET_BLOCKED",
  GEOGRAPHIC_BLOCKED: "GEOGRAPHIC_BLOCKED",
  KM_BELOW_MINIMUM: "KM_BELOW_MINIMUM",
  PICKUP_ABOVE_LIMIT: "PICKUP_ABOVE_LIMIT",
  PICKUP_TOLERANCE_APPLIED: "PICKUP_TOLERANCE_APPLIED",
  KM_AND_HOUR_APPROVED: "KM_AND_HOUR_APPROVED",
  KM_COMPENSATED_HOUR: "KM_COMPENSATED_HOUR",
  HOUR_BELOW_TARGET: "HOUR_BELOW_TARGET"
});

const REQUIRED_PROFILE_FIELDS = [
  "minPerKm",
  "targetPerHour",
  "maxPickupDistanceKm",
  "maxPickupMinutes"
];

const REQUIRED_OFFER_FIELDS = [
  "grossFare",
  "pickupDistanceKm",
  "tripDistanceKm",
  "pickupMinutes",
  "tripMinutes",
  "readingConfidence"
];

function requireFiniteNumbers(value, fields, label) {
  for (const field of fields) {
    if (!Number.isFinite(value?.[field])) {
      throw new TypeError(`${label}.${field} deve ser um numero finito`);
    }
  }
}

function round(value, places = 2) {
  const factor = 10 ** places;
  return Math.round((value + Number.EPSILON) * factor) / factor;
}

export function calculateMetrics(offer) {
  requireFiniteNumbers(offer, REQUIRED_OFFER_FIELDS, "offer");

  const totalDistanceKm = offer.pickupDistanceKm + offer.tripDistanceKm;
  const totalMinutes = offer.pickupMinutes + offer.tripMinutes;

  if (offer.grossFare < 0 || totalDistanceKm <= 0 || totalMinutes <= 0) {
    throw new RangeError("Oferta deve possuir valor nao negativo, distancia e tempo totais positivos");
  }

  return Object.freeze({
    totalDistanceKm: round(totalDistanceKm),
    totalMinutes: round(totalMinutes),
    perKm: round(offer.grossFare / totalDistanceKm),
    perHour: round((offer.grossFare / totalMinutes) * 60)
  });
}

/**
 * Avalia uma oferta usando apenas regras deterministicas e dados normalizados.
 * O chamador escolhe o perfil normal ou o perfil proprio do Modo Destino.
 */
export function evaluateOffer({ offer, profile, policy = {} }) {
  requireFiniteNumbers(profile, REQUIRED_PROFILE_FIELDS, "profile");
  const metrics = calculateMetrics(offer);

  const readingConfidenceMinimum = policy.readingConfidenceMinimum ?? 0.95;
  const pickupTolerance = policy.pickupTolerance ?? 0.10;
  const strongKmBonus = policy.strongKmBonus ?? 0.10;
  const hourTolerance = policy.hourTolerance ?? 0.10;
  const trafficContext = offer.trafficContext ?? "NORMAL";
  const destination = Boolean(offer.destination?.matches);

  const result = (decision, reason, details = {}) => Object.freeze({
    decision,
    reason,
    destination,
    metrics,
    profileId: profile.id,
    details: Object.freeze(details)
  });

  if (offer.readingConfidence < readingConfidenceMinimum) {
    return result(Decision.ANALYZE, Reason.LOW_READING_CONFIDENCE, {
      actual: offer.readingConfidence,
      required: readingConfidenceMinimum
    });
  }

  if (offer.supermarketBlocked) {
    return result(Decision.REJECT, Reason.SUPERMARKET_BLOCKED);
  }

  if (offer.geographicBlocked) {
    return result(Decision.REJECT, Reason.GEOGRAPHIC_BLOCKED);
  }

  if (metrics.perKm < profile.minPerKm) {
    return result(Decision.REJECT, Reason.KM_BELOW_MINIMUM, {
      actual: metrics.perKm,
      required: profile.minPerKm
    });
  }

  const pickupDistanceWithTolerance = profile.maxPickupDistanceKm * (1 + pickupTolerance);
  const pickupMinutesWithTolerance = profile.maxPickupMinutes * (1 + pickupTolerance);
  const pickupStrict =
    offer.pickupDistanceKm <= profile.maxPickupDistanceKm &&
    offer.pickupMinutes <= profile.maxPickupMinutes;
  const pickupTolerated =
    offer.pickupDistanceKm <= pickupDistanceWithTolerance &&
    offer.pickupMinutes <= pickupMinutesWithTolerance;
  const hourApproved = metrics.perHour >= profile.targetPerHour;

  if (!pickupTolerated) {
    return result(Decision.REJECT, Reason.PICKUP_ABOVE_LIMIT, {
      maxDistanceKm: round(pickupDistanceWithTolerance),
      maxMinutes: round(pickupMinutesWithTolerance)
    });
  }

  if (!pickupStrict) {
    if (hourApproved) {
      return result(Decision.ACCEPT, Reason.PICKUP_TOLERANCE_APPLIED, {
        tolerance: pickupTolerance
      });
    }
    return result(Decision.REJECT, Reason.PICKUP_ABOVE_LIMIT, {
      toleranceAvailable: true,
      hourApproved: false
    });
  }

  if (hourApproved) {
    return result(Decision.ACCEPT, Reason.KM_AND_HOUR_APPROVED);
  }

  const strongKm = metrics.perKm >= profile.minPerKm * (1 + strongKmBonus);
  const hourInsideTolerance = metrics.perHour >= profile.targetPerHour * (1 - hourTolerance);
  const compensationAllowed = trafficContext !== "INTENSE";

  if (strongKm && hourInsideTolerance && compensationAllowed) {
    return result(Decision.ACCEPT, Reason.KM_COMPENSATED_HOUR, {
      strongKmBonus,
      hourTolerance,
      trafficContext
    });
  }

  return result(Decision.ANALYZE, Reason.HOUR_BELOW_TARGET, {
    actual: metrics.perHour,
    target: profile.targetPerHour,
    trafficContext
  });
}
