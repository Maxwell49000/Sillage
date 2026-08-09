import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import path from 'path';

export default defineConfig({
    plugins: [vue()],
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src'),
            'components': path.resolve(__dirname, './src/components'),
            'pages': path.resolve(__dirname, './src/pages'),
            'stores': path.resolve(__dirname, './src/stores'),
            'api': path.resolve(__dirname, './src/api'),
        },
    },
    server: {
        port: 8000,
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
                timeout: 320000, // 320 seconds - match axios timeout so proxy doesn't cut off long requests
                proxyTimeout: 320000, // internal proxy timeout for node http
                logLevel: 'info', // show proxy logs for debugging
            },
        },
        middlewareMode: false, // ensure proxy works in dev server mode
    },
    build: {
        outDir: 'dist',
        sourcemap: false,
    },
});
