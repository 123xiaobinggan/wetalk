<template>
  <div class="mask" @click="close" @wheel.prevent="onWheel" tabindex="0" ref="maskRef">
    <div class="toolbar" @click.stop>
      <button @click="zoomIn">+</button>
      <button @click="zoomOut">-</button>
      <button @click="rotateLeft">左转</button>
      <button @click="rotateRight">右转</button>
      <button @click="flipX">翻转</button>
      <button @click="copyImage">复制</button>
      <button @click="downloadImage">下载</button>
      <button @click="reset">重置</button>
      <button @click="close">关闭</button>
    </div>

    <div class="stage" @click.stop>
      <img
        :src="store.src"
        :alt="store.alt"
        class="img"
        :style="imgStyle"
        @mousedown.prevent="startDrag"
        @mousemove.prevent="onDrag"
        @mouseup="endDrag"
        @mouseleave="endDrag"
        draggable="false"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import { useImagePreviewStore } from '@/stores/imagePreviewStore'
defineOptions({
  name: 'ImagePreview',
})
const store = useImagePreviewStore()
const maskRef = ref(null)

const imgStyle = computed(() => {
  const sx = store.flipX ? -1 : 1
  const sy = store.flipY ? -1 : 1
  return {
    transform: `translate(${store.offset.x}px, ${store.offset.y}px) rotate(${
      store.rotate
    }deg) scale(${store.scale * sx}, ${store.scale * sy})`,
    cursor: store.dragging ? 'grabbing' : 'grab',
  }
})

const close = () => store.close()
const reset = () => store.resetView()
const zoomIn = () => store.zoom(0.2)
const zoomOut = () => store.zoom(-0.2)

const rotateLeft = () => store.rotateLeft()
const rotateRight = () => store.rotateRight()
const flipX = () => store.toggleFlipX()

const onWheel = (e) => {
  // e.deltaY > 0 向下滚动 => 缩小
  const delta = e.deltaY > 0 ? -0.15 : 0.15
  store.zoom(delta)
}

const startDrag = (e) => store.startDrag(e)
const onDrag = (e) => store.onDrag(e)
const endDrag = () => store.endDrag()

const copyImage = () => store.copyImage()

const downloadImage = () => store.downloadImage()

const onKeydown = (e) => {
  if (!store.visible) return
  if (e.key === 'Escape') close()
  if (e.key === '+' || e.key === '=') zoomIn()
  if (e.key === '-') zoomOut()
  if (e.key.toLowerCase() === 'r') reset()
}

onMounted(() => {
  window.addEventListener('keydown', onKeydown)
  // 打开后让 mask 获得焦点（方便 ESC）
  setTimeout(() => maskRef.value?.focus?.(), 0)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
})

</script>

<style scoped>
.mask {
  position: fixed;
  inset: 0;
  z-index: 10000;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  outline: none;
}

.toolbar {
  position: fixed;
  top: 16px;
  right: 16px;
  display: flex;
  gap: 8px;
}

.toolbar button {
  padding: 6px 10px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
}

.stage {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.img {
  max-width: 70vw;
  max-height: 70vh;
  user-select: none;
  -webkit-user-drag: none;
  border-radius: 10px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.35);
}
</style>
