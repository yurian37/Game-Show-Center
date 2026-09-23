/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}", // <--- Esto le dice que busque estilos dentro de tu App.jsx
  ],
  theme: {
    extend: {},
  },
  plugins: [],
}