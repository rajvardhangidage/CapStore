/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        primary: { DEFAULT: '#1f38d3', strong: '#1230c9', soft: '#eef2ff' },
        secondary: '#282828',
        tertiary: '#e5e7eb',
        neutral: '#ffffff',
        surface: '#ffffff',
        'on-surface': '#282828',
        error: '#d32f2f',
        ink: { DEFAULT: '#282828', soft: '#374151', faint: '#4b5563' },
        twill: { DEFAULT: '#eef2ff', dark: '#dfe7ff' },
        thread: { DEFAULT: '#1f38d3', dark: '#1230c9', light: '#eef2ff' },
        brim: { DEFAULT: '#d32f2f', light: '#fbe8e8' },
        moss: { DEFAULT: '#2f7d5b', light: '#e3f5eb' },
      },
      fontFamily: {
        display: ['"Clash Display"', '"ClashDisplay"', 'system-ui', 'sans-serif'],
        sans: ['"Clash Display"', '"ClashDisplay"', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
