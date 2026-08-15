import type {NextConfig} from "next";

const nextConfig: NextConfig = {
  output: "export",
  reactStrictMode: true,
  poweredByHeader: false,
  allowedDevOrigins: ["192.168.1.8"],
  images: {
    unoptimized: true,
  },
};

export default nextConfig;

