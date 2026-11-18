import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        configure: (proxy) => {
          proxy.on("proxyReq", (proxyReq, request) => {
            if (request.headers.cookie) {
              proxyReq.setHeader("cookie", request.headers.cookie);
            }
          });
        }
      }
    }
  }
});
