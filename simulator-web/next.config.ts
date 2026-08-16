import type {NextConfig} from "next";

const nextConfig: NextConfig = {
  output: "export",
  reactStrictMode: true,
  poweredByHeader: false,
  allowedDevOrigins: ["localhost", "127.0.0.1", "192.168.1.103"],
  images: {
    unoptimized: true,
  },
};

export default nextConfig;

