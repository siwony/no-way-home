import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import { resolveBackendOrigin } from "./vite.backend";

export default defineConfig(async ({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const backend = await resolveBackendOrigin(env);
  const proxy = {
    "/api": {
      target: backend.origin,
      changeOrigin: true,
    },
  };

  if (backend.source !== "env") {
    const action = backend.source === "detected" ? "Detected" : "Defaulting to";
    console.info(`[vite] ${action} backend origin ${backend.origin} for /api proxy`);
  }

  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy,
    },
    preview: {
      proxy,
    },
    test: {
      environment: "node",
    },
  };
});
