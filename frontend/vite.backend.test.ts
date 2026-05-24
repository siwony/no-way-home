import { describe, expect, it, vi } from "vitest";
import { isNoWayHomeBackend, parseBackendCandidates, resolveBackendOrigin } from "./vite.backend";

describe("parseBackendCandidates", () => {
  it("trims, deduplicates, and ignores empty values", () => {
    expect(parseBackendCandidates(" http://localhost:8080, ,http://localhost:8081,http://localhost:8080 "))
      .toEqual(["http://localhost:8080", "http://localhost:8081"]);
  });
});

describe("isNoWayHomeBackend", () => {
  it("accepts the expected app status payload", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ service: "no-way-home" }),
    });

    await expect(isNoWayHomeBackend("http://localhost:8081", fetchMock as typeof fetch)).resolves.toBe(true);
  });

  it("rejects non-matching services and failed responses", async () => {
    const wrongServiceFetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ service: "other-app" }),
    });
    const notFoundFetch = vi.fn().mockResolvedValue({
      ok: false,
      json: async () => ({}),
    });

    await expect(isNoWayHomeBackend("http://localhost:8080", wrongServiceFetch as typeof fetch)).resolves.toBe(false);
    await expect(isNoWayHomeBackend("http://localhost:8080", notFoundFetch as typeof fetch)).resolves.toBe(false);
  });
});

describe("resolveBackendOrigin", () => {
  it("prefers an explicit backend origin override", async () => {
    const fetchMock = vi.fn();

    await expect(
      resolveBackendOrigin({ VITE_BACKEND_ORIGIN: "http://127.0.0.1:9090" }, fetchMock as typeof fetch),
    ).resolves.toEqual({
      origin: "http://127.0.0.1:9090",
      source: "env",
    });

    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("detects a healthy backend from the candidate list", async () => {
    const fetchMock = vi.fn(async (input: URL | RequestInfo) => {
      const url = typeof input === "string" ? input : input instanceof URL ? input.href : input.url;
      const origin = new URL(url).origin;

      return {
        ok: origin === "http://127.0.0.1:8081",
        json: async () => ({ service: origin === "http://127.0.0.1:8081" ? "no-way-home" : "other-app" }),
      };
    });

    await expect(
      resolveBackendOrigin(
        { VITE_BACKEND_CANDIDATES: "http://127.0.0.1:8080,http://127.0.0.1:8081" },
        fetchMock as typeof fetch,
      ),
    ).resolves.toEqual({
      origin: "http://127.0.0.1:8081",
      source: "detected",
    });
  });
});
