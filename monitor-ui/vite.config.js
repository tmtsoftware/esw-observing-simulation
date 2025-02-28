import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import { AppConfig } from './src/config/AppConfig.js'

const testDeps = []
export default defineConfig({
  server: {
    host: true,
    port: 9000
  },
  base: `./`,
  optimizeDeps: {
    include: testDeps,
  },
  css: {
    preprocessorOptions: {
      less: {
        javascriptEnabled: true,
        additionalData: '@root-entry-name: default;',
      },
    },
  },
  build: {
    outDir: AppConfig.applicationName,
    // sourcemap: 'inline',
    sourcemap: true,
    minify: false,
    rollupOptions: {
      input: ['./index.html'],
      onLog(level, log, handler) {
        if (log.cause && log.cause.message === `Can't resolve original location of error.`) {
          return
        }
        handler(level, log)
      }
    }
  },
  plugins: [react()]
})
