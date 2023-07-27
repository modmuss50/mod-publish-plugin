const withNextra = require("nextra")({
  theme: "nextra-theme-docs",
  themeConfig: "./theme.config.tsx",
})

/**
 * @type {import('next').NextConfig}
 */
const nextConfig = {
  output: "export",
  distDir: "build",
  trailingSlash: true,
  rewrites: undefined,
  images: {
    unoptimized: true,
  },
}

module.exports = {
  ...withNextra(),
  ...nextConfig,
}