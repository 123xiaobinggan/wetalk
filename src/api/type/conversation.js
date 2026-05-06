export function createConversationModel(data = {}) {
  return {
    convId: data.convId ?? null,
    convType: data.convType ?? 0, // 0 私聊 1 群聊
    peerId: data.peerId ?? null,
    groupId: data.groupId ?? null,
    remark: data.remark ?? '',
    avatar: data.avatar ?? '',
    unreadCnt: data.unreadCnt ?? 0,
    pinned: data.pinned ?? false,
    muted: data.muted ?? false,
    lastMsgBrief: data.lastMsgBrief ?? '',
    lastMsgTime: data.lastMsgTime ?? '',
    lastMsgSenderId: data.lastMsgSenderId ?? null,
  }
}
