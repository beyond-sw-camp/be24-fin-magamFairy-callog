/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  darkMode: 'class', // Callog 설정 페이지 다크모드를 위해 필수
  theme: {
    extend: {},
  },
  plugins: [],
}