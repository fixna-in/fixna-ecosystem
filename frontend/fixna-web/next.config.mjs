import path from "node:path";
import { fileURLToPath } from "node:url";

const rootDirectory = path.join(path.dirname(fileURLToPath(import.meta.url)), "..", "..");

/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  output: "standalone",
  outputFileTracingRoot: rootDirectory,
};

export default nextConfig;
