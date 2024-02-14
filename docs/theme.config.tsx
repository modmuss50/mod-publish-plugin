import React from 'react'
import { DocsThemeConfig } from 'nextra-theme-docs'

const config: DocsThemeConfig = {
  logo: <span>Mod Publish Plugin</span>,
  project: {
    link: 'https://github.com/modmuss50/mod-publish-plugin',
  },
  chat: {
    link: 'https://discord.gg/teamreborn',
  },
  docsRepositoryBase: 'https://github.com/modmuss50/mod-publish-plugin/tree/main/docs',
  footer: {
    text: (
      <a href="https://nextra.site" target="_blank">
        Built with Nextra
      </a>
    ),
  },
  useNextSeoProps() {
    return {
      titleTemplate: '%s – Mod Publish Plugin'
    }
  },
  navigation: {
    prev: true,
    next: true,
  }
}

export default config
