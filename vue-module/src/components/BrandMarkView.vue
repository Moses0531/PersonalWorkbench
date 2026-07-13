<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 32 32"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden="true"
  >
    <rect width="32" height="32" rx="8" fill="currentColor" fill-opacity="0.12" />
    <path
      d="M8 22V10l8-4 8 4v12l-8 4-8-4z"
      stroke="currentColor"
      stroke-width="1.8"
      stroke-linejoin="round"
    />
    <path d="M16 6v20M8 10l8 4 8-4" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round" />
  </svg>
</template>

<script setup>
defineProps({
  size: {
    type: Number,
    default: 24,
  },
})
</script>
