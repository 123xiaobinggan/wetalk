<template>
  <div
    class="editable-info-item"
    @mouseenter="hover = true"
    @mouseleave="hover = false"
    @click="startEdit"
  >
    <div class="editable-title">{{ label }}</div>

    <template v-if="editing">
      <input
        ref="inputRef"
        v-model="localValue"
        class="editable-input"
        @click.stop
        @keydown.enter="save"
        @blur="save"
      />
    </template>

    <template v-else>
      <div class="editable-value-row">
        <span class="editable-value">{{ localValue || placeholder }}</span>
        <span v-show="!readonly && hover" class="editable-pen">✎</span>
      </div>
    </template>
  </div>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue'

defineOptions({ name: 'EditableInfoItem' })

const props = defineProps({
  label: {
    type: String,
    required: true,
  },
  value: {
    type: String,
    default: '',
  },
  placeholder: {
    type: String,
    default: '未设置',
  },
  readonly: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['save'])

const hover = ref(false)
const editing = ref(false)
const localValue = ref(props.value)
const inputRef = ref(null)

watch(
  () => props.value,
  (val) => {
    localValue.value = val || ''
  },
)

async function startEdit() {
  if(props.readonly) return
  editing.value = true
  await nextTick()
  inputRef.value?.focus()
}

function save() {
  editing.value = false
  if(localValue.value === props.value) return
  emit('save', localValue.value)
}
</script>

<style scoped>
.editable-info-item {
  padding: 14px 16px;
  cursor: pointer;
}

.editable-info-item:hover {
  background: #fafafa;
}

.editable-title {
  font-size: 14px;
  font-weight: 600;
  color: #111;
  margin-bottom: 7px;
}

.editable-value-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.editable-value {
  font-size: 13px;
  color: #8a8a8a;
  line-height: 1.5;
  word-break: break-all;
}

.editable-pen {
  flex: none;
  color: #999;
  font-size: 14px;
}

.editable-input {
  width: 100%;
  height: 34px;
  border: 1px solid #d9d9d9;
  outline: none;
  border-radius: 8px;
  padding: 0 10px;
  font-size: 13px;
  color: #333;
  box-sizing: border-box;
  background: #fff;
}

.editable-input:focus {
  border-color: #1890ff;
}
</style>