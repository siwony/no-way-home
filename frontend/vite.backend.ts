const DEFAULT_BACKEND_ORIGIN = "http://localhost:8080";
const DEFAULT_BACKEND_CANDIDATES = [
  "http://localhost:8080",
  "http://127.0.0.1:8080",
  "http://localhost:8081",
  "http://127.0.0.1:8081",
];

type FetchLike = typeof fetch;

export type ResolvedBackendOrigin = {
  origin: string;
  source: "env" | "detected" | "default";
};

export function parseBackendCandidates(rawValue: string | undefined): string[] {
  if (!rawValue) {
    return [];
  }

  return Array.from(new Set(rawValue.split(",").map((value) => value.trim()).filter(Boolean)));
}

export async function isNoWayHomeBackend(origin: string, fetchImpl: FetchLike = fetch): Promise<boolean> {
  try {
    const response = await fetchImpl(new URL("/api/status", origin), {
      headers: {
        accept: "application/json",
      },
      signal: AbortSignal.timeout(1000),
    });

    if (!response.ok) {
      return false;
    }

    const payload = (await response.json()) as { service?: unknown };
    return payload.service === "no-way-home";
  } catch {
    return false;
  }
}

export async function resolveBackendOrigin(
  env: Record<string, string | undefined>,
  fetchImpl: FetchLike = fetch,
): Promise<ResolvedBackendOrigin> {
  const configuredOrigin = env.VITE_BACKEND_ORIGIN?.trim();
  if (configuredOrigin) {
    return {
      origin: configuredOrigin,
      source: "env",
    };
  }

  const configuredCandidates = parseBackendCandidates(env.VITE_BACKEND_CANDIDATES);
  const candidates = configuredCandidates.length > 0 ? configuredCandidates : DEFAULT_BACKEND_CANDIDATES;

  for (const candidate of candidates) {
    if (await isNoWayHomeBackend(candidate, fetchImpl)) {
      return {
        origin: candidate,
        source: "detected",
      };
    }
  }

  return {
    origin: DEFAULT_BACKEND_ORIGIN,
    source: "default",
  };
}
