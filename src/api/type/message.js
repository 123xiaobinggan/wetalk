export function createMessageModel(data = {}) {
  return {
    msgId: data.msgId ?? null,
    clientMsgId: data.clientMsgId ?? '',
    convType: data.convType ?? 0,
    senderId: data.senderId ?? null,
    peerId: data.peerId ?? null,
    groupId: data.groupId ?? null,
    msgType: data.msgType ?? 1,
    content: data.content ?? {},
    createdTime: data.createdTime ?? '',
    recallFlag: data.recallFlag ?? 0,
  }
}
