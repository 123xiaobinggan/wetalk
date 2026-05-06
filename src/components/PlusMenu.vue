<template>
    <div class="mask" @click="plusMenuStore.hideMenu">
        <div class="plus-menu" :style="menuStyle" @click.stop>
            <div class="menu-item" @click="plusMenuStore.handleBuildGroup">
                发起群聊
            </div>
            <div class="menu-item" @click="plusMenuStore.handleAddFriend">
                添加朋友
            </div>
        </div>
    </div>
</template>

<script setup>
import { computed } from 'vue'
import { usePlusMenuStore } from '/src/stores/plusMenuStore'
defineOptions({
    name: 'PlusMenu',
})
const plusMenuStore = usePlusMenuStore()

const menuStyle = computed(() => {
    return {
        left: `${plusMenuStore.x}px`,
        top: `${plusMenuStore.y}px`
    }
})
</script>

<style scoped>
.mask {
    position: fixed;
    inset: 0;
    z-index: 9999;
}

.plus-menu {
    position: fixed;
    width: 140px;
    background: #fff;
    border-radius: 10px;
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
    overflow: hidden;
    z-index: 2001;
}

.menu-item {
    height: 44px;
    line-height: 44px;
    padding: 0 16px;
    font-size: 14px;
    color: #333;
    cursor: pointer;
    user-select: none;
}

.menu-item:hover {
    background: #f5f5f5;
}
</style>
