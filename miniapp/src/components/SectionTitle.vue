<template>
  <view class="section-heading">
    <view>
      <text class="section-heading__title">{{ title }}</text>
      <view v-if="subtitle" class="section-heading__subtitle">{{ subtitle }}</view>
    </view>
    <slot />
  </view>
</template>

<script setup lang="ts">
defineProps<{
  title: string;
  subtitle?: string;
}>();
</script>
