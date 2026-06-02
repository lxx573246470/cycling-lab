/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        brand: {
          50: "#eef7ff",
          100: "#d9ecff",
          500: "#1d6fd8",
          600: "#1658b3",
          700: "#0f4288",
        },
      },
    },
  },
  plugins: [],
};
