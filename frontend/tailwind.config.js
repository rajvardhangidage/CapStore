/** @type {import('tailwindcss').Config} */
// Tokens follow 52degree.in-design.md: one electric-blue accent, charcoal text, white surfaces, cool-grey borders.
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
        ink: { DEFAULT: '#282828', soft: '#4b5563', faint: '#6b7280' },
        // Legacy aliases kept so existing class names stay on-palette.
        twill: { DEFAULT: '#f5f6f8', dark: '#e5e7eb' },
        thread: { DEFAULT: '#1f38d3', dark: '#1230c9', light: '#eef2ff' },
        brim: { DEFAULT: '#d32f2f', light: '#fdecec' },
        moss: { DEFAULT: '#1f7a4d', light: '#e6f4ec' },
        warn: { DEFAULT: '#a15c07', light: '#fdf3e1' },
      },
      fontFamily: {
        display: ['"Clash Display"', '"ClashDisplay"', 'system-ui', 'sans-serif'],
        sans: ['"Clash Display"', '"ClashDisplay"', 'system-ui', 'sans-serif'],
      },
      borderRadius: { sm: '4px', md: '8px', lg: '12px', xl: '16px' },
      keyframes: {
        marquee: { from: { transform: 'translateX(0)' }, to: { transform: 'translateX(-50%)' } },
        shimmer: { '100%': { transform: 'translateX(100%)' } },
        'fade-up': { from: { opacity: 0, transform: 'translateY(8px)' }, to: { opacity: 1, transform: 'none' } },
      },
      animation: {
        marquee: 'marquee 28s linear infinite',
        'fade-up': 'fade-up .35s ease-out both',
      },
    },
  },
  plugins: [],
}
