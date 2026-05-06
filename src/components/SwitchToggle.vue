<template>
  <label class="switch">
    <input
      :checked="modelValue"
      type="checkbox"
      @change="emit('update:modelValue', $event.target.checked)"
    />
    <span class="switch-slider"></span>
  </label>
</template>

<script setup>
defineOptions({ name: 'SwitchToggle' })

defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue'])
</script>

<style scoped>
.switch {
  position: relative;
  width: 42px;
  height: 24px;
  display: inline-block;
}

.switch input {
  display: none;
}

.switch-slider {
  position: absolute;
  inset: 0;
  background: #d9d9d9;
  border-radius: 999px;
  transition: 0.2s ease;
  cursor: pointer;
}

.switch-slider::before {
  content: '';
  position: absolute;
  width: 18px;
  height: 18px;
  left: 3px;
  top: 3px;
  background: #fff;
  border-radius: 50%;
  transition: 0.2s ease;
}

.switch input:checked + .switch-slider {
  background: #2dc26b;
}

.switch input:checked + .switch-slider::before {
  transform: translateX(18px);
}
</style>