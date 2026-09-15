// Dev default — overwritten at container startup in production (see
// docker-entrypoint.sh), so the same built image works against any backend
// URL without rebuilding. Loaded before the Angular bundle (see index.html).
window.__env = { apiUrl: "http://localhost:8081" };
